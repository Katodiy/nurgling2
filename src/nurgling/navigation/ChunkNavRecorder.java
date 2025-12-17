package nurgling.navigation;

import haven.*;
import nurgling.NGob;
import nurgling.NUtils;
import nurgling.pf.CellsArray;
import nurgling.tasks.GateDetector;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;
import static nurgling.navigation.ChunkNavData.Direction;

/**
 * Records navigation data as the player explores the world.
 * Hooks into grid loading events and samples walkability.
 */
public class ChunkNavRecorder {
    private final ChunkNavGraph graph;
    private final Set<Long> recordedThisSession = new HashSet<>();

    // Blocked tile patterns
    private static final Set<String> BLOCKED_TILES = new HashSet<>(Arrays.asList(
            "gfx/tiles/cave",
            "gfx/tiles/rocks",
            "gfx/tiles/deep",
            "gfx/tiles/odeep",
            "gfx/tiles/nil"
    ));

    public ChunkNavRecorder(ChunkNavGraph graph) {
        this.graph = graph;
    }

    /**
     * Record navigation data for a grid that just became visible.
     */
    public void recordGrid(MCache.Grid grid) {
        if (grid == null || grid.ul == null) return;

        // Check if already recorded this session with recent data
        ChunkNavData existing = graph.getChunk(grid.id);
        if (existing != null && existing.getCurrentConfidence() > 0.8f) {
            // Already have good data, just update connections
            graph.updateConnections(existing);
            return;
        }

        try {
            Coord gridCoord = gridToCoord(grid);
            if (gridCoord == null) return;

            // Store both gridCoord and the actual world tile origin
            ChunkNavData chunk = new ChunkNavData(grid.id, gridCoord, grid.ul);
            sampleWalkability(grid, chunk);
            detectPortals(grid, chunk);
            updateEdgeWalkability(chunk);

            graph.addChunk(chunk);
            graph.updateConnections(chunk);
            recordedThisSession.add(grid.id);

        } catch (Exception e) {
            // Grid may have become invalid during recording
            System.err.println("ChunkNavRecorder: Error recording grid " + grid.id + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convert grid to a coordinate for spatial indexing.
     */
    private Coord gridToCoord(MCache.Grid grid) {
        // Use grid's upper-left tile coordinate divided by chunk size
        return grid.ul.div(CHUNK_SIZE);
    }

    /**
     * Sample walkability at coarse resolution (4x4 tiles per cell).
     */
    private void sampleWalkability(MCache.Grid grid, ChunkNavData chunk) {
        MCache mcache = getMCache();
        if (mcache == null) return;

        for (int cx = 0; cx < CELLS_PER_EDGE; cx++) {
            for (int cy = 0; cy < CELLS_PER_EDGE; cy++) {
                int blocked = 0;
                int total = 0;

                // Sample 4x4 tile area
                for (int tx = 0; tx < COARSE_CELL_SIZE; tx++) {
                    for (int ty = 0; ty < COARSE_CELL_SIZE; ty++) {
                        Coord localTile = new Coord(cx * COARSE_CELL_SIZE + tx, cy * COARSE_CELL_SIZE + ty);
                        Coord worldTile = grid.ul.add(localTile);

                        if (isTileBlocked(mcache, worldTile)) {
                            blocked++;
                        }
                        total++;
                    }
                }

                // Check for gob hitboxes in this cell
                int gobBlocked = countBlockedByGobs(grid, cx, cy);
                blocked += gobBlocked;

                // Classify cell
                float blockRatio = (float) blocked / (total + gobBlocked);
                if (blockRatio < 0.25f) {
                    chunk.walkability[cx][cy] = 0;  // Walkable
                } else if (blockRatio < 0.75f) {
                    chunk.walkability[cx][cy] = 1;  // Partially blocked
                } else {
                    chunk.walkability[cx][cy] = 2;  // Fully blocked
                }
            }
        }
    }

    /**
     * Check if a tile is blocked by terrain.
     */
    private boolean isTileBlocked(MCache mcache, Coord tileCoord) {
        try {
            String tileName = mcache.tilesetname(mcache.gettile(tileCoord));
            if (tileName == null) return false;

            for (String blocked : BLOCKED_TILES) {
                if (tileName.startsWith(blocked) || tileName.equals(blocked)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false; // Tile not loaded
        }
    }

    /**
     * Count how many sub-tiles in a coarse cell are blocked by gobs.
     */
    private int countBlockedByGobs(MCache.Grid grid, int cx, int cy) {
        int blocked = 0;

        try {
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            Coord cellWorldTL = grid.ul.add(cx * COARSE_CELL_SIZE, cy * COARSE_CELL_SIZE);
            Coord2d cellWorldTL2d = cellWorldTL.mul(MCache.tilesz);
            Coord2d cellWorldBR2d = cellWorldTL2d.add(COARSE_CELL_SIZE * MCache.tilesz.x, COARSE_CELL_SIZE * MCache.tilesz.y);

            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob == null || gob.ngob.hitBox == null) continue;
                    if (gob.getattr(Following.class) != null) continue; // Skip following

                    // Check if gob is in this cell
                    if (gob.rc.x >= cellWorldTL2d.x && gob.rc.x < cellWorldBR2d.x &&
                            gob.rc.y >= cellWorldTL2d.y && gob.rc.y < cellWorldBR2d.y) {

                        // Estimate blocked area based on hitbox
                        CellsArray ca = gob.ngob.getCA();
                        if (ca != null) {
                            blocked += Math.min(ca.x_len * ca.y_len / 4, COARSE_CELL_SIZE * COARSE_CELL_SIZE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        return blocked;
    }

    /**
     * Detect portals (doors, stairs, etc.) in the grid.
     */
    private void detectPortals(MCache.Grid grid, ChunkNavData chunk) {
        try {
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            Coord2d gridTL = grid.ul.mul(MCache.tilesz);
            Coord2d gridBR = grid.ul.add(CHUNK_SIZE, CHUNK_SIZE).mul(MCache.tilesz);

            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob == null || gob.ngob.name == null) continue;

                    // Check if gob is in this grid
                    if (gob.rc.x < gridTL.x || gob.rc.x >= gridBR.x ||
                            gob.rc.y < gridTL.y || gob.rc.y >= gridBR.y) {
                        continue;
                    }

                    // Check if it's a portal
                    ChunkPortal.PortalType type = ChunkPortal.classifyPortal(gob.ngob.name);
                    if (type != null) {
                        Coord localCoord = worldToLocalCoord(gob.rc, grid);

                        // Generate hash if not available
                        String gobHash = gob.ngob.hash;
                        if (gobHash == null || gobHash.isEmpty()) {
                            // Create a fallback hash from gob id and position
                            gobHash = "gob_" + gob.id + "_" + grid.id;
                        }

                        ChunkPortal portal = new ChunkPortal(
                                gobHash,
                                gob.ngob.name,
                                type,
                                localCoord
                        );
                        chunk.addOrUpdatePortal(portal);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ChunkNavRecorder: Error detecting portals: " + e.getMessage());
        }
    }

    /**
     * Convert world coordinate to local chunk coordinate.
     */
    private Coord worldToLocalCoord(Coord2d worldCoord, MCache.Grid grid) {
        Coord tileCoord = worldCoord.floor(MCache.tilesz);
        return tileCoord.sub(grid.ul);
    }

    /**
     * Update edge walkability based on walkability grid.
     */
    private void updateEdgeWalkability(ChunkNavData chunk) {
        for (int i = 0; i < CELLS_PER_EDGE; i++) {
            // North edge (y = 0)
            chunk.northEdge[i].walkable = chunk.walkability[i][0] <= 1;

            // South edge (y = max)
            chunk.southEdge[i].walkable = chunk.walkability[i][CELLS_PER_EDGE - 1] <= 1;

            // West edge (x = 0)
            chunk.westEdge[i].walkable = chunk.walkability[0][i] <= 1;

            // East edge (x = max)
            chunk.eastEdge[i].walkable = chunk.walkability[CELLS_PER_EDGE - 1][i] <= 1;
        }
    }

    /**
     * Record a portal traversal (player went through a door/stairs).
     * This updates the portal's connection information.
     */
    public void recordPortalTraversal(String gobHash, long fromGridId, long toGridId) {
        ChunkPortal portal = graph.findPortal(gobHash);
        if (portal != null) {
            portal.connectsToGridId = toGridId;
            portal.lastTraversed = System.currentTimeMillis();
        }
    }

    /**
     * Check if two adjacent chunks can connect at their shared edge.
     * Called when both chunks are loaded.
     */
    public void updateEdgeConnectivity(ChunkNavData chunkA, ChunkNavData chunkB) {
        if (chunkA.gridCoord == null || chunkB.gridCoord == null) return;

        Direction dir = getDirection(chunkA.gridCoord, chunkB.gridCoord);
        if (dir == null) return; // Not adjacent

        EdgePoint[] edgeA = chunkA.getEdge(dir);
        EdgePoint[] edgeB = chunkB.getEdge(dir.opposite());

        boolean hasConnection = false;
        for (int i = 0; i < CELLS_PER_EDGE; i++) {
            // Both sides must be walkable
            boolean canCross = edgeA[i].walkable && edgeB[i].walkable;
            edgeA[i].walkable = canCross;
            edgeB[i].walkable = canCross;

            if (canCross) hasConnection = true;
        }

        // Update connected chunks
        if (hasConnection) {
            chunkA.connectedChunks.add(chunkB.gridId);
            chunkB.connectedChunks.add(chunkA.gridId);
        }
    }

    /**
     * Get direction from one grid coord to another.
     */
    private Direction getDirection(Coord from, Coord to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (Math.abs(dx) + Math.abs(dy) != 1) return null; // Not adjacent

        if (dx == 1) return Direction.EAST;
        if (dx == -1) return Direction.WEST;
        if (dy == 1) return Direction.SOUTH;
        if (dy == -1) return Direction.NORTH;

        return null;
    }

    /**
     * Get the MCache safely.
     */
    private MCache getMCache() {
        try {
            if (NUtils.getGameUI() == null || NUtils.getGameUI().map == null) return null;
            return NUtils.getGameUI().map.glob.map;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clear session tracking.
     */
    public void clearSession() {
        recordedThisSession.clear();
    }

    /**
     * Get statistics about recording.
     */
    public String getStats() {
        return String.format("ChunkNavRecorder[recorded=%d this session]", recordedThisSession.size());
    }
}
