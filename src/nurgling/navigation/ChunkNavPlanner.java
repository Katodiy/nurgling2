package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import haven.Pair;
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
     * Plan a path from player's current position to a target area.
     * Uses the unified tile pathfinder to build a complete path through all chunks.
     */
    public ChunkPath planToArea(NArea area) {
        if (area == null) return null;

        // Get player's current chunk and local position using direct MCache lookup
        // This is more reliable than ngob.grid_id which can be stale
        PlayerLocation playerLoc = getPlayerLocation();
        if (playerLoc == null) {
            System.out.println("ChunkNav: Cannot get player's current location");
            return null;
        }

        long startChunkId = playerLoc.gridId;
        Coord playerLocal = playerLoc.localCoord;

        System.out.println("ChunkNav: Planning path to area '" + area.name + "' from chunk " + startChunkId + " playerLocal=" + playerLocal);

        // Find target chunk and local coord using STORED data (not live visibility)
        TargetLocation target = findTargetLocation(area);
        if (target == null) {
            System.out.println("ChunkNav: Cannot determine target location for area '" + area.name + "'");
            return null;
        }

        System.out.println("ChunkNav: Target location: chunk=" + target.chunkId + " local=" + target.localCoord);

        // Use unified pathfinder to get complete tile-level path
        UnifiedTilePathfinder.UnifiedPath unifiedPath = unifiedPathfinder.findPath(
            startChunkId, playerLocal,
            target.chunkId, target.localCoord
        );

        if (unifiedPath == null || !unifiedPath.reachable) {
            System.out.println("ChunkNav: Unified pathfinder found no path");
            return null;
        }

        System.out.println("ChunkNav: Unified path found with " + unifiedPath.size() + " tile steps, cost=" + unifiedPath.cost);

        // Convert to ChunkPath with segments
        ChunkPath path = new ChunkPath();
        unifiedPath.populateChunkPath(path, graph);

        System.out.println("ChunkNav: Built " + path.segments.size() + " segments with " + path.getTotalTileSteps() + " total steps");

        return path;
    }

    /**
     * Find the target chunk and local coordinate for an area using stored data.
     * Does NOT rely on live visibility - uses stored area data and chunk worldTileOrigins.
     *
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
        // Check tiles just outside the area bounds first
        for (int dist = 0; dist <= 5; dist++) {
            // Check around the perimeter at this distance
            for (int x = minX - dist; x <= maxX + dist; x++) {
                for (int y = minY - dist; y <= maxY + dist; y++) {
                    // Only check tiles at exactly 'dist' distance from area edge
                    boolean onPerimeter = (x == minX - dist || x == maxX + dist ||
                                          y == minY - dist || y == maxY + dist);
                    if (!onPerimeter && dist > 0) continue;

                    if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE) {
                        if (chunk.walkability[x][y] == 0) {
                            return new Coord(x, y);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find a walkable tile near the given coordinate.
     */
    private Coord findWalkableTileNear(ChunkNavData chunk, Coord target) {
        // Check the target itself first
        if (target.x >= 0 && target.x < CHUNK_SIZE &&
            target.y >= 0 && target.y < CHUNK_SIZE &&
            chunk.walkability[target.x][target.y] == 0) {
            return target;
        }

        // Search in expanding rings around the target
        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue; // Only perimeter

                    int nx = target.x + dx;
                    int ny = target.y + dy;
                    if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE) {
                        if (chunk.walkability[nx][ny] == 0) {
                            return new Coord(nx, ny);
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
     * Get area's local coordinate within a chunk using stored data.
     */
    private Coord getAreaLocalCoordFromStored(NArea area, ChunkNavData chunk) {
        if (chunk.worldTileOrigin == null) return null;

        // Check if area has stored region in this chunk
        if (area.space != null && area.space.space != null) {
            NArea.VArea varea = area.space.space.get(chunk.gridId);
            if (varea != null && varea.area != null) {
                // Return center of the VArea region in this chunk
                int centerX = (varea.area.ul.x + varea.area.br.x) / 2;
                int centerY = (varea.area.ul.y + varea.area.br.y) / 2;
                return new Coord(centerX, centerY);
            }
        }

        // Fallback: calculate from area center
        Coord2d areaCenter = getAreaCenterFromStored(area);
        if (areaCenter == null) return null;

        Coord areaTile = areaCenter.floor(MCache.tilesz);
        Coord localCoord = areaTile.sub(chunk.worldTileOrigin);

        if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
            localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
            return localCoord;
        }

        return null;
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
            if (gui == null || gui.map == null || gui.map.glob == null || gui.map.glob.map == null) {
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
            System.err.println("ChunkNav: Error getting player location: " + e.getMessage());
            return null;
        }
    }

    // ============= Legacy chunk-level A* methods (kept for compatibility) =============

    /**
     * Plan a path from a start chunk to any of the target chunks.
     * Uses chunk-level A* (less precise than unified tile pathfinder).
     */
    public ChunkPath planPath(long startChunkId, Set<Long> targetChunkIds, NArea targetArea) {
        if (targetChunkIds.isEmpty()) {
            return null;
        }

        // Check if already at target
        if (targetChunkIds.contains(startChunkId)) {
            AStarNode node = new AStarNode(startChunkId);
            node.g = 0;
            return reconstructPath(node, targetArea);
        }

        // A* search
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Long, AStarNode> allNodes = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();

        // Initialize start node
        AStarNode startNode = new AStarNode(startChunkId);
        startNode.g = 0;
        startNode.h = heuristic(startChunkId, targetChunkIds);
        startNode.f = startNode.g + startNode.h;
        openSet.add(startNode);
        allNodes.put(startChunkId, startNode);

        int iterations = 0;
        int edgesExpanded = 0;
        while (!openSet.isEmpty() && iterations < 1000) {
            iterations++;
            AStarNode current = openSet.poll();

            System.out.println("ChunkNav: A* iteration " + iterations + " expanding " + current.gridId + " (g=" + current.g + ", f=" + current.f + ")");

            // Check if we reached a target
            if (targetChunkIds.contains(current.gridId)) {
                System.out.println("ChunkNav: A* reached target " + current.gridId);
                return reconstructPath(current, targetArea);
            }

            closedSet.add(current.gridId);

            // Expand neighbors
            List<ChunkNavGraph.ChunkEdge> edges = graph.getEdges(current.gridId);
            edgesExpanded += edges.size();
            for (ChunkNavGraph.ChunkEdge edge : edges) {
                if (closedSet.contains(edge.toGridId)) continue;

                float tentativeG = current.g + edge.cost;

                AStarNode neighbor = allNodes.get(edge.toGridId);
                if (neighbor == null) {
                    neighbor = new AStarNode(edge.toGridId);
                    neighbor.g = Float.MAX_VALUE;
                    allNodes.put(edge.toGridId, neighbor);
                }

                if (tentativeG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(edge.toGridId, targetChunkIds);
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.crossingPoint = edge.crossingPoint;
                    neighbor.portal = edge.portal;

                    System.out.println("ChunkNav: A* adding neighbor " + edge.toGridId + " (g=" + neighbor.g + ", h=" + neighbor.h + ", f=" + neighbor.f + ")");

                    // Remove and re-add to update priority
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        // No path found
        System.err.println("ChunkNav: A* found no path. iterations=" + iterations + " edges=" + edgesExpanded +
            " start=" + startChunkId + " targets=" + targetChunkIds);
        return null;
    }

    /**
     * Plan a path between two specific chunks.
     */
    public ChunkPath planPath(long startChunkId, long targetChunkId) {
        Set<Long> targets = new HashSet<>();
        targets.add(targetChunkId);
        return planPath(startChunkId, targets, null);
    }

    /**
     * Heuristic function for A* - estimates cost to reach any target.
     */
    private float heuristic(long fromGridId, Set<Long> targetChunkIds) {
        ChunkNavData fromChunk = graph.getChunk(fromGridId);
        if (fromChunk == null || fromChunk.gridCoord == null) {
            return Float.MAX_VALUE / 2;
        }

        float minDist = Float.MAX_VALUE;
        for (Long targetId : targetChunkIds) {
            ChunkNavData targetChunk = graph.getChunk(targetId);
            if (targetChunk != null && targetChunk.gridCoord != null) {
                float dist = (float)(fromChunk.gridCoord.dist(targetChunk.gridCoord) * BASE_CHUNK_COST);
                minDist = Math.min(minDist, dist);
            }
        }

        return minDist;
    }

    /**
     * Reconstruct path from goal node back to start.
     */
    private ChunkPath reconstructPath(AStarNode goalNode, NArea targetArea) {
        ChunkPath path = new ChunkPath();
        path.confidence = 1.0f;

        // Build list of nodes from start to goal
        List<AStarNode> nodes = new ArrayList<>();
        AStarNode current = goalNode;
        while (current != null) {
            nodes.add(0, current);
            path.confidence = Math.min(path.confidence, getChunkConfidence(current.gridId));
            current = current.parent;
        }

        // Convert to waypoints
        for (int i = 0; i < nodes.size(); i++) {
            AStarNode node = nodes.get(i);
            AStarNode prevNode = (i > 0) ? nodes.get(i - 1) : null;
            ChunkNavData chunk = graph.getChunk(node.gridId);

            // Handle portal waypoints specially
            // The portal is located in the PREVIOUS node's grid, not this node's grid
            if (node.portal != null && prevNode != null) {
                // Create waypoint to walk to portal in the PREVIOUS (source) grid
                ChunkPath.ChunkWaypoint portalWaypoint = new ChunkPath.ChunkWaypoint();
                portalWaypoint.gridId = prevNode.gridId;  // Portal is in source grid
                portalWaypoint.localCoord = node.portal.localCoord;
                portalWaypoint.type = ChunkPath.WaypointType.PORTAL_ENTRY;
                portalWaypoint.portal = node.portal;
                portalWaypoint.worldCoord = localToWorldCoord(prevNode.gridId, portalWaypoint.localCoord);


                path.addWaypoint(portalWaypoint);
                continue;  // Don't create another waypoint for this node
            }

            // Skip first waypoint (player's current position) unless it has a crossing point
            if (i == 0 && node.crossingPoint == null) {
                continue;
            }

            ChunkPath.ChunkWaypoint waypoint = new ChunkPath.ChunkWaypoint();
            waypoint.gridId = node.gridId;

            if (node.crossingPoint != null) {
                waypoint.localCoord = node.crossingPoint;
            } else if (chunk != null) {
                // Use center of chunk as default
                waypoint.localCoord = new Coord(CHUNK_SIZE / 2, CHUNK_SIZE / 2);
            } else {
                waypoint.localCoord = new Coord(CHUNK_SIZE / 2, CHUNK_SIZE / 2);
            }

            // Set waypoint type
            if (i == nodes.size() - 1) {
                waypoint.type = ChunkPath.WaypointType.DESTINATION;
                // If we have a target area, try to get a better destination point
                if (targetArea != null) {
                    Coord2d areaCenter = targetArea.getCenter2d();
                    if (areaCenter != null && chunk != null) {
                        waypoint.worldCoord = areaCenter;
                    }
                }
            } else {
                waypoint.type = ChunkPath.WaypointType.WALK;
            }

            // Calculate world coordinate if not set
            if (waypoint.worldCoord == null) {
                waypoint.worldCoord = localToWorldCoord(node.gridId, waypoint.localCoord);
            }

            path.addWaypoint(waypoint);
        }

        path.totalCost = goalNode.g;
        return path;
    }

    /**
     * Convert local chunk coordinate to world coordinate.
     * First tries loaded grids in MCache, then falls back to stored worldTileOrigin.
     */
    private Coord2d localToWorldCoord(long gridId, Coord localCoord) {
        // First try MCache for currently loaded grids
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == gridId) {
                        Coord tileCoord = grid.ul.add(localCoord);
                        return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
                    }
                }
            }
        } catch (Exception e) {
            // Grid not loaded in MCache
        }

        // Fall back to stored worldTileOrigin from ChunkNavData
        ChunkNavData chunk = graph.getChunk(gridId);
        if (chunk != null && chunk.worldTileOrigin != null) {
            Coord tileCoord = chunk.worldTileOrigin.add(localCoord);
            return tileCoord.mul(MCache.tilesz).add(MCache.tilehsz);
        }

        return null;
    }

    /**
     * Get confidence for a chunk.
     */
    private float getChunkConfidence(long gridId) {
        ChunkNavData chunk = graph.getChunk(gridId);
        return chunk != null ? chunk.getCurrentConfidence() : 0;
    }

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
     * Check if a path exists between two chunks.
     */
    public boolean pathExists(long startChunkId, long targetChunkId) {
        ChunkPath path = planPath(startChunkId, targetChunkId);
        return path != null && !path.isEmpty();
    }

    /**
     * Get estimated travel cost between two chunks.
     */
    public float estimateCost(long startChunkId, long targetChunkId) {
        ChunkPath path = planPath(startChunkId, targetChunkId);
        return path != null ? path.totalCost : Float.MAX_VALUE;
    }

    /**
     * Get the player's local coordinate within the specified chunk.
     * Returns null if the player is not in that chunk or position cannot be determined.
     */
    private Coord getPlayerLocalCoord(long chunkGridId) {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui == null || gui.map == null) return null;

            Gob player = NUtils.player();
            if (player == null) return null;

            MCache mcache = gui.map.glob.map;
            if (mcache == null) return null;

            Coord playerTile = player.rc.floor(MCache.tilesz);

            // Try to find the grid in MCache
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == chunkGridId) {
                        // Calculate local coord relative to grid origin
                        Coord localCoord = playerTile.sub(grid.ul);

                        // Check if local coord is valid (player actually in this grid)
                        if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                            localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                            return localCoord;
                        } else {
                            // Player is NOT in this grid - the grid ID is wrong
                            System.out.println("ChunkNav: Player tile " + playerTile + " not in grid " + chunkGridId +
                                " (ul=" + grid.ul + ", local=" + localCoord + ")");
                            // Try to find the correct grid
                            MCache.Grid correctGrid = mcache.getgridt(playerTile);
                            if (correctGrid != null) {
                                Coord correctLocal = playerTile.sub(correctGrid.ul);
                                System.out.println("ChunkNav: Player is actually in grid " + correctGrid.id + " at local " + correctLocal);
                            }
                            return null;
                        }
                    }
                }
            }

            // Grid not found in MCache - try stored worldTileOrigin
            ChunkNavData chunk = graph.getChunk(chunkGridId);
            if (chunk != null && chunk.worldTileOrigin != null) {
                Coord localCoord = playerTile.sub(chunk.worldTileOrigin);

                // Check if local coord is valid
                if (localCoord.x >= 0 && localCoord.x < CHUNK_SIZE &&
                    localCoord.y >= 0 && localCoord.y < CHUNK_SIZE) {
                    return localCoord;
                } else {
                    System.out.println("ChunkNav: Player tile " + playerTile + " not in stored chunk " + chunkGridId +
                        " (worldTileOrigin=" + chunk.worldTileOrigin + ", local=" + localCoord + ")");
                    return null;
                }
            }

            System.out.println("ChunkNav: Grid " + chunkGridId + " not found in MCache or stored data");

        } catch (Exception e) {
            System.err.println("ChunkNav: Error getting player local coord: " + e.getMessage());
        }
        return null;
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
            if (gui != null && gui.map != null && gui.map.glob != null && gui.map.glob.map != null) {
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
            System.err.println("ChunkNav: Error getting area local coord: " + e.getMessage());
        }
        return null;
    }

    /**
     * A* node for pathfinding.
     */
    private static class AStarNode {
        long gridId;
        AStarNode parent;
        float g; // Cost from start
        float h; // Heuristic to goal
        float f; // Total (g + h)
        Coord crossingPoint;
        ChunkPortal portal;

        AStarNode(long gridId) {
            this.gridId = gridId;
            this.g = Float.MAX_VALUE;
            this.h = 0;
            this.f = Float.MAX_VALUE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AStarNode that = (AStarNode) o;
            return gridId == that.gridId;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(gridId);
        }
    }
}
