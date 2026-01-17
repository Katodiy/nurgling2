package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * A* pathfinding on the chunk graph.
 * Plans global paths from player position to target areas.
 */
public class ChunkNavPlanner {
    private final ChunkNavGraph graph;
    private final UnifiedTilePathfinder unifiedPathfinder;

    public ChunkNavPlanner(ChunkNavGraph graph) {
        this.graph = graph;
        this.unifiedPathfinder = new UnifiedTilePathfinder(graph);
    }

    /**
     * Check if a tile (in tile coordinates 0-99) is walkable.
     * With half-tile resolution, a tile is walkable if any of its 4 cells is walkable.
     */
    private boolean isTileWalkable(ChunkNavData chunk, int tileX, int tileY) {
        // Convert tile to cell coordinates
        int cellX = tileX * CELLS_PER_TILE;
        int cellY = tileY * CELLS_PER_TILE;

        // Check all cells in the 2x2 block - tile is walkable if any cell is walkable
        for (int dx = 0; dx < CELLS_PER_TILE; dx++) {
            for (int dy = 0; dy < CELLS_PER_TILE; dy++) {
                int cx = cellX + dx;
                int cy = cellY + dy;
                if (cx >= 0 && cx < CELLS_PER_EDGE && cy >= 0 && cy < CELLS_PER_EDGE) {
                    if (chunk.walkability[cx][cy] == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Plan a path from player's current position to a target area.
     * Uses the unified tile pathfinder to build a complete path through all chunks.
     */
    public ChunkPath planToArea(NArea area) {
        if (area == null) return null;

        // Get player's current chunk and local position using direct MCache lookup
        // This is more reliable than ngob.grid_id which can be stale
        PlayerLocation playerLoc = getPlayerLocation();
        if (playerLoc == null) {
            return null;
        }

        long startChunkId = playerLoc.gridId;
        Coord playerLocal = playerLoc.localCoord;

        // Find target chunk and local coord using STORED data (not live visibility)
        TargetLocation target = findTargetLocation(area);
        if (target == null) {
            return null;
        }

        // Use unified pathfinder to get complete tile-level path
        UnifiedTilePathfinder.UnifiedPath unifiedPath = unifiedPathfinder.findPath(
            startChunkId, playerLocal,
            target.chunkId, target.localCoord
        );

        if (unifiedPath == null || !unifiedPath.reachable) {
            return null;
        }

        // Convert to ChunkPath with segments
        ChunkPath path = new ChunkPath();
        unifiedPath.populateChunkPath(path, graph);

        // Truncate path at first tile that enters the target area
        // This prevents walking through the entire area to reach the far corner
        truncatePathAtAreaEntry(path, area);

        return path;
    }
    
    /**
     * Plan a path from player's current position to a specific world coordinate.
     * Used for planning paths to specific corners of an area.
     */
    public ChunkPath planToCoord(Coord2d worldCoord) {
        if (worldCoord == null) return null;

        // Get player's current chunk and local position
        PlayerLocation playerLoc = getPlayerLocation();
        if (playerLoc == null) {
            return null;
        }

        long startChunkId = playerLoc.gridId;
        Coord playerLocal = playerLoc.localCoord;

        // Convert world coord to tile coord
        Coord targetTile = worldCoord.floor(MCache.tilesz);
        
        // Find which chunk contains this tile
        TargetLocation target = findTargetLocationForTile(targetTile);
        if (target == null) {
            return null;
        }

        // Use unified pathfinder to get complete tile-level path
        UnifiedTilePathfinder.UnifiedPath unifiedPath = unifiedPathfinder.findPath(
            startChunkId, playerLocal,
            target.chunkId, target.localCoord
        );

        if (unifiedPath == null || !unifiedPath.reachable) {
            return null;
        }

        // Convert to ChunkPath with segments
        ChunkPath path = new ChunkPath();
        unifiedPath.populateChunkPath(path, graph);

        return path;
    }
    
    /**
     * Find the target chunk and local coordinate for a specific world tile.
     */
    private TargetLocation findTargetLocationForTile(Coord targetTile) {
        try {
            // Try loaded grid first
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map != null && gui.map.glob != null) {
                MCache mcache = gui.map.glob.map;
                MCache.Grid grid = mcache.getgridt(targetTile);
                if (grid != null) {
                    Coord localCoord = targetTile.sub(grid.ul);
                    if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                        localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                        // Check if walkable
                        ChunkNavData chunk = graph.getChunk(grid.id);
                        if (chunk != null) {
                            Coord walkable = findWalkableTileNear(chunk, localCoord);
                            if (walkable != null) {
                                return new TargetLocation(grid.id, walkable);
                            }
                        }
                        return new TargetLocation(grid.id, localCoord);
                    }
                }
            }
            
            // Search recorded chunks by worldTileOrigin
            for (ChunkNavData chunk : graph.getAllChunks()) {
                if (chunk.worldTileOrigin == null) continue;

                Coord localCoord = targetTile.sub(chunk.worldTileOrigin);
                if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                    localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                    Coord walkable = findWalkableTileNear(chunk, localCoord);
                    if (walkable != null) {
                        return new TargetLocation(chunk.gridId, walkable);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Find the target chunk and local coordinate for an area using stored data.
     * Does NOT rely on live visibility - uses stored area data and chunk worldTileOrigins.
     * IMPORTANT: We find a WALKABLE tile near the area edge, not the center.
     * The area center might be blocked by objects inside the area.
     */
    private TargetLocation findTargetLocation(NArea area) {
        // Strategy 1: Use area's stored grid references
        if (area.space != null && area.space.space != null) {
            for (Long gridId : area.space.space.keySet()) {
                ChunkNavData chunk = graph.getChunk(gridId);
                if (chunk != null) {
                    // Get area bounds in this chunk and find walkable tile near edge
                    Coord walkableNearArea = findWalkableNearAreaEdge(area, chunk);
                    if (walkableNearArea != null) {
                        return new TargetLocation(gridId, walkableNearArea);
                    }
                }
            }
        }

        // Strategy 2: Search all recorded chunks for one that contains the area
        Coord2d areaCenter = getAreaCenterFromStored(area);
        if (areaCenter != null) {
            Coord areaTile = areaCenter.floor(MCache.tilesz);

            for (ChunkNavData chunk : graph.getAllChunks()) {
                if (chunk.worldTileOrigin == null) continue;

                Coord localCoord = areaTile.sub(chunk.worldTileOrigin);
                if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                    localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                    // Find walkable tile near this point
                    Coord walkable = findWalkableTileNear(chunk, localCoord);
                    if (walkable != null) {
                        return new TargetLocation(chunk.gridId, walkable);
                    }
                }
            }
        }

        // Strategy 3: Fallback - try live visibility (may not work if area not visible)
        Set<Long> targetChunks = graph.getChunksForArea(area.id);
        if (targetChunks.isEmpty()) {
            targetChunks = findChunksNearArea(area);
        }

        for (Long chunkId : targetChunks) {
            Coord areaLocal = getAreaLocalCoord(area, chunkId);
            if (areaLocal != null) {
                ChunkNavData chunk = graph.getChunk(chunkId);
                if (chunk != null) {
                    Coord walkable = findWalkableTileNear(chunk, areaLocal);
                    if (walkable != null) {
                        return new TargetLocation(chunkId, walkable);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find a walkable tile near the edge of an area.
     * Searches outward from the area bounds to find accessible tiles.
     */
    private Coord findWalkableNearAreaEdge(NArea area, ChunkNavData chunk) {
        if (area.space == null || area.space.space == null) return null;

        NArea.VArea varea = area.space.space.get(chunk.gridId);
        if (varea == null || varea.area == null) return null;

        // Get area bounds in local chunk coordinates
        int minX = Math.max(0, varea.area.ul.x);
        int minY = Math.max(0, varea.area.ul.y);
        int maxX = Math.min(CHUNK_SIZE - 1, varea.area.br.x);
        int maxY = Math.min(CHUNK_SIZE - 1, varea.area.br.y);

        // Search around the edges of the area for walkable tiles
        // Prefer outside (dist=2,1) then fallback to inside (dist=0)
        int[] distances = {2, 1, 0};
        for (int dist : distances) {
            // Check around the perimeter at this distance
            for (int x = minX - dist; x <= maxX + dist; x++) {
                for (int y = minY - dist; y <= maxY + dist; y++) {
                    // Only check tiles at exactly 'dist' distance from area edge
                    boolean onPerimeter = (x == minX - dist || x == maxX + dist ||
                                          y == minY - dist || y == maxY + dist);
                    if (!onPerimeter && dist > 0) continue;

                    if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE) {
                        if (isTileWalkable(chunk, x, y)) {
                            return new Coord(x, y);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if a specific cell is walkable.
     */
    private boolean isCellWalkable(ChunkNavData chunk, int cellX, int cellY) {
        if (cellX >= 0 && cellX < CELLS_PER_EDGE && cellY >= 0 && cellY < CELLS_PER_EDGE) {
            return chunk.walkability[cellX][cellY] == 0;
        }
        return false;
    }
    
    /**
     * Find a walkable tile near the given coordinate.
     * Searches in cell grid (half-tile resolution, ~5.5 world units per step)
     * with max radius of 4 cells from the target.
     */
    private Coord findWalkableTileNear(ChunkNavData chunk, Coord target) {
        // Convert target tile to cell coordinates (center of the tile)
        int targetCellX = target.x * CELLS_PER_TILE;
        int targetCellY = target.y * CELLS_PER_TILE;
        
        // Check the target cell itself first
        if (isCellWalkable(chunk, targetCellX, targetCellY)) {
            return target;
        }

        // Search in expanding rings around the target in CELL coordinates
        // Max radius = 4 cells = 2 tiles = ~22 world units
        final int MAX_CELL_RADIUS = 4;
        
        for (int radius = 1; radius <= MAX_CELL_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue; // Only perimeter

                    int cx = targetCellX + dx;
                    int cy = targetCellY + dy;
                    
                    if (isCellWalkable(chunk, cx, cy)) {
                        // Convert cell back to tile
                        int tileX = cx / CELLS_PER_TILE;
                        int tileY = cy / CELLS_PER_TILE;
                        if (tileX >= 0 && tileX < CHUNK_SIZE && tileY >= 0 && tileY < CHUNK_SIZE) {
                            return new Coord(tileX, tileY);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get area center from stored data (not live visibility).
     */
    private Coord2d getAreaCenterFromStored(NArea area) {
        // Try to get from stored space data
        if (area.space != null && area.space.space != null && !area.space.space.isEmpty()) {
            // Calculate center from stored VArea regions
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

            for (Map.Entry<Long, NArea.VArea> entry : area.space.space.entrySet()) {
                ChunkNavData chunk = graph.getChunk(entry.getKey());
                if (chunk == null || chunk.worldTileOrigin == null) continue;

                NArea.VArea varea = entry.getValue();
                if (varea.area != null) {
                    // VArea.area defines local tile bounds within the chunk
                    Coord ul = chunk.worldTileOrigin.add(varea.area.ul);
                    Coord br = chunk.worldTileOrigin.add(varea.area.br);
                    minX = Math.min(minX, ul.x);
                    minY = Math.min(minY, ul.y);
                    maxX = Math.max(maxX, br.x);
                    maxY = Math.max(maxY, br.y);
                }
            }

            if (minX != Integer.MAX_VALUE) {
                int centerX = (minX + maxX) / 2;
                int centerY = (minY + maxY) / 2;
                return new Coord(centerX, centerY).mul(MCache.tilesz).add(MCache.tilehsz);
            }
        }

        // Fallback to getCenter2d (requires visibility)
        return area.getCenter2d();
    }

    /**
     * Target location result.
     */
    private static class TargetLocation {
        final long chunkId;
        final Coord localCoord;

        TargetLocation(long chunkId, Coord localCoord) {
            this.chunkId = chunkId;
            this.localCoord = localCoord;
        }
    }

    /**
     * Player location result.
     */
    private static class PlayerLocation {
        final long gridId;
        final Coord localCoord;

        PlayerLocation(long gridId, Coord localCoord) {
            this.gridId = gridId;
            this.localCoord = localCoord;
        }
    }

    /**
     * Get player's current grid ID and local coordinate using direct MCache lookup.
     * This is more reliable than ngob.grid_id which can be stale after teleports.
     */
    private PlayerLocation getPlayerLocation() {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui == null || gui.map == null || gui.map.glob == null) {
                return null;
            }

            Gob player = NUtils.player();
            if (player == null) return null;

            MCache mcache = gui.map.glob.map;
            Coord playerTile = player.rc.floor(MCache.tilesz);

            // Direct lookup - most reliable
            MCache.Grid grid = mcache.getgridt(playerTile);
            if (grid != null) {
                Coord localCoord = playerTile.sub(grid.ul);
                // Validate
                if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                    localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                    return new PlayerLocation(grid.id, localCoord);
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ============= Path truncation for efficient area navigation =============

    /**
     * Check if a local coordinate is inside the area bounds for a chunk.
     */
    private boolean isInsideVArea(Coord localCoord, NArea.VArea varea) {
        if (varea == null || varea.area == null) return false;

        int minX = varea.area.ul.x;
        int minY = varea.area.ul.y;
        int maxX = varea.area.br.x;
        int maxY = varea.area.br.y;

        return localCoord.x >= minX && localCoord.x <= maxX &&
               localCoord.y >= minY && localCoord.y <= maxY;
    }

    /**
     * Truncate the path at the first tile that enters the target area.
     * This prevents walking through an entire area to reach the far corner.
     * Instead, navigation stops as soon as we enter the area bounds.
     */
    private void truncatePathAtAreaEntry(ChunkPath path, NArea area) {
        if (path == null || area == null) return;
        if (area.space == null || area.space.space == null) return;
        if (path.segments == null || path.segments.isEmpty()) return;

        for (int segIdx = 0; segIdx < path.segments.size(); segIdx++) {
            ChunkPath.PathSegment segment = path.segments.get(segIdx);

            // Check if the area exists in this chunk
            NArea.VArea varea = area.space.space.get(segment.gridId);
            if (varea == null || varea.area == null) {
                continue; // Area not in this chunk, keep going
            }

            // Area exists in this chunk - check each step
            for (int stepIdx = 0; stepIdx < segment.steps.size(); stepIdx++) {
                ChunkPath.TileStep step = segment.steps.get(stepIdx);

                if (isInsideVArea(step.localCoord, varea)) {
                    // Found entry point! Truncate here.

                    // Keep steps up to and including this one
                    if (stepIdx + 1 < segment.steps.size()) {
                        segment.steps = new ArrayList<>(segment.steps.subList(0, stepIdx + 1));
                    }

                    // Remove all subsequent segments
                    if (segIdx + 1 < path.segments.size()) {
                        path.segments = new ArrayList<>(path.segments.subList(0, segIdx + 1));
                    }

                    // Mark this segment as destination
                    segment.type = ChunkPath.SegmentType.WALK;

                    return; // Done truncating
                }
            }
        }
        // If we get here, path never entered the area (shouldn't happen for valid paths)
    }

    // ============= Legacy chunk-level A* methods (kept for compatibility) =============

    /**
     * Find chunks that contain or are near an area.
     * Uses multiple strategies:
     * 1. Direct grid ID match from area's stored grid references
     * 2. World coordinate matching if area is visible
     * 3. WorldTileOrigin matching for unloaded chunks
     */
    private Set<Long> findChunksNearArea(NArea area) {
        Set<Long> result = new HashSet<>();

        try {
            // Strategy 1: Direct grid ID match from area's stored grid references
            // Areas store which grid IDs they occupy - check if we have those chunks recorded
            if (area.space != null && area.space.space != null) {
                for (Long gridId : area.space.space.keySet()) {
                    if (graph.hasChunk(gridId)) {
                        result.add(gridId);
                    }
                }
            }

            if (!result.isEmpty()) {
                return result;
            }

            // Strategy 2: If area is visible, use world coordinates
            Coord2d areaCenter = area.getCenter2d();
            if (areaCenter != null) {
                Coord areaTile = areaCenter.floor(MCache.tilesz);

                // Try loaded grid first
                try {
                    MCache mcache = NUtils.getGameUI().map.glob.map;
                    MCache.Grid grid = mcache.getgridt(areaTile);
                    if (grid != null && graph.hasChunk(grid.id)) {
                        result.add(grid.id);
                        return result;
                    }
                } catch (Exception e) {
                    // Grid not loaded
                }

                // Search recorded chunks by worldTileOrigin
                for (ChunkNavData chunk : graph.getAllChunks()) {
                    if (chunk.worldTileOrigin == null) continue;

                    if (areaTile.x >= chunk.worldTileOrigin.x && areaTile.x < chunk.worldTileOrigin.x + CHUNK_SIZE &&
                        areaTile.y >= chunk.worldTileOrigin.y && areaTile.y < chunk.worldTileOrigin.y + CHUNK_SIZE) {
                        result.add(chunk.gridId);
                    }
                }
            }

        } catch (Exception e) {
            // Ignore search errors
        }

        return result;
    }

    /**
     * Get the local coordinate of an area's center within the specified chunk.
     * Returns null if the area is not in that chunk or coordinates cannot be determined.
     */
    private Coord getAreaLocalCoord(NArea area, long chunkGridId) {
        if (area == null) return null;

        try {
            // Get area center - getCenter2d returns the center in world coordinates
            Coord2d areaCenter = area.getCenter2d();
            if (areaCenter == null) return null;

            Coord areaTile = areaCenter.floor(MCache.tilesz);

            // Try to find the grid in MCache
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map != null && gui.map.glob != null) {
                MCache mcache = gui.map.glob.map;
                synchronized (mcache.grids) {
                    for (MCache.Grid grid : mcache.grids.values()) {
                        if (grid.id == chunkGridId) {
                            Coord localCoord = areaTile.sub(grid.ul);
                            // Check if the area is actually in this chunk
                            if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                                localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                                return localCoord;
                            }
                            return null; // Area not in this chunk
                        }
                    }
                }
            }

            // Fallback: use stored worldTileOrigin
            ChunkNavData chunk = graph.getChunk(chunkGridId);
            if (chunk != null && chunk.worldTileOrigin != null) {
                Coord localCoord = areaTile.sub(chunk.worldTileOrigin);
                // Check if the area is actually in this chunk
                if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                    localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                    return localCoord;
                }
            }

        } catch (Exception e) {
        }
        return null;
    }
}
