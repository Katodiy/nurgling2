package nurgling.navigation;

import haven.*;
import nurgling.NConfig;
import nurgling.NCore;
import nurgling.NUtils;
import nurgling.tasks.GateDetector;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

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
    // Only "inside" and "cellar" are special - everything else is "outside" (walkable between grids)
    private static final Map<String, String> PORTAL_TO_LAYER = new HashMap<>();
    static {
        // Cellar
        PORTAL_TO_LAYER.put("cellardoor", "inside");     // Exiting cellar -> inside building
        PORTAL_TO_LAYER.put("cellarstairs", "cellar");   // Entering cellar -> cellar

        // Building interiors (entering from outside)
        PORTAL_TO_LAYER.put("stonemansion-door", "inside");
        PORTAL_TO_LAYER.put("logcabin-door", "inside");
        PORTAL_TO_LAYER.put("timberhouse-door", "inside");
        PORTAL_TO_LAYER.put("stonestead-door", "inside");
        PORTAL_TO_LAYER.put("greathall-door", "inside");
        PORTAL_TO_LAYER.put("stonetower-door", "inside");
        PORTAL_TO_LAYER.put("windmill-door", "inside");

        // Stairs between floors (still inside)
        PORTAL_TO_LAYER.put("downstairs", "inside");
        PORTAL_TO_LAYER.put("upstairs", "inside");

        // Everything else (exiting buildings, mines) -> outside
        // Buildings, minehole, ladder all lead to "outside" (walkable between grids)
    }

    // Portal resource patterns to track
    // Note: "gate" is intentionally excluded - gates are passthrough openings, not teleporting portals
    // Buildings are included because clicking them teleports you inside (the door is implicit)
    private static final String[] PORTAL_PATTERNS = {
        "door",
        "cellar",
        "minehole",
        "ladder",
        "stairs",
        // Buildings - clicking these teleports you inside
        "stonemansion",
        "logcabin",
        "timberhouse",
        "stonestead",
        "greathall",
        "stonetower",
        "windmill"
    };

    public PortalTraversalTracker(ChunkNavGraph graph, ChunkNavRecorder recorder) {
        this.graph = graph;
        this.recorder = recorder;
    }

    /**
     * Call this periodically to check for portal traversals.
     * Safe to call frequently - internally throttled.
     */
    public void tick() {
        // Skip if ChunkNav overlay is disabled
        Object val = NConfig.get(NConfig.Key.chunkNavOverlay);
        if (!(val instanceof Boolean) || !(Boolean) val) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime = now;
        try {
            doCheck();
        } catch (InterruptedException e) {

        }
    }

    private void doCheck() throws InterruptedException {
        Gob player = NUtils.player();
        if (player == null) return;

        long currentGridId = graph.getPlayerChunkId();
        if (currentGridId == -1) return;

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
                // Use player's grid, not portal gob's grid - this ensures the portal is recorded
                // in the chunk where the player accesses it from (fixes large building boundary bug)
                cachedLastActionsGobGridId = currentGridId;
            }
        }
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
            lastProcessedFromGridId = fromGridId;
            lastProcessedToGridId = toGridId;
            lastProcessedTime = now;
            return;
        }

        // Mark this transition as processed
        lastProcessedFromGridId = fromGridId;
        lastProcessedToGridId = toGridId;
        lastProcessedTime = now;

        // Brief wait for gobs to load after grid change
        // This blocks the main thread but is necessary because:
        // 1. We need exit portal gobs to be loaded to find them
        // 2. We can't retry on next tick (duplicate prevention would skip)
        // 3. 100ms is short enough to not noticeably affect gameplay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        // Portal detection uses cachedLastActionsGob (captured from getLastActions() before grid change)
        // This works for both manual and automated navigation since the game tracks all clicks
        // If we know what portal was clicked, search for the SPECIFIC expected exit using getDoorPair()
        // This prevents phantom portals from proximity matching the wrong portal type
        Gob exitPortal;
        String expectedExitName = null;

        // Determine the expected exit from what we clicked
        if (cachedLastActionsGob != null && cachedLastActionsGob.ngob != null) {
            String clickedName = cachedLastActionsGob.ngob.name;
            expectedExitName = GateDetector.getDoorPair(clickedName);
        }

        // If we don't know what exit to look for, we didn't click a known portal - don't record anything
        if (expectedExitName == null) {
            return;
        }

        // Search for the specific exit portal we expect
        exitPortal = Finder.findGob(new NAlias(expectedExitName));

        if (exitPortal == null || exitPortal.ngob == null) {
            return;
        }

        String exitName = exitPortal.ngob.name;
        String exitHash = getPortalHash(exitPortal);
        String entranceName = GateDetector.getDoorPair(exitName);

        // Use PLAYER's current position - this is where we land after exiting, the accessible spot
        Coord exitLocalCoord = getGobLocalCoord(player);

        // Update the destination chunk's layer based on the exit portal
        updateChunkLayer(toGridId, exitName);

        // Record: entrance portal on its actual grid connects to toGrid
        // We determine entranceGridId first so we can use it for the exit portal's back-connection
        long entranceGridId = fromGridId;  // Default to player's grid, but prefer portal's actual grid

        if (entranceName != null) {
            Coord entranceCoord = null;
            String entranceHash = null;

            // Use cached lastActions gob (captured BEFORE grid change, like routes system)
            // This is the most reliable way because we captured it while the grid was still loaded
            if (cachedLastActionsGob != null && cachedLastActionsGob.ngob != null &&
                cachedLastActionsGobLocalCoord != null) {
                String cachedName = cachedLastActionsGob.ngob.name;
                // Verify this is the entrance portal we're looking for
                // Use strict matching: must be exact match OR cachedName must be the building (entranceName)
                // NOT the reverse (don't match stonemansion-door when looking for stonemansion)
                if (isPortalGob(cachedName) &&
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

        // Use provided localCoord or default to center (in tile coordinates 0-99)
        Coord portalCoord = localCoord != null ? localCoord : new Coord(CHUNK_SIZE / 2, CHUNK_SIZE / 2);

        // Find or create the portal - check by hash first, then by position+name
        // Do NOT use findPortalByName - that causes all buildings with same name to merge
        // Use position+name to avoid merging different portal types at same location (e.g., cellardoor vs stonemansion-door)
        ChunkPortal portal = fromChunk.findPortal(gobHash);
        if (portal == null && gobName != null) {
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
        if (lower.contains("greathall")) return 8;
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
                        Coord gridCoord = grid.ul.div(CHUNK_SIZE);
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
     * Only "inside" and "cellar" are special - everything else is "outside".
     * @param exitPortalName The name of the exit portal (the portal you see after traversing)
     * @return The layer name for the destination grid
     */
    private String determineLayerFromExitPortal(String exitPortalName) {
        if (exitPortalName == null) return null;

        String lowerName = exitPortalName.toLowerCase();

        // Check PORTAL_TO_LAYER for inside/cellar mappings
        for (Map.Entry<String, String> entry : PORTAL_TO_LAYER.entrySet()) {
            if (lowerName.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Default: if it ends with "-door" it's likely inside a building
        if (lowerName.contains("-door")) {
            return "inside";
        }

        // Everything else (surface, mines, etc.) is "outside"
        return "outside";
    }

    /**
     * Update the layer of a chunk based on the exit portal.
     * Called when we traverse to a new grid and find an exit portal.
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
        cachedLastActionsGob = null;
        cachedLastActionsGobLocalCoord = null;
        cachedLastActionsGobGridId = -1;
        lastProcessedPortalGobId = -1;
    }
}
