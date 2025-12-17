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
     * Merges new observations with existing data - tiles observed as walkable update
     * tiles that were previously unknown (blocked due to not being visible).
     * Re-samples every time to accumulate walkability as player moves around.
     */
    public void recordGrid(MCache.Grid grid) {
        if (grid == null || grid.ul == null) return;

        try {
            Coord gridCoord = gridToCoord(grid);
            if (gridCoord == null) return;

            // Check for existing data to merge with
            ChunkNavData existing = graph.getChunk(grid.id);
            ChunkNavData chunk;

            if (existing != null) {
                // Merge new observations with existing data
                chunk = existing;
                chunk.gridCoord = gridCoord;
                chunk.worldTileOrigin = grid.ul;
                int beforeObs = chunk.countObservedCells();
                mergeWalkability(grid, chunk);
                int afterObs = chunk.countObservedCells();
                if (afterObs != beforeObs) {
                    System.out.println("ChunkNavRecorder: Grid " + grid.id + " MERGE observed " + beforeObs + " -> " + afterObs);
                }
            } else {
                // New chunk - create fresh data
                chunk = new ChunkNavData(grid.id, gridCoord, grid.ul);
                sampleWalkability(grid, chunk);
                System.out.println("ChunkNavRecorder: Grid " + grid.id + " NEW SAMPLE");
            }

            detectPortals(grid, chunk);
            updateEdgeWalkability(chunk);
            chunk.markUpdated();

            graph.addChunk(chunk);
            graph.updateConnections(chunk);

            // Debug: count observed tiles
            int obsCount = chunk.countObservedCells();
            System.out.println("ChunkNavRecorder: Added/updated chunk " + grid.id + " to graph, observed=" + obsCount + ", graph now has " + graph.getChunkCount() + " chunks");

        } catch (Exception e) {
            // Grid may have become invalid during recording
            System.err.println("ChunkNavRecorder: Error recording grid " + grid.id + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Merge new walkability observations with existing chunk data.
     * Tiles observed as walkable in the new scan update tiles that were blocked.
     * This allows building up walkability knowledge as player explores different parts of the grid.
     */
    private void mergeWalkability(MCache.Grid grid, ChunkNavData chunk) {
        MCache mcache = getMCache();
        if (mcache == null) return;

        // Get player's position to determine visible area
        Coord playerTile = null;
        try {
            Gob player = NUtils.player();
            if (player != null) {
                playerTile = player.rc.floor(MCache.tilesz);
            }
        } catch (Exception e) {
            return; // Can't determine visible area
        }

        if (playerTile == null) return;

        // Build set of tiles blocked by gobs (more efficient for tile-level)
        Set<Long> gobBlockedTiles = getGobBlockedTiles(grid);

        int visibleCount = 0;
        int newlyObserved = 0;
        int updatedToWalkable = 0;
        int updatedToBlocked = 0;

        for (int tx = 0; tx < CELLS_PER_EDGE; tx++) {
            for (int ty = 0; ty < CELLS_PER_EDGE; ty++) {
                // Calculate world tile coordinate
                Coord worldTile = grid.ul.add(tx, ty);

                // Check if this tile is within visible range
                int distX = Math.abs(worldTile.x - playerTile.x);
                int distY = Math.abs(worldTile.y - playerTile.y);
                boolean isVisible = distX <= VISIBLE_RADIUS_TILES && distY <= VISIBLE_RADIUS_TILES;

                if (!isVisible) {
                    // Tile not visible now - keep existing data
                    continue;
                }

                visibleCount++;

                // Tile is visible - check terrain and gobs
                boolean terrainBlocked = isTileBlocked(mcache, worldTile);
                long tileKey = ((long) worldTile.x << 32) | (worldTile.y & 0xFFFFFFFFL);
                boolean gobBlocked = gobBlockedTiles.contains(tileKey);

                // Determine new walkability (tile-level: only 0 or 2)
                byte newWalkability = (terrainBlocked || gobBlocked) ? (byte) 2 : (byte) 0;

                // Track if newly observed
                if (!chunk.observed[tx][ty]) {
                    newlyObserved++;
                }

                // Mark tile as observed (we actually saw it)
                chunk.observed[tx][ty] = true;

                // Always update walkability for observed tiles
                byte existing = chunk.walkability[tx][ty];
                if (newWalkability != existing) {
                    if (newWalkability == 0) {
                        updatedToWalkable++;
                    } else {
                        updatedToBlocked++;
                    }
                }
                chunk.walkability[tx][ty] = newWalkability;
            }
        }

        // Only log if something interesting happened
        if (newlyObserved > 0 || updatedToWalkable > 0 || updatedToBlocked > 0) {
            System.out.println("ChunkNavRecorder: Grid " + grid.id + " merge - visible=" + visibleCount +
                ", newlyObserved=" + newlyObserved + ", updatedWalkable=" + updatedToWalkable +
                ", updatedBlocked=" + updatedToBlocked + ", playerTile=" + playerTile + ", gridUL=" + grid.ul);
        }
    }

    /**
     * Convert grid to a coordinate for spatial indexing.
     */
    private Coord gridToCoord(MCache.Grid grid) {
        // Use grid's upper-left tile coordinate divided by chunk size
        return grid.ul.div(CHUNK_SIZE);
    }

    // Visible area is approximately 81x81 tiles centered on player (9 grids * 100 units / 11 tilesz)
    // Use a conservative radius in tiles for walkability sampling
    private static final int VISIBLE_RADIUS_TILES = 40;

    /**
     * Sample walkability at tile-level resolution (1 tile per cell).
     * Only tiles within the player's visible area are sampled for gob collisions.
     * Tiles outside visible area are marked as blocked (unknown/unsafe).
     */
    private void sampleWalkability(MCache.Grid grid, ChunkNavData chunk) {
        MCache mcache = getMCache();
        if (mcache == null) return;

        // Get player's position to determine visible area
        Coord playerTile = null;
        try {
            Gob player = NUtils.player();
            if (player != null) {
                playerTile = player.rc.floor(MCache.tilesz);
            }
        } catch (Exception e) {
            // Ignore - will mark everything as unknown
        }

        // Build set of tiles blocked by gobs (more efficient for tile-level)
        Set<Long> gobBlockedTiles = getGobBlockedTiles(grid);

        int visibleCount = 0;
        int terrainBlockedCount = 0;
        int gobBlockedCount = 0;
        int walkableCount = 0;

        for (int tx = 0; tx < CELLS_PER_EDGE; tx++) {
            for (int ty = 0; ty < CELLS_PER_EDGE; ty++) {
                // Calculate world tile coordinate
                Coord worldTile = grid.ul.add(tx, ty);

                // Check if this tile is within visible range
                boolean isVisible = false;
                if (playerTile != null) {
                    int distX = Math.abs(worldTile.x - playerTile.x);
                    int distY = Math.abs(worldTile.y - playerTile.y);
                    isVisible = distX <= VISIBLE_RADIUS_TILES && distY <= VISIBLE_RADIUS_TILES;
                }

                if (!isVisible) {
                    // Tile is outside visible area - mark as blocked (unknown/unsafe)
                    chunk.walkability[tx][ty] = 2;  // Blocked (unknown - not visible)
                    chunk.observed[tx][ty] = false;  // Not observed
                    continue;
                }

                visibleCount++;
                // Tile is visible - mark as observed and check terrain and gobs
                chunk.observed[tx][ty] = true;

                // Check terrain
                boolean terrainBlocked = isTileBlocked(mcache, worldTile);

                // Check gob hitboxes
                long tileKey = ((long) worldTile.x << 32) | (worldTile.y & 0xFFFFFFFFL);
                boolean gobBlocked = gobBlockedTiles.contains(tileKey);

                // Classify tile: 0 = walkable, 2 = blocked
                // No "partial" at tile level - a tile is either walkable or not
                if (terrainBlocked) {
                    terrainBlockedCount++;
                    chunk.walkability[tx][ty] = 2;  // Blocked
                } else if (gobBlocked) {
                    gobBlockedCount++;
                    chunk.walkability[tx][ty] = 2;  // Blocked
                } else {
                    walkableCount++;
                    chunk.walkability[tx][ty] = 0;  // Walkable
                }
            }
        }

        System.out.println("ChunkNavRecorder: Grid " + grid.id + " sampling - visible=" + visibleCount +
            ", terrainBlocked=" + terrainBlockedCount + ", gobBlocked=" + gobBlockedCount +
            ", walkable=" + walkableCount + ", gobBlockedTilesSize=" + gobBlockedTiles.size());
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
     * Get set of all tiles blocked by gobs in this grid.
     * Returns tile keys as (x << 32) | y for efficient lookup.
     */
    private Set<Long> getGobBlockedTiles(MCache.Grid grid) {
        Set<Long> blockedTiles = new HashSet<>();

        try {
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            Coord2d gridTL = grid.ul.mul(MCache.tilesz);
            Coord2d gridBR = grid.ul.add(CHUNK_SIZE, CHUNK_SIZE).mul(MCache.tilesz);

            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob == null || gob.ngob.hitBox == null) continue;
                    if (gob.getattr(Following.class) != null) continue; // Skip following

                    // Check if gob is near this grid
                    if (gob.rc.x < gridTL.x - 50 || gob.rc.x >= gridBR.x + 50 ||
                            gob.rc.y < gridTL.y - 50 || gob.rc.y >= gridBR.y + 50) {
                        continue;
                    }

                    // Get hitbox and mark all tiles it covers
                    CellsArray ca = gob.ngob.getCA();
                    if (ca != null && ca.cells != null && ca.begin != null) {
                        // ca.cells is a 2D array where cells[x][y] != 0 means blocked
                        // ca.begin is the top-left corner of the hitbox in pathfinder grid coords (half-tile resolution)
                        // We need to convert from pf grid to tile coords
                        for (int i = 0; i < ca.x_len; i++) {
                            for (int j = 0; j < ca.y_len; j++) {
                                if (ca.cells[i][j] != 0) {
                                    // Convert from pathfinder grid (half-tile) to world coords, then to tile coords
                                    Coord pfCoord = ca.begin.add(i, j);
                                    // pfGridToWorld: pfCoord * tilehsz (5.5)
                                    Coord2d worldCoord = pfCoord.mul(MCache.tilehsz);
                                    // World to tile: divide by tilesz (11)
                                    Coord blockedTile = worldCoord.div(MCache.tilesz).floor();

                                    long tileKey = ((long) blockedTile.x << 32) | (blockedTile.y & 0xFFFFFFFFL);
                                    blockedTiles.add(tileKey);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        return blockedTiles;
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
     * Clear session tracking (no-op since we resample every time now).
     */
    public void clearSession() {
        // No longer tracking session - we resample every time
    }

    /**
     * Get statistics about recording.
     */
    public String getStats() {
        return String.format("ChunkNavRecorder[chunks=%d]", graph.getChunkCount());
    }
}
