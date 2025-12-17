package nurgling.navigation;

import haven.*;
import nurgling.NCore;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.GateDetector;
import nurgling.tasks.WaitForMapLoadNoCoord;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

/**
 * Tracks when the player traverses portals (doors, stairs, cellars, mines)
 * and records the connections in the ChunkNav graph.
 *
 * Detection approach:
 * 1. Monitor player's grid ID
 * 2. When grid ID changes, check if player was near a portal
 * 3. If so, find the matching portal on the destination side
 * 4. Record the bidirectional connection
 */
public class PortalTraversalTracker {
    private final ChunkNavGraph graph;
    private final ChunkNavRecorder recorder;

    // State tracking
    private long lastGridId = -1;
    private Gob lastNearbyPortal = null;
    private Coord lastNearbyPortalLocalCoord = null;  // Cached local coord of lastNearbyPortal
    private Coord lastPlayerLocalCoord = null;  // Player's local coord in current grid (cached before portal traversal)
    private long lastCheckTime = 0;

    // Explicitly tracked portal (set when we know which portal was clicked)
    private Gob clickedPortal = null;
    private Coord clickedPortalLocalCoord = null;

    // Cached lastActions gob - captured before grid change to preserve the clicked portal info
    private Gob cachedLastActionsGob = null;
    private Coord cachedLastActionsGobLocalCoord = null;

    private static final long CHECK_INTERVAL_MS = 100;
    private static final double PORTAL_PROXIMITY_THRESHOLD = 15.0;

    // Layer mappings based on portal exit type
    // Maps portal exit name patterns to the layer the destination chunk should be assigned
    // The EXIT portal (what you see after traversing) determines the layer
    private static final Map<String, String> PORTAL_TO_LAYER = new HashMap<>();
    static {
        // When you see cellardoor as exit -> you're in the cellar (you went down via cellardoor)
        PORTAL_TO_LAYER.put("cellardoor", "cellar");

        // When you see cellarstairs as exit -> you're inside the building (you came up from cellar)
        PORTAL_TO_LAYER.put("cellarstairs", "inside");

        // When you see a *-door as exit -> you're inside the building (you entered from outside)
        // These are the "-door" variants you see INSIDE the building after entering
        PORTAL_TO_LAYER.put("stonemansion-door", "inside");
        PORTAL_TO_LAYER.put("logcabin-door", "inside");
        PORTAL_TO_LAYER.put("timberhouse-door", "inside");
        PORTAL_TO_LAYER.put("stonestead-door", "inside");
        PORTAL_TO_LAYER.put("greathall-door", "inside");
        PORTAL_TO_LAYER.put("stonetower-door", "inside");
        PORTAL_TO_LAYER.put("windmill-door", "inside");

        // When you see the building gob as exit -> you're outside on surface (you exited the building)
        // These are the building gobs you see OUTSIDE after leaving the building
        PORTAL_TO_LAYER.put("stonemansion", "surface");
        PORTAL_TO_LAYER.put("logcabin", "surface");
        PORTAL_TO_LAYER.put("timberhouse", "surface");
        PORTAL_TO_LAYER.put("stonestead", "surface");
        PORTAL_TO_LAYER.put("greathall", "surface");
        PORTAL_TO_LAYER.put("stonetower", "surface");
        PORTAL_TO_LAYER.put("windmill", "surface");

        // Mine and underground
        PORTAL_TO_LAYER.put("minehole", "mine");
        PORTAL_TO_LAYER.put("ladderdown", "underground");
        PORTAL_TO_LAYER.put("ladderup", "surface");
    }

    // Portal resource patterns to track
    private static final String[] PORTAL_PATTERNS = {
        "door",
        "cellar",
        "minehole",
        "ladder",
        "stairs",
        "gate"
    };

    public PortalTraversalTracker(ChunkNavGraph graph, ChunkNavRecorder recorder) {
        this.graph = graph;
        this.recorder = recorder;
    }

    /**
     * Call this BEFORE clicking on a portal to explicitly track which portal was clicked.
     * This is more reliable than proximity-based tracking.
     * Records the PLAYER's position (accessible spot in front of door), not the door gob's position.
     */
    public void setClickedPortal(Gob portal) {
        if (portal != null && portal.ngob != null) {
            this.clickedPortal = portal;
            // Record PLAYER's position - this is where you can access the door from
            Gob player = NUtils.player();
            if (player != null) {
                this.clickedPortalLocalCoord = getGobLocalCoord(player);
            } else {
                this.clickedPortalLocalCoord = getGobLocalCoord(portal);
            }
            System.err.println("PortalTraversalTracker: Explicitly tracking clicked portal: " + portal.ngob.name +
                " (player access point at " + clickedPortalLocalCoord + ")");
        }
    }

    /**
     * Clear the explicitly clicked portal (call after traversal is complete).
     */
    public void clearClickedPortal() {
        this.clickedPortal = null;
        this.clickedPortalLocalCoord = null;
    }

    /**
     * Call this periodically to check for portal traversals.
     * Safe to call frequently - internally throttled.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime = now;

        try {
            doCheck();
        } catch (Exception e) {
            // Ignore - player might not exist yet
        }
    }

    private void doCheck() {
        Gob player = NUtils.player();
        if (player == null) return;

        long currentGridId = graph.getPlayerChunkId();
        if (currentGridId == -1) return;

        // Calculate player's current local coord in their CURRENT grid
        Coord currentPlayerLocalCoord = null;
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            Coord tileCoord = player.rc.floor(MCache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            if (grid != null && grid.id == currentGridId) {
                currentPlayerLocalCoord = tileCoord.sub(grid.ul);
            }
        } catch (Exception e) {
            // Ignore
        }

        // Check for grid change
        if (lastGridId != -1 && currentGridId != lastGridId) {
            // Grid changed! Use the PREVIOUS lastPlayerLocalCoord (from before the change)
            onGridChanged(lastGridId, currentGridId, player);
        }

        // Update tracking state AFTER handling grid change
        lastGridId = currentGridId;
        // Only update lastPlayerLocalCoord if we got a valid position for the current grid
        // This ensures we always have the player's position in lastGridId's coordinate space
        if (currentPlayerLocalCoord != null) {
            lastPlayerLocalCoord = currentPlayerLocalCoord;
        }

        // Track nearby portals and cache PLAYER's position (not portal position) while grid is loaded
        // The player's position is where the door is accessible from - this is what we navigate to
        Gob nearbyPortal = findNearbyPortal(player);
        if (nearbyPortal != null) {
            lastNearbyPortal = nearbyPortal;
            // Cache the PLAYER's local coordinate NOW while the grid is still loaded
            // This is the accessible spot in front of the door
            lastNearbyPortalLocalCoord = currentPlayerLocalCoord;
            System.err.println("PortalTraversalTracker: Tracking portal " + nearbyPortal.ngob.name +
                " (player access point at " + lastNearbyPortalLocalCoord + ")");
        }

        // Capture lastActions gob BEFORE grid change (like routes system does)
        // This preserves the clicked portal info even after the grid changes
        // We store the PLAYER's position (not the gob's position) as that's where we can access the door from
        try {
            NCore.LastActions lastActions = NUtils.getUI().core.getLastActions();
            if (lastActions != null && lastActions.gob != null && lastActions.gob.ngob != null) {
                String gobName = lastActions.gob.ngob.name;
                if (gobName != null && isPortalGob(gobName)) {
                    cachedLastActionsGob = lastActions.gob;
                    // Store PLAYER's position - this is the accessible spot in front of the door
                    cachedLastActionsGobLocalCoord = currentPlayerLocalCoord;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Called when player's grid ID changes.
     */
    private void onGridChanged(long fromGridId, long toGridId, Gob player) {
        System.err.println("PortalTraversalTracker: Grid changed from " + fromGridId + " to " + toGridId);

        // FIRST: Check if we have an explicitly clicked portal (from executor navigation)
        if (clickedPortal != null && clickedPortal.ngob != null && clickedPortalLocalCoord != null) {
            String portalName = clickedPortal.ngob.name;
            String portalHash = getPortalHash(clickedPortal);
            System.err.println("PortalTraversalTracker: Using explicitly clicked portal: " + portalName);

            // Record the entrance connection
            recordPortalConnection(portalHash, portalName, fromGridId, toGridId, clickedPortalLocalCoord);

            // Find and record exit portal (reverse connection) and detect layer
            Gob exitPortal = findExitPortalAndUpdateLayer(player, portalName, toGridId);
            if (exitPortal != null && exitPortal.ngob != null) {
                String exitHash = getPortalHash(exitPortal);
                String exitName = exitPortal.ngob.name;
                Coord exitLocalCoord = getGobLocalCoord(player);
                recordPortalConnection(exitHash, exitName, toGridId, fromGridId, exitLocalCoord);
            }

            // Clear tracking
            clickedPortal = null;
            clickedPortalLocalCoord = null;
            return;
        }

        // FALLBACK: For manual traversals, use exit portal to determine entrance
        // Find the exit portal on the new side
        Gob exitPortal = findNearbyPortal(player);
        if (exitPortal == null || exitPortal.ngob == null) {
            System.err.println("PortalTraversalTracker: No exit portal found, cannot record");
            return;
        }

        String exitName = exitPortal.ngob.name;
        String exitHash = getPortalHash(exitPortal);
        String entranceName = GateDetector.getDoorPair(exitName);
        // Use PLAYER's current position - this is where we land after exiting, the accessible spot
        Coord exitLocalCoord = getGobLocalCoord(player);

        System.err.println("PortalTraversalTracker: Found exit portal: " + exitName + ", entrance pair: " + entranceName);

        // Update the destination chunk's layer based on the exit portal
        updateChunkLayer(toGridId, exitName);

        // Record: exit portal on toGrid connects back to fromGrid (using player's position as access point)
        recordPortalConnection(exitHash, exitName, toGridId, fromGridId, exitLocalCoord);

        // Record: entrance portal on fromGrid connects to toGrid
        // Try multiple strategies to get the actual entrance portal position
        if (entranceName != null) {
            Coord entranceCoord = null;
            String entranceHash = null;

            // Strategy 1: Use cached lastActions gob (captured BEFORE grid change, like routes system)
            // This is the most reliable way because we captured it while the grid was still loaded
            if (cachedLastActionsGob != null && cachedLastActionsGob.ngob != null &&
                cachedLastActionsGobLocalCoord != null) {
                String cachedName = cachedLastActionsGob.ngob.name;
                // Verify this is the entrance portal we're looking for
                if (cachedName != null && isPortalGob(cachedName) &&
                    (cachedName.equals(entranceName) ||
                     cachedName.contains(entranceName) ||
                     entranceName.contains(cachedName))) {
                    entranceCoord = cachedLastActionsGobLocalCoord;
                    entranceHash = getPortalHash(cachedLastActionsGob);
                    System.err.println("PortalTraversalTracker: Using cached lastActions gob for entrance " + entranceName + " at " + entranceCoord);
                }
            }

            // Strategy 2: Check if the portal we were tracking matches the expected entrance
            if (entranceCoord == null && lastNearbyPortal != null && lastNearbyPortal.ngob != null &&
                lastNearbyPortal.ngob.name != null && lastNearbyPortalLocalCoord != null &&
                (lastNearbyPortal.ngob.name.equals(entranceName) ||
                 lastNearbyPortal.ngob.name.contains(entranceName) ||
                 entranceName.contains(lastNearbyPortal.ngob.name))) {
                // Use the actual portal position we were tracking
                entranceCoord = lastNearbyPortalLocalCoord;
                entranceHash = getPortalHash(lastNearbyPortal);
                System.err.println("PortalTraversalTracker: Using tracked portal position for entrance " + entranceName + " at " + entranceCoord);
            }

            // Strategy 3: Fallback to player position if we don't have the portal position
            if (entranceCoord == null && lastPlayerLocalCoord != null) {
                entranceCoord = lastPlayerLocalCoord;
                entranceHash = "entrance_" + fromGridId + "_" + entranceName.hashCode();
                System.err.println("PortalTraversalTracker: Using player position for entrance " + entranceName + " at " + entranceCoord + " (portal not tracked)");
            }

            if (entranceCoord != null && entranceHash != null) {
                recordPortalConnection(entranceHash, entranceName, fromGridId, toGridId, entranceCoord);
            } else {
                System.err.println("PortalTraversalTracker: No position available for entrance recording");
            }
        } else {
            System.err.println("PortalTraversalTracker: No entrance pair known for " + exitName);
        }

        // Clear tracking state after use
        lastNearbyPortal = null;
        lastNearbyPortalLocalCoord = null;
        cachedLastActionsGob = null;
        cachedLastActionsGobLocalCoord = null;
    }

    /**
     * Record a portal connection in the graph.
     * @param localCoord The local tile coordinate within the chunk (can be null for default center)
     */
    private void recordPortalConnection(String gobHash, String gobName, long fromGridId, long toGridId, Coord localCoord) {
        // Update the portal in the source chunk
        ChunkNavData fromChunk = graph.getChunk(fromGridId);
        if (fromChunk == null) {
            // Chunk not recorded yet - try to record it now
            System.err.println("PortalTraversalTracker: Source chunk " + fromGridId + " not found, attempting to create");
            fromChunk = createMinimalChunk(fromGridId);
            if (fromChunk == null) {
                System.err.println("PortalTraversalTracker: Could not create chunk " + fromGridId);
                return;
            }
        }

        // Use provided localCoord or default to center
        Coord portalCoord = localCoord != null ? localCoord : new Coord(50, 50);

        // Find or create the portal - check by hash first, then by name
        ChunkPortal portal = fromChunk.findPortal(gobHash);
        if (portal == null) {
            portal = fromChunk.findPortalByName(gobName);
        }

        if (portal == null) {
            System.err.println("PortalTraversalTracker: Portal not in chunk, creating new entry at " + portalCoord);
            ChunkPortal.PortalType type = ChunkPortal.classifyPortal(gobName);
            if (type == null) type = ChunkPortal.PortalType.DOOR;

            portal = new ChunkPortal(gobHash, gobName, type, portalCoord);
            fromChunk.addOrUpdatePortal(portal);
            graph.addChunk(fromChunk); // Re-add to update portal index
        } else {
            // Update existing portal with new hash and position if provided
            portal.gobHash = gobHash;  // Update hash in case it was a synthetic one
            if (localCoord != null) {
                portal.localCoord = portalCoord;
            }
            System.err.println("PortalTraversalTracker: Updated existing portal at " + portal.localCoord);
        }

        // Update the connection
        portal.connectsToGridId = toGridId;
        portal.lastTraversed = System.currentTimeMillis();

        System.err.println("PortalTraversalTracker: Recorded connection " + gobName +
            " from grid " + fromGridId + " -> " + toGridId + " at " + portalCoord);

        // Also notify recorder
        recorder.recordPortalTraversal(gobHash, fromGridId, toGridId);

        // Trigger a throttled save
        try {
            ChunkNavManager.getInstance().saveThrottled();
        } catch (Exception e) {
            // Ignore save errors
        }
    }

    /**
     * Get the local tile coordinate of a gob within its grid.
     */
    private Coord getGobLocalCoord(Gob gob) {
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            Coord tileCoord = gob.rc.floor(MCache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            if (grid != null) {
                return tileCoord.sub(grid.ul);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get the local tile coordinate of a gob, using cached chunk data if the grid is unloaded.
     * This is useful when we need the position of a gob on a grid that's no longer in MCache.
     */
    private Coord getGobLocalCoordFromCache(Gob gob, long expectedGridId) {
        // First try the normal method
        Coord normalCoord = getGobLocalCoord(gob);
        if (normalCoord != null) {
            return normalCoord;
        }

        // Grid might be unloaded - try to use stored worldTileOrigin from ChunkNavData
        try {
            ChunkNavData chunk = graph.getChunk(expectedGridId);
            if (chunk != null && chunk.worldTileOrigin != null) {
                Coord tileCoord = gob.rc.floor(MCache.tilesz);
                Coord localCoord = tileCoord.sub(chunk.worldTileOrigin);
                // Validate the local coord is within chunk bounds (0-99)
                if (localCoord.x >= 0 && localCoord.x < 100 && localCoord.y >= 0 && localCoord.y < 100) {
                    return localCoord;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Create a minimal chunk entry for a grid that wasn't recorded yet.
     */
    private ChunkNavData createMinimalChunk(long gridId) {
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == gridId) {
                        Coord gridCoord = grid.ul.div(100);
                        ChunkNavData chunk = new ChunkNavData(gridId, gridCoord, grid.ul);
                        graph.addChunk(chunk);
                        System.err.println("PortalTraversalTracker: Created minimal chunk for grid " + gridId);
                        return chunk;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("PortalTraversalTracker: Error creating chunk: " + e.getMessage());
        }
        return null;
    }

    /**
     * Find exit portal and update the destination chunk's layer.
     * Returns the exit portal gob if found.
     */
    private Gob findExitPortalAndUpdateLayer(Gob player, String entrancePortalName, long toGridId) {
        String exitPortalName = GateDetector.getDoorPair(entrancePortalName);
        Gob exitPortal = null;

        if (exitPortalName != null) {
            exitPortal = findExitPortal(player, exitPortalName);
        }

        if (exitPortal == null) {
            exitPortal = findNearbyPortal(player);
        }

        if (exitPortal != null && exitPortal.ngob != null) {
            updateChunkLayer(toGridId, exitPortal.ngob.name);
        }

        return exitPortal;
    }

    /**
     * Find and record the exit portal after traversing through a known entrance.
     */
    private void findAndRecordExitPortal(Gob player, String entrancePortalName, long fromGridId, long toGridId) {
        // Get the expected exit portal name
        String exitPortalName = GateDetector.getDoorPair(entrancePortalName);
        Gob exitPortal = null;

        if (exitPortalName != null) {
            exitPortal = findExitPortal(player, exitPortalName);
        }

        // If no mapped pair found, look for any nearby portal
        if (exitPortal == null) {
            exitPortal = findNearbyPortal(player);
        }

        if (exitPortal != null && exitPortal.ngob != null) {
            String exitHash = getPortalHash(exitPortal);
            String exitName = exitPortal.ngob.name;
            // Use PLAYER's position - this is where we land after exiting, the accessible spot
            Coord exitLocalCoord = getGobLocalCoord(player);
            recordPortalConnection(exitHash, exitName, toGridId, fromGridId, exitLocalCoord);
            System.err.println("PortalTraversalTracker: Found exit portal (" + exitName + "), recorded reverse connection at player pos " + exitLocalCoord);
        } else {
            System.err.println("PortalTraversalTracker: No exit portal found, reverse connection not recorded");
        }
    }

    /**
     * Find a portal near the player.
     */
    private Gob findNearbyPortal(Gob player) {
        try {
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            double closestDist = PORTAL_PROXIMITY_THRESHOLD;
            Gob closest = null;

            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob == null || gob.ngob.name == null) continue;

                    // Check if it's a portal type
                    if (!isPortalGob(gob.ngob.name)) continue;

                    double dist = player.rc.dist(gob.rc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = gob;
                    }
                }
            }

            return closest;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find an exit portal after traversing.
     * Uses Finder.findGob which searches more broadly than proximity check.
     */
    private Gob findExitPortal(Gob player, String exitPortalName) {
        try {
            // First try using Finder which searches all visible gobs
            Gob exitGob = Finder.findGob(new NAlias(exitPortalName));
            if (exitGob != null) {
                return exitGob;
            }

            // Fallback: search by proximity with larger radius
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            double closestDist = PORTAL_PROXIMITY_THRESHOLD * 4;  // Much larger radius for exit search
            Gob closest = null;

            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob == null || gob.ngob.name == null) continue;

                    if (gob.ngob.name.contains(exitPortalName) || exitPortalName.contains(gob.ngob.name)) {
                        double dist = player.rc.dist(gob.rc);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = gob;
                        }
                    }
                }
            }

            return closest;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a gob name is a portal type.
     */
    private boolean isPortalGob(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();

        for (String pattern : PORTAL_PATTERNS) {
            if (lower.contains(pattern)) {
                // Exclude water gates and similar non-traversable things
                if (lower.contains("water") || lower.contains("floodgate")) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the layer for a destination grid based on the exit portal type.
     * @param exitPortalName The name of the exit portal (the portal you see after traversing)
     * @return The layer name for the destination grid
     */
    private String determineLayerFromExitPortal(String exitPortalName) {
        if (exitPortalName == null) return null;

        String lowerName = exitPortalName.toLowerCase();

        // Check each mapping
        for (Map.Entry<String, String> entry : PORTAL_TO_LAYER.entrySet()) {
            if (lowerName.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Default: if it ends with "-door" it's likely inside a building
        if (lowerName.endsWith("-door") || lowerName.contains("-door")) {
            return "inside";
        }

        return null; // Unknown - don't change layer
    }

    /**
     * Update the layer of a chunk based on the exit portal.
     * Called when we traverse to a new grid and find an exit portal.
     * Only updates if a definitive layer can be determined from the exit portal.
     */
    private void updateChunkLayer(long gridId, String exitPortalName) {
        String layer = determineLayerFromExitPortal(exitPortalName);
        if (layer == null) {
            System.err.println("PortalTraversalTracker: No layer change for exit portal " + exitPortalName + " (keeping existing layer)");
            return;
        }

        ChunkNavData chunk = graph.getChunk(gridId);
        if (chunk != null && !layer.equals(chunk.layer)) {
            String oldLayer = chunk.layer;
            chunk.layer = layer;
            System.err.println("PortalTraversalTracker: Updated chunk " + gridId + " layer: " + oldLayer + " -> " + layer);
        }
    }

    /**
     * Get a consistent hash for a portal gob.
     */
    private String getPortalHash(Gob gob) {
        if (gob.ngob != null && gob.ngob.hash != null && !gob.ngob.hash.isEmpty()) {
            return gob.ngob.hash;
        }
        // Fallback - use gob id and position
        return "portal_" + gob.id + "_" + (int)gob.rc.x + "_" + (int)gob.rc.y;
    }

    /**
     * Reset tracking state.
     */
    public void reset() {
        lastGridId = -1;
        lastNearbyPortal = null;
        lastNearbyPortalLocalCoord = null;
        lastPlayerLocalCoord = null;
    }
}
