package nurgling.navigation;

import haven.*;
import nurgling.NCore;
import nurgling.NUtils;
import nurgling.tasks.GateDetector;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

/**
 * Tracks when the player traverses portals (doors, stairs, cellars, mines)
 * and records the connections in the ChunkNav graph.
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
    private long lastCheckTime = 0;

    // Mine level tracking - tracks how deep we are in the mine
    // 0 = surface, 1 = mine1, 2 = mine2, etc.
    private int currentMineLevel = 0;
    private boolean mineLevelInitialized = false;  // Has mine level been initialized from current grid?

    // Duplicate grid transition prevention
    private long lastProcessedFromGridId = -1;
    private long lastProcessedToGridId = -1;
    private long lastProcessedTime = 0;
    private static final long DUPLICATE_PREVENTION_MS = 2000; // Ignore same transition within 2 seconds

    // Cached lastActions gob - captured before grid change to preserve the clicked portal info
    // This works for both manual and automated navigation since the game tracks all clicks
    private Gob cachedLastActionsGob = null;
    private Coord cachedLastActionsGobLocalCoord = null;
    private long cachedLastActionsGobGridId = -1;  // Grid ID of the cached portal (for boundary fix)
    private long lastProcessedPortalGobId = -1;  // Gob ID of last processed portal (prevents re-capture)

    private static final long CHECK_INTERVAL_MS = 100;

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

        // Stairs between floors - all floors inside a building are "inside" layer
        // When you see downstairs as exit -> you're on an upper floor (still "inside")
        PORTAL_TO_LAYER.put("downstairs", "inside");
        // When you see upstairs as exit -> you're on a lower floor (still "inside")
        PORTAL_TO_LAYER.put("upstairs", "inside");

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
     * Call this periodically to check for portal traversals.
     * Safe to call frequently - internally throttled.
     */
    public void tick() throws InterruptedException {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime = now;
        doCheck();
    }

    private void doCheck() throws InterruptedException {
        Gob player = NUtils.player();
        if (player == null) return;

        long currentGridId = graph.getPlayerChunkId();
        if (currentGridId == -1) return;

        // Initialize mine level from current grid's layer on first valid tick
        // This handles login/teleport where we need to know our starting level
        if (!mineLevelInitialized) {
            initializeMineLevelFromCurrentGrid(currentGridId);
        }

        // Calculate player's current local coord in their CURRENT grid
        Coord currentPlayerLocalCoord = null;
        MCache mcache = NUtils.getGameUI().map.glob.map;
        Coord tileCoord = player.rc.floor(MCache.tilesz);
        MCache.Grid grid = mcache.getgridt(tileCoord);
        if (grid != null && grid.id == currentGridId) {
            currentPlayerLocalCoord = tileCoord.sub(grid.ul);
        }

        // Check for grid change
        if (lastGridId != -1 && currentGridId != lastGridId) {
            onGridChanged(lastGridId, currentGridId, player);
        }

        // Update tracking state AFTER handling grid change
        lastGridId = currentGridId;

        // Capture lastActions gob BEFORE grid change (like routes system does)
        // This preserves the clicked portal info even after the grid changes
        // Only capture if it's a NEW portal (different from the last one we processed)
        // This prevents re-capturing stale actions after we've already recorded the portal
        try {
            NCore.LastActions lastActions = NUtils.getUI().core.getLastActions();
            if (lastActions != null && lastActions.gob != null && lastActions.gob.ngob != null) {
                String gobName = lastActions.gob.ngob.name;
                // Only capture if it's a portal AND it's not the same one we already processed
                if (isPortalGob(gobName) && lastActions.gob.id != lastProcessedPortalGobId) {
                    cachedLastActionsGob = lastActions.gob;
                    // Use getPortalLocalCoord which offsets buildings toward player for stable door position
                    Coord portalCoord = getPortalLocalCoord(lastActions.gob, player);
                    if (portalCoord != null) {
                        cachedLastActionsGobLocalCoord = portalCoord;
                    }
                    // Cache the portal's actual grid ID while the grid is still loaded (fixes boundary bug)
                    cachedLastActionsGobGridId = getGobGridId(lastActions.gob);
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // NOTE: Stairs (upstairs/downstairs) DO cause grid changes, just like doors.
        // They are handled by the normal onGridChanged() logic, not special same-grid handling.
    }

    /**
     * Called when player's grid ID changes.
     */
    private void onGridChanged(long fromGridId, long toGridId, Gob player) throws InterruptedException {
        // Check for duplicate grid transition (same transition firing multiple times)
        long now = System.currentTimeMillis();
        if (fromGridId == lastProcessedFromGridId && toGridId == lastProcessedToGridId &&
            (now - lastProcessedTime) < DUPLICATE_PREVENTION_MS) {
            return;
        }

        // Check if player landed on their hearthfire - this indicates a teleport, not a portal traversal
        if (isPlayerOnHearthfire(player)) {
            // Player teleported to hearthfire - don't record this as a portal connection
            // Re-initialize mine level from the new location since we teleported
            mineLevelInitialized = false;
            lastProcessedFromGridId = fromGridId;
            lastProcessedToGridId = toGridId;
            lastProcessedTime = now;
            return;
        }

        // Mark this transition as processed
        lastProcessedFromGridId = fromGridId;
        lastProcessedToGridId = toGridId;
        lastProcessedTime = now;

        // Wait a moment for gobs to load after grid change
        // This is called from tick(), so we can't do async waiting - just retry on next tick if needed
        // But we can wait a short time for gobs to appear
        try {
            Thread.sleep(100);  // Brief wait for gobs to load
        } catch (InterruptedException e) {
            return;
        }

        // Portal detection uses cachedLastActionsGob (captured from getLastActions() before grid change)
        // This works for both manual and automated navigation since the game tracks all clicks
        // If we know what portal was clicked, search for the SPECIFIC expected exit using getDoorPair()
        // This prevents phantom portals from proximity matching the wrong portal type
        Gob exitPortal = null;
        String expectedExitName = null;

        // First, try to determine the expected exit from what we clicked
        if (cachedLastActionsGob != null && cachedLastActionsGob.ngob != null) {
            String clickedName = cachedLastActionsGob.ngob.name;
            expectedExitName = GateDetector.getDoorPair(clickedName);
        }

        // Search for the exit portal
        // We know exactly what exit to look for - search specifically for it
        exitPortal = Finder.findGob(new NAlias(expectedExitName));

        if (exitPortal == null || exitPortal.ngob == null) {
            return;
        }

        String exitName = exitPortal.ngob.name;

        // LAYER CHECK: Verify this is actually a portal traversal, not just walking across a grid boundary
        // If we didn't click a portal and the layers are the same (or unknown), we just walked across a boundary
        // This prevents recording phantom portals when player walks past buildings/portals near grid edges
        //
        // Check if we actually CLICKED any portal (cachedLastActionsGob).
        // If yes, we intentionally used a portal and should record.
        // If no, we just walked past and should apply strict layer checking.
        boolean clickedAnyPortal = cachedLastActionsGob != null && cachedLastActionsGob.ngob != null;

        if (!clickedAnyPortal) {
            // We didn't click any portal - be CONSERVATIVE about recording
            String fromLayer = getLayerFromChunk(fromGridId);
            String predictedToLayer = predictLayerFromPortal(exitName);

            // If we can't determine layers, DON'T record - we can't verify this is a real portal traversal
            // If layers are the same, DON'T record - this is just walking across a grid boundary
            boolean layersUnknown = (fromLayer == null || predictedToLayer == null);
            boolean layersSame = isSameLayerType(fromLayer, predictedToLayer);

            if (layersUnknown || layersSame) {
                // Either can't determine layers or same layer type - not a verified portal traversal
                // Clear tracking state and return without recording
                cachedLastActionsGob = null;
                cachedLastActionsGobLocalCoord = null;
                cachedLastActionsGobGridId = -1;
                return;
            }
        }

        String exitHash = getPortalHash(exitPortal);
        String entranceName = GateDetector.getDoorPair(exitName);

        // Use PLAYER's current position - this is where we land after exiting, the accessible spot
        Coord exitLocalCoord = getGobLocalCoord(player);

        // Update the destination chunk's layer based on the exit portal
        updateChunkLayer(toGridId, exitName);

        // Record: entrance portal on its actual grid connects to toGrid
        // Try multiple strategies to get the actual entrance portal position
        // We determine entranceGridId first so we can use it for the exit portal's back-connection
        long entranceGridId = fromGridId;  // Default to player's grid, but prefer portal's actual grid

        if (entranceName != null) {
            Coord entranceCoord = null;
            String entranceHash = null;

            // Strategy 1: Use cached lastActions gob (captured BEFORE grid change, like routes system)
            // This is the most reliable way because we captured it while the grid was still loaded
            if (cachedLastActionsGob != null && cachedLastActionsGob.ngob != null &&
                cachedLastActionsGobLocalCoord != null) {
                String cachedName = cachedLastActionsGob.ngob.name;
                // Verify this is the entrance portal we're looking for
                // Use strict matching: must be exact match OR cachedName must be the building (entranceName)
                // NOT the reverse (don't match stonemansion-door when looking for stonemansion)
                if (cachedName != null && isPortalGob(cachedName) &&
                    (cachedName.equals(entranceName) ||
                     cachedName.endsWith("/" + getSimpleName(entranceName)))) {
                    entranceCoord = cachedLastActionsGobLocalCoord;
                    entranceHash = getPortalHash(cachedLastActionsGob);
                    // Use the portal's actual grid ID (fixes boundary bug)
                    if (cachedLastActionsGobGridId != -1) {
                        entranceGridId = cachedLastActionsGobGridId;
                    }
                }
            }

            if (entranceCoord != null && entranceHash != null) {
                recordPortalConnection(entranceHash, entranceName, entranceGridId, toGridId, entranceCoord);

                // Update entry portal with where we appear in destination
                updatePortalExitCoord(entranceHash, entranceGridId, exitLocalCoord);

                // Update exit portal with where we came from (enables reverse navigation)
                updatePortalExitCoord(exitHash, toGridId, entranceCoord);
            }
        }

        // Record: exit portal on toGrid connects back to entrance portal's grid
        // This uses entranceGridId which was determined from the portal's actual location (fixes boundary bug)
        recordPortalConnection(exitHash, exitName, toGridId, entranceGridId, exitLocalCoord);

        // Mark the cached portal as processed so tick() won't re-capture it
        if (cachedLastActionsGob != null) {
            lastProcessedPortalGobId = cachedLastActionsGob.id;
        }

        // Clear tracking state after use
        cachedLastActionsGob = null;
        cachedLastActionsGobLocalCoord = null;
        cachedLastActionsGobGridId = -1;
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
            fromChunk = createMinimalChunk(fromGridId);
            if (fromChunk == null) {
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
        } else {
            // Update existing portal with new hash and position if provided
            portal.gobHash = gobHash;  // Update hash in case it was a synthetic one
            if (localCoord != null) {
                portal.localCoord = portalCoord;
            }
        }

        // Update the connection
        portal.connectsToGridId = toGridId;
        portal.lastTraversed = System.currentTimeMillis();

        // Also notify recorder
        recorder.recordPortalTraversal(gobHash, toGridId);
    }

    /**
     * Update a portal's exit coordinate (where you appear after using it).
     * This is called after we've found the exit portal to record the exact exit position.
     */
    private void updatePortalExitCoord(String gobHash, long chunkId, Coord exitLocalCoord) {
        if (exitLocalCoord == null) return;

        ChunkNavData chunk = graph.getChunk(chunkId);
        if (chunk == null) return;

        ChunkPortal portal = chunk.findPortal(gobHash);
        if (portal != null) {
            portal.exitLocalCoord = exitLocalCoord;
        }
    }

    /**
     * Get the grid ID of the grid containing a gob.
     * Returns -1 if the grid cannot be determined.
     */
    private long getGobGridId(Gob gob) {
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            Coord tileCoord = gob.rc.floor(MCache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            if (grid != null) {
                return grid.id;
            }
        } catch (Exception e) {
            // Ignore
        }
        return -1;
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
     * Get the appropriate local coordinate for a portal gob.
     * For buildings (stonemansion, etc.), offsets from center toward player to get door position.
     * For other portals (doors, cellars), uses the gob's actual position.
     */
    private Coord getPortalLocalCoord(Gob portalGob, Gob player) {
        if (portalGob == null || portalGob.ngob == null) {
            return null;
        }

        String name = portalGob.ngob.name;
        if (name == null) {
            return getGobLocalCoord(portalGob);
        }

        // Buildings need offset toward player to get door position
        // These are whole-building gobs where the door is on the edge, not at center
        double offset = getBuildingDoorOffset(name);
        if (offset > 0 && player != null) {
            return getDoorLocalCoord(portalGob, player, offset);
        }

        // Regular portals (doors, cellars, gates) - use actual gob position
        return getGobLocalCoord(portalGob);
    }

    /**
     * Get the door offset for a building type.
     * Returns 0 for non-building portals (use gob position directly).
     */
    private double getBuildingDoorOffset(String gobName) {
        if (gobName == null) return 0;
        String lower = gobName.toLowerCase();

        // Interior doors (seen from inside) - no offset needed
        if (lower.contains("-door")) return 0;

        // Buildings where the gob is the whole structure and door is on the edge
        // Offset is approximate distance from center to door in tiles
        if (lower.contains("stonemansion")) return 6;
        if (lower.contains("logcabin")) return 3;
        if (lower.contains("timberhouse")) return 3;
        if (lower.contains("stonestead")) return 4;
        if (lower.contains("greathall")) return 5;
        if (lower.contains("stonetower")) return 3;
        if (lower.contains("windmill")) return 3;

        return 0; // Not a building, use direct position
    }

    /**
     * Get the "door position" for a building gob by offsetting from building center toward player.
     * This gives a stable position for the door even if player stands at slightly different spots.
     * @param buildingGob The building gob (e.g., stonemansion)
     * @param player The player gob
     * @param offsetTiles How many tiles to offset toward player (e.g., 5 for stonemansion)
     */
    private Coord getDoorLocalCoord(Gob buildingGob, Gob player, double offsetTiles) {
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;

            // Get building center in world coords
            Coord2d buildingPos = buildingGob.rc;
            Coord2d playerPos = player.rc;

            // Calculate direction from building to player
            Coord2d direction = playerPos.sub(buildingPos);
            double dist = direction.dist(Coord2d.z);
            if (dist < 1.0) {
                // Player is at building center, just use building pos
                return getGobLocalCoord(buildingGob);
            }

            // Normalize and scale by offset
            Coord2d normalized = new Coord2d(direction.x / dist, direction.y / dist);
            Coord2d doorPos = buildingPos.add(normalized.mul(offsetTiles * MCache.tilesz.x));

            // Convert to tile coord
            Coord tileCoord = doorPos.floor(MCache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            if (grid != null) {
                return tileCoord.sub(grid.ul);
            }
        } catch (Exception e) {
            // Fallback to building center
        }
        return getGobLocalCoord(buildingGob);
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
     * Get the simple name from a full resource path.
     * e.g., "gfx/terobjs/arch/stonemansion" -> "stonemansion"
     */
    private String getSimpleName(String fullName) {
        if (fullName == null) return "";
        int lastSlash = fullName.lastIndexOf('/');
        return lastSlash >= 0 ? fullName.substring(lastSlash + 1) : fullName;
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
     * Also updates mine level tracking when traversing mine portals.
     * @param exitPortalName The name of the exit portal (the portal you see after traversing)
     * @return The layer name for the destination grid
     */
    private String determineLayerFromExitPortal(String exitPortalName) {
        if (exitPortalName == null) return null;

        String lowerName = exitPortalName.toLowerCase();

        // Mine level tracking based on exit portal:
        // - If we see a LADDER as exit, we went DOWN through a minehole (level++)
        //   (ladder is what takes you back UP, so seeing it means we descended)
        // - If we see a MINEHOLE as exit, we went UP through a ladder (level--)
        //   (minehole is what takes you DOWN, so seeing it means we ascended)
        if (lowerName.contains("ladder")) {
            // We went DOWN into mine (minehole entrance -> ladder exit)
            currentMineLevel++;
            return "mine" + currentMineLevel;
        }
        if (lowerName.contains("minehole")) {
            // We went UP out of mine (ladder entrance -> minehole exit)
            currentMineLevel--;
            if (currentMineLevel <= 0) {
                currentMineLevel = 0;
                return "surface";
            }
            return "mine" + currentMineLevel;
        }

        // Check each mapping for non-mine portals
        for (Map.Entry<String, String> entry : PORTAL_TO_LAYER.entrySet()) {
            String key = entry.getKey().toLowerCase();
            // Skip mine-related entries - handled above
            if (key.equals("minehole") || key.contains("ladder")) {
                continue;
            }
            if (lowerName.contains(key)) {
                // Reset mine level when going to surface
                if ("surface".equals(entry.getValue())) {
                    currentMineLevel = 0;
                }
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
     * For mine levels, also updates all currently loaded grids to the same level.
     */
    private void updateChunkLayer(long gridId, String exitPortalName) {
        String layer = determineLayerFromExitPortal(exitPortalName);
        if (layer == null) {
            return;
        }

        // Update the target chunk
        ChunkNavData chunk = graph.getChunk(gridId);
        if (chunk != null && !layer.equals(chunk.layer)) {
            chunk.layer = layer;
        }

        // For mine levels, update ALL currently loaded grids to the same level
        // This ensures all grids at this mine level get the correct layer
        if (layer.startsWith("mine")) {
            updateAllLoadedGridsToMineLevel(layer, gridId);
        }
    }

    /**
     * Update all currently loaded grids to the specified mine level.
     * Called after we determine the mine level from portal traversal.
     * @param mineLayer The mine layer (e.g., "mine1", "mine2")
     * @param excludeGridId Grid to exclude (already updated)
     */
    private void updateAllLoadedGridsToMineLevel(String mineLayer, long excludeGridId) {
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            if (mcache == null) return;

            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == excludeGridId) continue;

                    ChunkNavData chunk = graph.getChunk(grid.id);
                    if (chunk != null) {
                        // Only update if chunk doesn't have a proper mine level yet,
                        // or if it has the wrong level
                        if (chunk.layer == null ||
                            chunk.layer.equals("mine") ||
                            chunk.layer.equals("surface") ||
                            (chunk.layer.startsWith("mine") && !chunk.layer.equals(mineLayer))) {
                            chunk.layer = mineLayer;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - grids might not be available
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
     * Check if the player is standing on a hearthfire.
     * This indicates a teleport (Hearth Fire skill) rather than walking through a portal.
     */
    private boolean isPlayerOnHearthfire(Gob player) {
        if (player == null) return false;

        try {
            // Search for fire gobs near the player
            // Fire gob resource is "gfx/terobjs/pow", but only model attribute 17 is a hearthfire
            ArrayList<Gob> fires = Finder.findGobs(new NAlias("gfx/terobjs/pow"));

            for (Gob fire : fires) {
                // Check if this is actually a hearthfire (model attribute 17)
                if (fire.ngob == null || fire.ngob.getModelAttribute() != 17) {
                    continue;
                }

                double dist = player.rc.dist(fire.rc);
                // If player is very close to a hearthfire (within ~2 tiles), they likely teleported
                if (dist < 11.0) {  // ~2 tiles in world units
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        return false;
    }

    /**
     * Reset tracking state.
     */
    public void reset() {
        lastGridId = -1;
        lastProcessedFromGridId = -1;
        lastProcessedToGridId = -1;
        lastProcessedTime = 0;
        currentMineLevel = 0;
        mineLevelInitialized = false;  // Will re-initialize on next tick
        cachedLastActionsGob = null;
        cachedLastActionsGobLocalCoord = null;
        cachedLastActionsGobGridId = -1;
        lastProcessedPortalGobId = -1;
    }

    /**
     * Initialize mine level from the current grid's stored layer.
     * Called on first tick after reset/login to restore correct mine depth.
     */
    private void initializeMineLevelFromCurrentGrid(long gridId) {
        mineLevelInitialized = true;  // Mark as initialized even if we fail, to avoid repeated attempts

        ChunkNavData chunk = graph.getChunk(gridId);
        if (chunk == null || chunk.layer == null) {
            // No stored data - assume surface
            currentMineLevel = 0;
            return;
        }

        // Parse mine level from layer string (e.g., "mine3" -> 3)
        if (chunk.layer.startsWith("mine")) {
            try {
                String levelStr = chunk.layer.substring(4);  // Remove "mine" prefix
                int level = Integer.parseInt(levelStr);
                currentMineLevel = Math.max(0, level);
            } catch (NumberFormatException e) {
                // Legacy "mine" without number - assume level 1
                currentMineLevel = 1;
            }
        } else {
            // Not in a mine (surface, inside, cellar, etc.)
            currentMineLevel = 0;
        }
    }

    /**
     * Get the current mine level (for debugging/display).
     * @return 0 if on surface, 1+ for mine levels
     */
    public int getCurrentMineLevel() {
        return currentMineLevel;
    }

    /**
     * Get the layer of a chunk from the graph.
     * Returns null if chunk doesn't exist or has no layer set.
     */
    private String getLayerFromChunk(long gridId) {
        ChunkNavData chunk = graph.getChunk(gridId);
        return chunk != null ? chunk.layer : null;
    }

    /**
     * Predict what layer we would be in based on a nearby portal, without modifying state.
     * This is used to check if a grid transition was actually a portal traversal.
     * Returns null if we can't determine the layer.
     */
    private String predictLayerFromPortal(String portalName) {
        if (portalName == null) return null;
        String lowerName = portalName.toLowerCase();

        // Mine portals - we can tell it's mine-related
        if (lowerName.contains("ladder")) {
            // Seeing a ladder means we went DOWN (through minehole) - we're in a mine
            return "mine";  // Generic mine indicator
        }
        if (lowerName.contains("minehole")) {
            // Seeing a minehole means we went UP (through ladder)
            // If we were in mine1, we're now on surface
            // If we were in deeper mine, we're still in mine
            return currentMineLevel > 1 ? "mine" : "surface";
        }

        // Check standard mappings (excluding mine-related which are handled above)
        for (Map.Entry<String, String> entry : PORTAL_TO_LAYER.entrySet()) {
            String key = entry.getKey().toLowerCase();
            // Skip mine-related (handled above)
            if (key.equals("minehole") || key.contains("ladder")) continue;
            if (lowerName.contains(key)) {
                return entry.getValue();
            }
        }

        // Default for doors
        if (lowerName.endsWith("-door") || lowerName.contains("-door")) {
            return "inside";
        }

        return null;
    }

    /**
     * Check if two layers are the same "type" (meaning crossing between them does NOT require a portal).
     * Returns true if they're the same type AND you can walk between grids without a portal.
     * If either is null, returns false (can't determine, so assume portal was used).
     *
     * Layer types where same→same means NO portal:
     * - surface: Can walk between surface grids without portal
     * - mine, mine1, mine2: Can walk between mine grids at same level without portal
     *
     * Layer types where same→same DOES require a portal:
     * - inside: All building floors are "inside" - stairs between floors are portals
     * - cellar: Unlikely to span multiple grids, but if so, treat as portal
     */
    private boolean isSameLayerType(String layer1, String layer2) {
        if (layer1 == null || layer2 == null) return false;

        // "inside" layers ALWAYS require portals to move between grids
        // (stairs between floors, doors between rooms in multi-grid buildings)
        if ("inside".equals(layer1) || "inside".equals(layer2)) return false;

        // "cellar" - treat transitions as portal-required to be safe
        if ("cellar".equals(layer1) || "cellar".equals(layer2)) return false;

        // Exact match for surface
        if (layer1.equals(layer2)) return true;

        // Both are mine types - can walk between grids in the same mine level without portals
        if (layer1.startsWith("mine") && layer2.startsWith("mine")) return true;

        return false;
    }
}
