package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;
import haven.MCache;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Unified A* pathfinder that operates on an implicit tile graph spanning all recorded chunks.
 *
 * The graph is implicit - nodes and edges are computed on-demand during A* expansion:
 * - Nodes: Any walkable tile in any recorded chunk
 * - Edges: Adjacent walkable tiles (same chunk), edge crossings (between chunks), portal connections
 *
 * This allows pathfinding through chunks that aren't currently visible, using only stored data.
 */
public class UnifiedTilePathfinder {

    private final ChunkNavGraph graph;

    public UnifiedTilePathfinder(ChunkNavGraph graph) {
        this.graph = graph;
    }

    /**
     * Find a path from a starting tile to a target tile.
     * Both tiles are specified as (chunkGridId, localCoord).
     *
     * @return UnifiedPath containing the complete tile-level path, or null if no path exists
     */
    public UnifiedPath findPath(long startChunkId, Coord startLocal, long targetChunkId, Coord targetLocal) {
        if (startChunkId == targetChunkId && startLocal.equals(targetLocal)) {
            // Already at target
            UnifiedPath path = new UnifiedPath();
            path.reachable = true;
            path.steps.add(new TileNode(startChunkId, startLocal));
            return path;
        }

        ChunkNavData startChunk = graph.getChunk(startChunkId);
        ChunkNavData targetChunk = graph.getChunk(targetChunkId);

        if (startChunk == null || targetChunk == null) {
            return null;
        }

        // A* data structures
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<TileNode, AStarNode> allNodes = new HashMap<>();
        Set<TileNode> closedSet = new HashSet<>();

        // Initialize start node
        TileNode startTile = new TileNode(startChunkId, startLocal);
        TileNode targetTile = new TileNode(targetChunkId, targetLocal);

        AStarNode startNode = new AStarNode(startTile);
        startNode.g = 0;
        startNode.h = heuristic(startTile, targetTile);
        startNode.f = startNode.g + startNode.h;

        openSet.add(startNode);
        allNodes.put(startTile, startNode);

        int iterations = 0;
        int maxIterations = 500000; // Safety limit - increased for large maps

        // Track unique chunks explored for diagnostics
        Set<Long> chunksExplored = new HashSet<>();

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;

            AStarNode current = openSet.poll();
            chunksExplored.add(current.tile.chunkId);

            if (current.tile.equals(targetTile)) {
                // Found path - reconstruct it
                return reconstructPath(current);
            }

            closedSet.add(current.tile);

            // Expand neighbors
            List<TileNode> neighbors = getNeighbors(current.tile);

            for (TileNode neighborTile : neighbors) {
                if (closedSet.contains(neighborTile)) {
                    continue;
                }

                // Calculate cost to neighbor
                double tentativeG = current.g + moveCost(current.tile, neighborTile);

                AStarNode neighborNode = allNodes.get(neighborTile);
                if (neighborNode == null) {
                    neighborNode = new AStarNode(neighborTile);
                    neighborNode.g = Double.MAX_VALUE;
                    allNodes.put(neighborTile, neighborNode);
                }

                if (tentativeG < neighborNode.g) {
                    neighborNode.parent = current;
                    neighborNode.viaPortalFromParent = neighborTile.viaPortal;  // Store how we got here
                    neighborNode.g = tentativeG;
                    neighborNode.h = heuristic(neighborTile, targetTile);
                    neighborNode.f = neighborNode.g + neighborNode.h;

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get all walkable neighbors of a tile.
     * This includes:
     * 1. Adjacent tiles in the same chunk
     * 2. Tiles in adjacent chunks (edge crossings)
     * 3. Portal destinations
     */
    private List<TileNode> getNeighbors(TileNode tile) {
        List<TileNode> neighbors = new ArrayList<>();

        ChunkNavData chunk = graph.getChunk(tile.chunkId);
        if (chunk == null) return neighbors;

        int x = tile.localCoord.x;
        int y = tile.localCoord.y;

        // 8-directional movement within chunk
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE) {
                // Same chunk
                if (chunk.walkability[nx][ny] == 0) {
                    neighbors.add(new TileNode(tile.chunkId, new Coord(nx, ny)));
                }
            } else {
                // Edge crossing - find adjacent chunk
                TileNode crossedTile = getCrossChunkTile(chunk, nx, ny);
                if (crossedTile != null) {
                    neighbors.add(crossedTile);
                }
            }
        }

        // Portal connections
        for (ChunkPortal portal : chunk.portals) {
            // Check if we're at or near the portal
            if (portal.localCoord != null && portal.connectsToGridId != -1) {
                double dist = tile.localCoord.dist(portal.localCoord);
                if (dist <= 2) {
                    ChunkNavData destChunk = graph.getChunk(portal.connectsToGridId);
                    if (destChunk != null) {
                        // Find the exit portal in the destination chunk
                        Coord exitCoord = findPortalExitCoord(destChunk, portal);
                        if (exitCoord != null) {
                            // Mark as portal transition (viaPortal = true)
                            neighbors.add(new TileNode(portal.connectsToGridId, exitCoord, true));
                        }
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * Get a tile in an adjacent chunk when crossing an edge.
     * Uses neighbor relationships (persistent) instead of gridCoord (session-based).
     */
    private TileNode getCrossChunkTile(ChunkNavData fromChunk, int nx, int ny) {
        // Determine which direction we're crossing and find neighbor
        int newX = nx, newY = ny;
        long neighborId = -1;

        if (nx < 0) {
            neighborId = fromChunk.neighborWest;
            newX = CHUNK_SIZE - 1;
        } else if (nx >= CHUNK_SIZE) {
            neighborId = fromChunk.neighborEast;
            newX = 0;
        }

        if (ny < 0) {
            // If also crossing horizontally, this is diagonal - need to handle specially
            if (neighborId != -1) {
                // Diagonal crossing - get the diagonal neighbor through corner
                // For simplicity, prefer horizontal then vertical
                ChunkNavData horzNeighbor = graph.getChunk(neighborId);
                if (horzNeighbor != null) {
                    neighborId = horzNeighbor.neighborNorth;
                }
            } else {
                neighborId = fromChunk.neighborNorth;
            }
            newY = CHUNK_SIZE - 1;
        } else if (ny >= CHUNK_SIZE) {
            if (neighborId != -1) {
                ChunkNavData horzNeighbor = graph.getChunk(neighborId);
                if (horzNeighbor != null) {
                    neighborId = horzNeighbor.neighborSouth;
                }
            } else {
                neighborId = fromChunk.neighborSouth;
            }
            newY = 0;
        }

        if (neighborId == -1) return null;

        ChunkNavData neighborChunk = graph.getChunk(neighborId);
        if (neighborChunk == null) return null;

        // Must be same layer
        if (!fromChunk.layer.equals(neighborChunk.layer)) return null;

        // Check if target tile is walkable
        if (neighborChunk.walkability[newX][newY] == 0) {
            return new TileNode(neighborChunk.gridId, new Coord(newX, newY));
        }

        return null;
    }

    /**
     * Find the exit coordinate when arriving through a portal.
     * Returns a walkable tile near the matching exit portal.
     *
     * @param destChunk The destination chunk we're entering
     * @param entryPortal The portal we're using to enter (contains source chunk's gridId)
     */
    private Coord findPortalExitCoord(ChunkNavData destChunk, ChunkPortal entryPortal) {
        // The entry portal is in the SOURCE chunk and connects to destChunk
        // We need to find the matching exit portal in destChunk that connects back to source
        long sourceChunkId = entryPortal.connectsToGridId;
        // Actually, entryPortal.connectsToGridId points to destChunk, not source
        // We need to find a portal in destChunk that connects back to where entryPortal came from

        // Find the chunk that contains entryPortal
        // entryPortal is from the current expansion tile's chunk
        // We want to find a portal in destChunk that connects back

        Coord bestExitCoord = null;
        Coord matchingExitCoord = null;

        for (ChunkPortal portal : destChunk.portals) {
            if (portal.localCoord == null) continue;

            // Check if this portal connects back to where we came from
            // The entry portal came from a chunk, and this portal should connect back there
            if (portal.connectsToGridId != -1) {
                // This portal has a connection - it could be our exit point
                // If it connects to the source chunk (where entryPortal is), it's the matching exit
                // Since entryPortal.connectsToGridId == destChunk.gridId,
                // we need portal.connectsToGridId to NOT be -1 and ideally match a related portal type

                // For door pairs: stonemansion -> stonemansion-door
                // For cellar pairs: cellardoor -> cellarstairs
                if (isMatchingPortalPair(entryPortal.gobName, portal.gobName)) {
                    Coord walkable = findWalkableTileNear(destChunk, portal.localCoord);
                    if (walkable != null) {
                        return walkable;
                    }
                    matchingExitCoord = portal.localCoord;
                }

                // Remember any portal with connection as fallback
                if (bestExitCoord == null) {
                    bestExitCoord = portal.localCoord;
                }
            }
        }

        // Use matching exit coord first, then best, then fallback
        if (matchingExitCoord != null) {
            Coord walkable = findWalkableTileNear(destChunk, matchingExitCoord);
            return walkable != null ? walkable : matchingExitCoord;
        }

        if (bestExitCoord != null) {
            Coord walkable = findWalkableTileNear(destChunk, bestExitCoord);
            if (walkable != null) {
                return walkable;
            }
            return bestExitCoord;
        }

        // Fallback: center of chunk
        return new Coord(CHUNK_SIZE / 2, CHUNK_SIZE / 2);
    }

    /**
     * Check if two portal types are a matching pair (entry/exit).
     */
    private boolean isMatchingPortalPair(String entryGobName, String exitGobName) {
        if (entryGobName == null || exitGobName == null) return false;

        // Normalize names
        String entry = entryGobName.toLowerCase();
        String exit = exitGobName.toLowerCase();

        // Door pairs
        if ((entry.contains("stonemansion") && !entry.contains("-door")) &&
            exit.contains("stonemansion-door")) return true;
        if (entry.contains("stonemansion-door") &&
            (exit.contains("stonemansion") && !exit.contains("-door"))) return true;

        // Cellar pairs
        if (entry.contains("cellardoor") && exit.contains("cellarstairs")) return true;
        if (entry.contains("cellarstairs") && exit.contains("cellardoor")) return true;

        // Generic door pairs
        if (entry.contains("door") && exit.contains("door")) return true;

        // Same type (some portals are bidirectional with same gob)
        if (entry.equals(exit)) return true;

        return false;
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
        int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int radius = 1; radius <= 3; radius++) {
            for (int[] dir : dirs) {
                int nx = target.x + dir[0] * radius;
                int ny = target.y + dir[1] * radius;
                if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE) {
                    if (chunk.walkability[nx][ny] == 0) {
                        return new Coord(nx, ny);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Heuristic function for A* - estimates distance between two tiles.
     * Uses chunk-aware distance calculation.
     *
     * IMPORTANT: gridCoord and worldTileOrigin are session-based and may be null
     * for chunks loaded from save files. Use neighbor-based estimation as fallback.
     */
    private double heuristic(TileNode from, TileNode to) {
        if (from.chunkId == to.chunkId) {
            // Same chunk - simple Euclidean distance
            return from.localCoord.dist(to.localCoord);
        }

        // Different chunks
        ChunkNavData fromChunk = graph.getChunk(from.chunkId);
        ChunkNavData toChunk = graph.getChunk(to.chunkId);

        if (fromChunk == null || toChunk == null) {
            return 1000.0; // Unknown chunk
        }

        // Check if same layer - if not, add portal traversal cost estimate
        if (!fromChunk.layer.equals(toChunk.layer)) {
            int depth = getPortalPathDepth(fromChunk, toChunk, new HashSet<>());
            if (depth == -1) {
                return 999999.0; // Dead end - this building doesn't connect to target
            }
            return 100.0 + (depth - 1) * 400.0;
        }

        // Try world coordinates if both chunks have been seen this session
        if (fromChunk.worldTileOrigin != null && toChunk.worldTileOrigin != null) {
            Coord fromWorld = fromChunk.worldTileOrigin.add(from.localCoord);
            Coord toWorld = toChunk.worldTileOrigin.add(to.localCoord);
            return fromWorld.dist(toWorld);
        }

        // Fallback: estimate based on neighbor-based distance
        // BFS to find shortest path through neighbor relationships
        int neighborDist = getNeighborDistance(from.chunkId, to.chunkId);
        if (neighborDist >= 0) {
            // Each chunk is 100 tiles, estimate walking across half of each
            return neighborDist * CHUNK_SIZE + from.localCoord.dist(new Coord(50, 50)) + to.localCoord.dist(new Coord(50, 50));
        }

        // No path through neighbors - might need portal
        // Return large but not infinite estimate
        return 5000.0;
    }

    /**
     * Get shortest path distance between chunks using neighbor relationships.
     * Returns number of chunks to traverse, or -1 if not connected through neighbors.
     */
    private int getNeighborDistance(long fromChunkId, long toChunkId) {
        if (fromChunkId == toChunkId) return 0;

        // BFS through neighbor relationships
        Map<Long, Integer> visited = new HashMap<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(fromChunkId);
        visited.put(fromChunkId, 0);

        while (!queue.isEmpty()) {
            long current = queue.poll();
            int dist = visited.get(current);

            // Limit search depth to avoid expensive searches
            if (dist > 20) continue;

            ChunkNavData chunk = graph.getChunk(current);
            if (chunk == null) continue;

            long[] neighbors = {chunk.neighborNorth, chunk.neighborSouth, chunk.neighborEast, chunk.neighborWest};
            for (long neighbor : neighbors) {
                if (neighbor == -1) continue;
                if (neighbor == toChunkId) return dist + 1;
                if (!visited.containsKey(neighbor)) {
                    visited.put(neighbor, dist + 1);
                    queue.add(neighbor);
                }
            }
        }

        return -1; // Not connected through neighbors
    }

    /**
     * Get the minimum number of portal hops needed to reach toChunk from fromChunk.
     * Returns -1 if unreachable, 1 for direct connection, 2+ for paths through other chunks.
     * Used to heavily penalize paths that go through unnecessary buildings.
     */
    private int getPortalPathDepth(ChunkNavData fromChunk, ChunkNavData toChunk, Set<Long> visited) {
        if (fromChunk.gridId == toChunk.gridId) {
            return 0;
        }

        if (visited.contains(fromChunk.gridId)) {
            return -1; // Already visited, avoid cycles
        }
        visited.add(fromChunk.gridId);

        int minDepth = -1;

        // Check all portals from this chunk
        for (ChunkPortal portal : fromChunk.portals) {
            if (portal.connectsToGridId == -1) continue;

            if (portal.connectsToGridId == toChunk.gridId) {
                return 1; // Direct connection - best case
            }

            // Recursively check connected chunks (limit depth to avoid long searches)
            if (visited.size() < 10) {
                ChunkNavData nextChunk = graph.getChunk(portal.connectsToGridId);
                if (nextChunk != null) {
                    int subDepth = getPortalPathDepth(nextChunk, toChunk, new HashSet<>(visited));
                    if (subDepth != -1) {
                        int totalDepth = 1 + subDepth;
                        if (minDepth == -1 || totalDepth < minDepth) {
                            minDepth = totalDepth;
                        }
                    }
                }
            }
        }

        return minDepth;
    }

    /**
     * Calculate movement cost between two tiles.
     */
    private double moveCost(TileNode from, TileNode to) {
        if (from.chunkId == to.chunkId) {
            // Same chunk - distance-based cost
            double dist = from.localCoord.dist(to.localCoord);
            return dist;
        }

        // Cross-chunk movement
        ChunkNavData fromChunk = graph.getChunk(from.chunkId);
        ChunkNavData toChunk = graph.getChunk(to.chunkId);

        if (fromChunk != null && toChunk != null) {
            if (!fromChunk.layer.equals(toChunk.layer)) {
                // Portal traversal - fixed cost
                return 10.0;
            }
        }

        // Edge crossing - small cost
        return 1.5;
    }

    /**
     * Reconstruct the path from the goal node back to the start.
     */
    private UnifiedPath reconstructPath(AStarNode goalNode) {
        UnifiedPath path = new UnifiedPath();
        path.reachable = true;

        List<TileNode> reversePath = new ArrayList<>();
        AStarNode current = goalNode;

        while (current != null) {
            // Create TileNode with correct viaPortal based on how we got to this node
            TileNode tileWithPortalInfo = new TileNode(
                current.tile.chunkId,
                current.tile.localCoord,
                current.viaPortalFromParent
            );
            reversePath.add(tileWithPortalInfo);
            current = current.parent;
        }

        // Reverse to get start-to-goal order
        Collections.reverse(reversePath);
        path.steps.addAll(reversePath);
        path.cost = goalNode.g;

        return path;
    }

    /**
     * A tile in the unified graph.
     */
    public static class TileNode {
        public final long chunkId;
        public final Coord localCoord;
        public final boolean viaPortal;  // True if reached via portal (door/gate), false for edge crossing

        public TileNode(long chunkId, Coord localCoord) {
            this(chunkId, localCoord, false);
        }

        public TileNode(long chunkId, Coord localCoord, boolean viaPortal) {
            this.chunkId = chunkId;
            this.localCoord = localCoord;
            this.viaPortal = viaPortal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TileNode tileNode = (TileNode) o;
            return chunkId == tileNode.chunkId && localCoord.equals(tileNode.localCoord);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkId, localCoord);
        }

        @Override
        public String toString() {
            return "Tile[chunk=" + chunkId + ", local=" + localCoord + "]";
        }
    }

    /**
     * A* node wrapper.
     */
    private static class AStarNode {
        TileNode tile;
        AStarNode parent;
        boolean viaPortalFromParent;  // True if this node was reached via portal from parent
        double g; // Cost from start
        double h; // Heuristic to goal
        double f; // Total (g + h)

        AStarNode(TileNode tile) {
            this.tile = tile;
            this.g = Double.MAX_VALUE;
            this.h = 0;
            this.f = Double.MAX_VALUE;
            this.viaPortalFromParent = false;
        }
    }

    /**
     * Result of unified pathfinding.
     */
    public static class UnifiedPath {
        public boolean reachable = false;
        public List<TileNode> steps = new ArrayList<>();
        public double cost = 0;

        public boolean isEmpty() {
            return steps.isEmpty();
        }

        public int size() {
            return steps.size();
        }

        /**
         * Convert this path to ChunkPath segments for the executor.
         */
        public void populateChunkPath(ChunkPath chunkPath, ChunkNavGraph graph) {
            if (steps.isEmpty()) return;

            long currentChunkId = steps.get(0).chunkId;
            ChunkNavData currentChunk = graph.getChunk(currentChunkId);
            ChunkPath.PathSegment currentSegment = null;

            if (currentChunk != null) {
                currentSegment = new ChunkPath.PathSegment(currentChunkId, currentChunk.worldTileOrigin);
            }

            for (TileNode step : steps) {
                if (step.chunkId != currentChunkId) {
                    // Chunk changed - finish current segment and start new one
                    if (currentSegment != null && !currentSegment.isEmpty()) {
                        // Only mark as PORTAL if this step was reached via an actual portal (door/gate/mine)
                        // Edge crossings between neighbor chunks are just WALK
                        currentSegment.type = step.viaPortal ? ChunkPath.SegmentType.PORTAL : ChunkPath.SegmentType.WALK;
                        chunkPath.segments.add(currentSegment);
                    }

                    currentChunkId = step.chunkId;
                    currentChunk = graph.getChunk(currentChunkId);
                    if (currentChunk != null) {
                        currentSegment = new ChunkPath.PathSegment(currentChunkId, currentChunk.worldTileOrigin);
                    } else {
                        currentSegment = null;
                    }
                }

                if (currentSegment != null && currentChunk != null) {
                    currentSegment.steps.add(new ChunkPath.TileStep(step.localCoord, currentChunk.worldTileOrigin));
                }
            }

            // Add final segment
            if (currentSegment != null && !currentSegment.isEmpty()) {
                currentSegment.type = ChunkPath.SegmentType.WALK;
                chunkPath.segments.add(currentSegment);
            }

            chunkPath.totalCost = (float) cost;
        }
    }
}
