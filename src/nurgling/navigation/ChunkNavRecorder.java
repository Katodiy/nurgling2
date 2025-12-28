package nurgling.navigation;

import haven.*;
import nurgling.NHitBox;
import nurgling.NUtils;
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
    // NOTE: "nil" = void/nothing, must be blocked (areas outside playable space)
    private static final Set<String> BLOCKED_TILES = new HashSet<>(Arrays.asList(
            "gfx/tiles/nil",
            "gfx/tiles/cave",
            "gfx/tiles/rocks",
            "gfx/tiles/deep",
            "gfx/tiles/odeep"
    ));

    /**
     * Snapshot of gob data for lock-free processing.
     * Captures only the fields needed for walkability and layer detection.
     */
    private static class GobSnapshot {
        final long id;
        final Coord2d rc;
        final double angle;
        final NHitBox hitBox;
        final String name;
        final boolean isFollowing;
        final long modelAttribute;  // For gate open/closed detection

        GobSnapshot(Gob gob) {
            this.id = gob.id;
            this.rc = gob.rc;
            this.angle = gob.a;
            this.hitBox = (gob.ngob != null) ? gob.ngob.hitBox : null;
            this.name = (gob.ngob != null) ? gob.ngob.name : null;
            this.isFollowing = gob.getattr(Following.class) != null;
            this.modelAttribute = (gob.ngob != null) ? gob.ngob.getModelAttribute() : -1L;
        }
    }

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
                mergeWalkability(grid, chunk);
            } else {
                // New chunk - create fresh data
                chunk = new ChunkNavData(grid.id, gridCoord, grid.ul);
                sampleWalkability(grid, chunk);
            }

            // Portals are recorded only when traversed (via PortalTraversalTracker)
            // This eliminates phantom portal bugs from proximity-based detection
            detectLayer(chunk);
            updateEdgeWalkability(chunk);
            discoverNeighbors(grid, chunk);
            chunk.markUpdated();

            graph.addChunk(chunk);
            graph.updateConnections(chunk);

        } catch (Exception e) {
        }
    }

    /**
     * Merge new walkability observations with existing chunk data.
     * Only records tiles within gob visibility range (~25 tiles radius).
     * Tiles outside visibility remain unobserved (blocked by default).
     */
    private void mergeWalkability(MCache.Grid grid, ChunkNavData chunk) {
        MCache mcache = getMCache();
        if (mcache == null) return;

        // Get player's tile position
        Coord playerTile = getPlayerTile();
        if (playerTile == null) return;

        // Build set of tiles blocked by gobs (only includes visible gobs)
        Set<Long> gobBlockedTiles = getGobBlockedTiles(grid);

        for (int tx = 0; tx < CELLS_PER_EDGE; tx++) {
            for (int ty = 0; ty < CELLS_PER_EDGE; ty++) {
                // Calculate world tile coordinate
                Coord worldTile = grid.ul.add(tx, ty);

                // Skip tiles outside visibility range
                int dx = Math.abs(worldTile.x - playerTile.x);
                int dy = Math.abs(worldTile.y - playerTile.y);
                if (dx > VISIBLE_RADIUS_TILES || dy > VISIBLE_RADIUS_TILES) {
                    continue;  // Leave as unobserved (blocked by default)
                }

                // Check terrain (deterministic from tile type)
                boolean terrainBlocked = isTileBlocked(mcache, worldTile);

                // Check gobs
                long tileKey = ((long) tx << 32) | (ty & 0xFFFFFFFFL);
                boolean gobBlocked = gobBlockedTiles.contains(tileKey);

                // Mark tile as observed (uses setObserved for quadrant count tracking)
                chunk.setObserved(tx, ty, true);

                // Record what we observe
                if (terrainBlocked) {
                    chunk.walkability[tx][ty] = 2;  // Blocked by terrain
                } else if (gobBlocked) {
                    chunk.walkability[tx][ty] = 2;  // Blocked by gob
                } else {
                    chunk.walkability[tx][ty] = 0;  // Walkable
                }
            }
        }
    }

    /**
     * Get the player's current tile coordinate.
     */
    private Coord getPlayerTile() {
        try {
            Gob player = NUtils.player();
            if (player != null) {
                return player.rc.floor(MCache.tilesz);
            }
        } catch (Exception e) {
            // Player not available
        }
        return null;
    }

    /**
     * Discover and record neighbor relationships by examining all currently loaded grids.
     * When multiple grids are loaded, we can see their spatial relationship through gc coordinates.
     * These relationships are persistent because grid IDs never change.
     */
    private void discoverNeighbors(MCache.Grid grid, ChunkNavData chunk) {
        try {
            MCache mcache = getMCache();
            if (mcache == null) return;

            Coord myGc = grid.gc;

            synchronized (mcache.grids) {
                for (MCache.Grid other : mcache.grids.values()) {
                    if (other.id == grid.id) continue;

                    Coord otherGc = other.gc;
                    int dx = otherGc.x - myGc.x;
                    int dy = otherGc.y - myGc.y;

                    // Check if this grid is an immediate neighbor (exactly 1 grid apart)
                    if (dx == 0 && dy == -1) {
                        // Other is to the north
                        chunk.neighborNorth = other.id;
                    } else if (dx == 0 && dy == 1) {
                        // Other is to the south
                        chunk.neighborSouth = other.id;
                    } else if (dx == 1 && dy == 0) {
                        // Other is to the east
                        chunk.neighborEast = other.id;
                    } else if (dx == -1 && dy == 0) {
                        // Other is to the west
                        chunk.neighborWest = other.id;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during neighbor discovery
        }
    }

    /**
     * Convert grid to a coordinate for spatial indexing.
     */
    private Coord gridToCoord(MCache.Grid grid) {
        // Use grid's upper-left tile coordinate divided by chunk size
        return grid.ul.div(CHUNK_SIZE);
    }

    // Gob visibility range is smaller than tile visibility range (~25-30 tiles vs ~40 tiles)
    // We must use the GOB visibility range, not tile visibility, to ensure we only
    // mark tiles as walkable when we can actually see all gobs on them.
    // Using 25 tiles to be conservative - if gobs aren't visible, don't trust the tile.
    private static final int VISIBLE_RADIUS_TILES = 25;

    /**
     * Sample walkability at tile-level resolution (1 tile per cell).
     * Only samples tiles within gob visibility range (~25 tiles radius).
     * Tiles outside visibility remain unobserved (blocked by default).
     */
    private void sampleWalkability(MCache.Grid grid, ChunkNavData chunk) {
        MCache mcache = getMCache();
        if (mcache == null) return;

        // Get player's tile position
        Coord playerTile = getPlayerTile();
        if (playerTile == null) return;

        // Build set of tiles blocked by gobs
        Set<Long> gobBlockedTiles = getGobBlockedTiles(grid);

        // DEBUG: Track floor tile bounds (non-nil tiles)
        int floorMinX = CELLS_PER_EDGE, floorMaxX = 0, floorMinY = CELLS_PER_EDGE, floorMaxY = 0;

        for (int tx = 0; tx < CELLS_PER_EDGE; tx++) {
            for (int ty = 0; ty < CELLS_PER_EDGE; ty++) {
                // Calculate world tile coordinate
                Coord worldTile = grid.ul.add(tx, ty);

                // Skip tiles outside visibility range
                int dx = Math.abs(worldTile.x - playerTile.x);
                int dy = Math.abs(worldTile.y - playerTile.y);
                if (dx > VISIBLE_RADIUS_TILES || dy > VISIBLE_RADIUS_TILES) {
                    continue;  // Leave as unobserved (blocked by default)
                }

                // Mark as observed (uses setObserved for quadrant count tracking)
                chunk.setObserved(tx, ty, true);

                // Check terrain
                boolean terrainBlocked = isTileBlocked(mcache, worldTile);

                // Check gob hitboxes (using local coordinates to match getGobBlockedTiles)
                long tileKey = ((long) tx << 32) | (ty & 0xFFFFFFFFL);
                boolean gobBlocked = gobBlockedTiles.contains(tileKey);

                // Classify tile: 0 = walkable, 2 = blocked
                if (terrainBlocked) {
                    chunk.walkability[tx][ty] = 2;  // Blocked
                } else {
                    // This is a floor tile (non-nil) - track bounds
                    floorMinX = Math.min(floorMinX, tx);
                    floorMaxX = Math.max(floorMaxX, tx);
                    floorMinY = Math.min(floorMinY, ty);
                    floorMaxY = Math.max(floorMaxY, ty);

                    if (gobBlocked) {
                        chunk.walkability[tx][ty] = 2;  // Blocked
                    } else {
                        chunk.walkability[tx][ty] = 0;  // Walkable
                    }
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
     * Get set of all tiles blocked by gobs in this grid.
     * Returns tile keys as (localX << 32) | localY for efficient lookup.
     * Computes blocked tiles directly from gob positions (gob.rc) relative to the grid,
     * avoiding CellsArray coordinate issues in cellars/buildings where coordinate spaces
     * may differ between sessions.
     */
    private Set<Long> getGobBlockedTiles(MCache.Grid grid) {
        Set<Long> blockedTiles = new HashSet<>();

        try {
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            long playerId = NUtils.player() != null ? NUtils.player().id : -1;

            // Grid bounds in world coordinates
            Coord2d gridWorldUL = grid.ul.mul(MCache.tilesz);
            Coord2d gridWorldBR = grid.ul.add(CHUNK_SIZE, CHUNK_SIZE).mul(MCache.tilesz);

            // Quick copy of gob data while holding lock - minimizes lock time
            List<GobSnapshot> snapshots = new ArrayList<>();
            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob == null || gob.ngob.hitBox == null) continue;
                    snapshots.add(new GobSnapshot(gob));
                }
            }

            // Process snapshots WITHOUT holding the lock - expensive operations here
            for (GobSnapshot snap : snapshots) {
                if (snap.hitBox == null) continue;
                if (snap.isFollowing) continue;

                // Skip player
                if (snap.id == playerId) continue;

                // Skip portals - doors, cellars, stairs should be passable
                // Gates are only passable when open
                if (isPassableGob(snap)) continue;

                // Quick bounds check - skip gobs clearly outside this grid
                if (snap.rc.x < gridWorldUL.x - 50 || snap.rc.x > gridWorldBR.x + 50 ||
                    snap.rc.y < gridWorldUL.y - 50 || snap.rc.y > gridWorldBR.y + 50) {
                    continue;
                }

                // Compute hitbox bounds directly from gob position
                nurgling.pf.NHitBoxD worldHitBox = new nurgling.pf.NHitBoxD(
                    snap.hitBox.begin, snap.hitBox.end, snap.rc, snap.angle
                );

                // Get circumscribed bounding box (axis-aligned after rotation)
                Coord2d hitUL = worldHitBox.getCircumscribedUL();
                Coord2d hitBR = worldHitBox.getCircumscribedBR();

                // Convert to tile coordinates
                Coord tileUL = hitUL.floor(MCache.tilesz);
                Coord tileBR = new Coord2d(hitBR.x - 0.01, hitBR.y - 0.01).floor(MCache.tilesz);

                // Mark all tiles covered by the hitbox
                for (int tx = tileUL.x; tx <= tileBR.x; tx++) {
                    for (int ty = tileUL.y; ty <= tileBR.y; ty++) {
                        int localX = tx - grid.ul.x;
                        int localY = ty - grid.ul.y;

                        if (localX >= 0 && localX < CHUNK_SIZE &&
                            localY >= 0 && localY < CHUNK_SIZE) {
                            long tileKey = ((long) localX << 32) | (localY & 0xFFFFFFFFL);
                            blockedTiles.add(tileKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle exceptions during gob iteration
        }

        return blockedTiles;
    }

    /**
     * Check if a gob should be considered passable (not blocking).
     * Only includes specific portal gobs that are traversable, NOT buildings themselves.
     * Gates are only passable when they are open.
     */
    private boolean isPassableGob(Gob gob) {
        if (gob == null || gob.ngob == null || gob.ngob.name == null) return false;
        String lower = gob.ngob.name.toLowerCase();

        // Indoor wall decorations are thin (about 1/8th of a tile)
        // They don't block tile access - the room boundary is defined by nil tiles
        // Specifically: hwall, vwall (horizontal/vertical walls inside buildings)
        // NOT palisade corners or other outdoor structures!
        if (lower.contains("/arch/") && (lower.contains("/hwall") || lower.contains("/vwall"))) return true;

        // Specific door gobs (the actual door objects, not buildings with doors)
        // These are the "-door" suffix objects like "stonemansion-door"
        if (lower.endsWith("-door")) return true;

        // Cellar door and stairs - the actual portal objects
        if (lower.equals("gfx/terobjs/cellardoor") || lower.contains("/cellardoor")) return true;
        if (lower.equals("gfx/terobjs/cellarstairs") || lower.contains("/cellarstairs")) return true;

        // Ladders
        if (lower.contains("/ladder")) return true;

        // All types of gates - only passable when OPEN (modelAttribute == 1)
        // Includes: polegate, polebiggate, palisadegate, palisadebiggate, drystonewallgate, drystonewallbiggate
        if (lower.contains("/polegate") || lower.contains("/polebiggate") ||
            lower.contains("/palisadegate") || lower.contains("/palisadebiggate") ||
            lower.contains("/drystonewallgate") || lower.contains("/drystonewallbiggate")) {
            // Check if gate is open using GateDetector logic
            return GateDetector.isDoorOpen(gob);
        }

        // Mine holes
        return lower.contains("/minehole");
    }

    /**
     * Check if a gob snapshot should be considered passable (not blocking).
     * Snapshot version for lock-free processing.
     */
    private boolean isPassableGob(GobSnapshot snap) {
        if (snap.name == null) return false;
        String lower = snap.name.toLowerCase();

        // Indoor wall decorations
        if (lower.contains("/arch/") && (lower.contains("/hwall") || lower.contains("/vwall"))) return true;

        // Door gobs
        if (lower.endsWith("-door")) return true;

        // Cellar door and stairs
        if (lower.equals("gfx/terobjs/cellardoor") || lower.contains("/cellardoor")) return true;
        if (lower.equals("gfx/terobjs/cellarstairs") || lower.contains("/cellarstairs")) return true;

        // Ladders
        if (lower.contains("/ladder")) return true;

        // Gates - only passable when OPEN (modelAttribute == 1)
        if (lower.contains("/polegate") || lower.contains("/polebiggate") ||
            lower.contains("/palisadegate") || lower.contains("/palisadebiggate") ||
            lower.contains("/drystonewallgate") || lower.contains("/drystonewallbiggate")) {
            return snap.modelAttribute == 1L;  // 1 = open
        }

        // Mine holes
        return lower.contains("/minehole");
    }

    /**
     * Detect the layer (outside/inside/cellar).
     * - "cellar" if we see cellarstairs
     * - "inside" if we see a door but no building exterior (we're inside a building)
     * - "outside" for everything else (surface, mines, etc.)
     */
    private void detectLayer(ChunkNavData chunk) {
        try {
            Glob glob = NUtils.getGameUI().ui.sess.glob;

            // Quick copy of gob names while holding lock
            List<String> gobNames = new ArrayList<>();
            synchronized (glob.oc) {
                for (Gob gob : glob.oc) {
                    if (gob.ngob != null && gob.ngob.name != null) {
                        gobNames.add(gob.ngob.name.toLowerCase());
                    }
                }
            }

            // Process WITHOUT holding the lock
            boolean hasCellarStairs = false;
            boolean hasDoor = false;
            boolean hasBuildingExterior = false;

            for (String name : gobNames) {
                if (name.contains("cellarstairs")) {
                    hasCellarStairs = true;
                } else if (name.endsWith("-door") || name.endsWith("door")) {
                    hasDoor = true;
                } else if (isBuildingExterior(name)) {
                    hasBuildingExterior = true;
                }
            }

            if (hasCellarStairs) {
                chunk.layer = "cellar";
            } else if (hasDoor && !hasBuildingExterior) {
                chunk.layer = "inside";
            } else {
                chunk.layer = "outside";
            }

        } catch (Exception e) {
            // Ignore layer detection errors
        }
    }

    /**
     * Check if a gob name is a building exterior (visible from outside).
     */
    private boolean isBuildingExterior(String name) {
        // Building exteriors - the full building gobs seen from outside
        // These are the main building gobs, NOT the -door variants
        if (name.contains("-door") || name.endsWith("door")) return false;

        return name.contains("stonemansion") ||
               name.contains("logcabin") ||
               name.contains("timberhouse") ||
               name.contains("stonestead") ||
               name.contains("greathall") ||
               name.contains("stonetower") ||
               name.contains("windmill");
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
    public void recordPortalTraversal(String gobHash, long toGridId) {
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
