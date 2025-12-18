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
    private Coord lastPlayerLocalCoord = null;  // Player's local coord in PREVIOUS grid (preserved across grid change)
    private Coord previousTickPlayerLocalCoord = null;  // Player position from previous tick (before any grid change)
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
        // When you see cellardoor as exit -> you're INSIDE the building (you came up from cellar)
        // The cellardoor is the entrance TO the cellar, located on the inside floor
        PORTAL_TO_LAYER.put("cellardoor", "inside");

        // When you see cellarstairs as exit -> you're IN the cellar (you went down)
        // The cellarstairs is what you see AT THE BOTTOM after going down
        PORTAL_TO_LAYER.put("cellarstairs", "cellar");

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
     * Uses the continuously cached player position (lastPlayerLocalCoord) as the access point.
     */
    public void setClickedPortal(Gob portal) {
        if (portal != null && portal.ngob != null) {
            this.clickedPortal = portal;
            // Use the cached player position - this was continuously updated on every tick
            // while the player was standing in front of the door, BEFORE clicking
            this.clickedPortalLocalCoord = lastPlayerLocalCoord;
            System.out.println("ChunkNav: setClickedPortal(" + portal.ngob.name + ") using cached player pos: " + clickedPortalLocalCoord);
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

        // Debug: log grid changes to verify we're being called
        if (lastGridId != -1 && currentGridId != lastGridId) {
            System.out.println("ChunkNav: doCheck() detected grid change: " + lastGridId + " -> " + currentGridId);
        }

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
            // Grid changed! Use previousTickPlayerLocalCoord - the player's position from BEFORE the change
            // This is the position they were standing at when they clicked the door
            onGridChanged(lastGridId, currentGridId, player);
        }

        // Update tracking state AFTER handling grid change
        lastGridId = currentGridId;

        // Chain the local coords: current -> previous -> lastPlayer
        // This ensures we always have the position from 1-2 ticks ago when grid change is detected
        if (currentPlayerLocalCoord != null) {
            // Save current tick position as the "previous tick" for next time
            // And save previous tick as "last player" for grid change detection
            lastPlayerLocalCoord = previousTickPlayerLocalCoord;
            previousTickPlayerLocalCoord = currentPlayerLocalCoord;
        }

        // Track nearby portals - use PLAYER's current position as the access point
        // The player must be standing in a walkable spot to be near the portal
        Gob nearbyPortal = findNearbyPortal(player);
        if (nearbyPortal != null) {
            lastNearbyPortal = nearbyPortal;
            // Use player's current position - they're standing in front of the door
            lastNearbyPortalLocalCoord = currentPlayerLocalCoord;
        }

        // Capture lastActions gob BEFORE grid change (like routes system does)
        // This preserves the clicked portal info even after the grid changes
        // Use PLAYER's position as the access point - they clicked while standing there
        try {
            NCore.LastActions lastActions = NUtils.getUI().core.getLastActions();
            if (lastActions != null && lastActions.gob != null && lastActions.gob.ngob != null) {
                String gobName = lastActions.gob.ngob.name;
                if (gobName != null && isPortalGob(gobName)) {
                    cachedLastActionsGob = lastActions.gob;
                    // Use player's current position - they clicked while standing here
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
        System.out.println("ChunkNav: Grid changed from " + fromGridId + " to " + toGridId);

        // Wait a moment for gobs to load after grid change
        // This is called from tick(), so we can't do async waiting - just retry on next tick if needed
        // But we can wait a short time for gobs to appear
        try {
            Thread.sleep(100);  // Brief wait for gobs to load
        } catch (InterruptedException e) {
            return;
        }

        // FIRST: Check if we have an explicitly clicked portal (from executor navigation)
        if (clickedPortal != null && clickedPortal.ngob != null && clickedPortalLocalCoord != null) {
            String portalName = clickedPortal.ngob.name;
            String portalHash = getPortalHash(clickedPortal);
            System.out.println("ChunkNav: Using explicitly clicked portal: " + portalName);

            // Record the entrance connection
            recordPortalConnection(portalHash, portalName, fromGridId, toGridId, clickedPortalLocalCoord);

            // Find and record exit portal (reverse connection) and detect layer
            // Retry a few times for gobs to load
            Gob exitPortal = null;
            for (int i = 0; i < 5 && exitPortal == null; i++) {
                exitPortal = findExitPortalAndUpdateLayer(player, portalName, toGridId);
                if (exitPortal == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            if (exitPortal != null && exitPortal.ngob != null) {
                String exitHash = getPortalHash(exitPortal);
                String exitName = exitPortal.ngob.name;
                Coord exitLocalCoord = getGobLocalCoord(player);
                System.out.println("ChunkNav: Found exit portal: " + exitName);
                recordPortalConnection(exitHash, exitName, toGridId, fromGridId, exitLocalCoord);
            } else {
                System.out.println("ChunkNav: No exit portal found for explicit traversal");
            }

            // Clear tracking
            clickedPortal = null;
            clickedPortalLocalCoord = null;
            return;
        }

        // FALLBACK: For manual traversals, use exit portal to determine entrance
        // Find the exit portal on the new side - retry a few times for gobs to load
        Gob exitPortal = null;
        System.out.println("ChunkNav: Looking for nearby exit portal...");
        for (int i = 0; i < 5 && exitPortal == null; i++) {
            exitPortal = findNearbyPortal(player);
            if (exitPortal == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        if (exitPortal == null || exitPortal.ngob == null) {
            System.out.println("ChunkNav: No exit portal found nearby player after retries");
            return;
        }

        String exitName = exitPortal.ngob.name;
        String exitHash = getPortalHash(exitPortal);
        String entranceName = GateDetector.getDoorPair(exitName);
        System.out.println("ChunkNav: Found exit portal: " + exitName + " entrance pair: " + entranceName);

        // Use PLAYER's current position - this is where we land after exiting, the accessible spot
        Coord exitLocalCoord = getGobLocalCoord(player);

        // Update the destination chunk's layer based on the exit portal
        updateChunkLayer(toGridId, exitName);

        // Record: exit portal on toGrid connects back to fromGrid (using player's position as access point)
        System.out.println("ChunkNav: Recording exit portal " + exitName + " connects " + toGridId + " -> " + fromGridId);
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
            }

            // Strategy 3: Fallback to player position if we don't have the portal position
            if (entranceCoord == null && lastPlayerLocalCoord != null) {
                entranceCoord = lastPlayerLocalCoord;
                entranceHash = "entrance_" + fromGridId + "_" + entranceName.hashCode();
            }

            if (entranceCoord != null && entranceHash != null) {
                System.out.println("ChunkNav: Recording entrance portal " + entranceName + " connects " + fromGridId + " -> " + toGridId);
                recordPortalConnection(entranceHash, entranceName, fromGridId, toGridId, entranceCoord);
            } else {
                System.out.println("ChunkNav: Could not determine entrance portal position for " + entranceName);
            }
        } else {
            System.out.println("ChunkNav: No entrance pair mapping for exit portal: " + exitName);
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
        System.out.println("ChunkNav: recordPortalConnection(" + gobName + ", " + fromGridId + " -> " + toGridId + ", " + localCoord + ")");

        // Update the portal in the source chunk
        ChunkNavData fromChunk = graph.getChunk(fromGridId);
        if (fromChunk == null) {
            System.out.println("ChunkNav: Chunk " + fromGridId + " not found, creating minimal chunk...");
            // Chunk not recorded yet - try to record it now
            fromChunk = createMinimalChunk(fromGridId);
            if (fromChunk == null) {
                System.out.println("ChunkNav: Failed to create minimal chunk for " + fromGridId);
                return;
            }
        }

        // Use provided localCoord or default to center
        Coord portalCoord = localCoord != null ? localCoord : new Coord(50, 50);

        // Find or create the portal - check by hash first, then by position+name
        // Do NOT use findPortalByName - that causes all buildings with same name to merge
        // Use position+name to avoid merging different portal types at same location (e.g., cellardoor vs stonemansion-door)
        ChunkPortal portal = fromChunk.findPortal(gobHash);
        if (portal == null && portalCoord != null && gobName != null) {
            portal = fromChunk.findPortalByPositionAndName(portalCoord, gobName, 3);
        }

        if (portal == null) {
            ChunkPortal.PortalType type = ChunkPortal.classifyPortal(gobName);
            if (type == null) type = ChunkPortal.PortalType.DOOR;

            portal = new ChunkPortal(gobHash, gobName, type, portalCoord);
            fromChunk.addOrUpdatePortal(portal);
            graph.addChunk(fromChunk); // Re-add to update portal index
            System.out.println("ChunkNav: Created new portal " + gobName + " in chunk " + fromGridId);
        } else {
            // Update existing portal with new hash and position if provided
            portal.gobHash = gobHash;  // Update hash in case it was a synthetic one
            if (localCoord != null) {
                portal.localCoord = portalCoord;
            }
            System.out.println("ChunkNav: Updated existing portal " + gobName + " in chunk " + fromGridId);
        }

        // Update the connection
        portal.connectsToGridId = toGridId;
        portal.lastTraversed = System.currentTimeMillis();
        System.out.println("ChunkNav: Portal " + gobName + " now connects to " + toGridId);

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
                        return chunk;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
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
            return;
        }

        ChunkNavData chunk = graph.getChunk(gridId);
        if (chunk != null && !layer.equals(chunk.layer)) {
            chunk.layer = layer;
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
        previousTickPlayerLocalCoord = null;
    }
}
