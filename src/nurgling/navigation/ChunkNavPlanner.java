package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;
import haven.MCache;
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

    public ChunkNavPlanner(ChunkNavGraph graph) {
        this.graph = graph;
    }

    /**
     * Plan a path from player's current position to a target area.
     */
    public ChunkPath planToArea(NArea area) {
        if (area == null) return null;

        // Get player's current chunk
        long startChunkId = graph.getPlayerChunkId();
        if (startChunkId == -1) {
            System.out.println("ChunkNav: Cannot get player's current chunk");
            return null;
        }

        System.out.println("ChunkNav: Planning path to area '" + area.name + "' from chunk " + startChunkId);

        // Find chunks that can reach the target area
        Set<Long> targetChunks = graph.getChunksForArea(area.id);
        System.out.println("ChunkNav: getChunksForArea(" + area.id + ") returned " + targetChunks.size() + " chunks: " + targetChunks);

        if (targetChunks.isEmpty()) {
            // Try to find chunks near the area's coordinates
            targetChunks = findChunksNearArea(area);
            System.out.println("ChunkNav: findChunksNearArea returned " + targetChunks.size() + " chunks: " + targetChunks);
        }

        if (targetChunks.isEmpty()) {
            // Debug: show why we couldn't find chunks
            StringBuilder debug = new StringBuilder();
            debug.append("ChunkNav: No chunks for area '").append(area.name).append("' (id=").append(area.id).append(")");
            if (area.space != null && area.space.space != null) {
                debug.append(" area.gridIds=").append(area.space.space.keySet());
            }
            debug.append(" recorded chunks=").append(graph.getChunkCount());
            System.out.println(debug.toString());
            return null;
        }

        ChunkPath path = planPath(startChunkId, targetChunks, area);
        if (path != null) {
            System.out.println("ChunkNav: Found path with " + path.waypoints.size() + " waypoints, cost=" + path.totalCost);
            for (int i = 0; i < path.waypoints.size(); i++) {
                ChunkPath.ChunkWaypoint wp = path.waypoints.get(i);
                System.out.println("  [" + i + "] grid=" + wp.gridId + " local=" + wp.localCoord + " type=" + wp.type);
            }
        } else {
            System.out.println("ChunkNav: A* returned no path from " + startChunkId + " to " + targetChunks);
        }
        return path;
    }

    /**
     * Plan a path from a start chunk to any of the target chunks.
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
