package nurgling.navigation;

import haven.Coord;
import nurgling.tasks.GateDetector;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Unified A* pathfinder that operates on an implicit tile graph spanning all recorded chunks.
 * The graph is implicit - nodes and edges are computed on-demand during A* expansion:
 * - Nodes: Any walkable tile in any recorded chunk
 * - Edges: Adjacent walkable tiles (same chunk), edge crossings (between chunks), portal connections
 * This allows pathfinding through chunks that aren't currently visible, using only stored data.
 */
public class UnifiedTilePathfinder {

    private final ChunkNavGraph graph;

    public UnifiedTilePathfinder(ChunkNavGraph graph) {
        this.graph = graph;
    }

    /**
     * Check if a tile (in 0-99 space) is walkable by checking if ANY of its 2x2 cells is walkable.
     */
    private boolean isTileWalkable(ChunkNavData chunk, int tileX, int tileY) {
        int cellX = tileX * CELLS_PER_TILE;
        int cellY = tileY * CELLS_PER_TILE;
        for (int dx = 0; dx < CELLS_PER_TILE; dx++) {
            for (int dy = 0; dy < CELLS_PER_TILE; dy++) {
                int cx = cellX + dx;
                int cy = cellY + dy;
                if (cx >= 0 && cx < CELLS_PER_EDGE && cy >= 0 && cy < CELLS_PER_EDGE) {
                    if (chunk.walkability[cx][cy] == 0) {
                        return true;  // At least one sub-cell is walkable
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find a walkable cell within a tile and return its world coordinate.
     * This targets the center of a specific walkable cell rather than the tile center,
     * which prevents pathfinding to blocked areas when only part of a tile is walkable.
     *
     * The method picks the walkable cell that is furthest from any blocked cells
     * to avoid edge cases where rounding puts the coordinate in blocked territory.
     *
     * @param chunk The chunk data
     * @param tileX Tile X coordinate (0-99)
     * @param tileY Tile Y coordinate (0-99)
     * @param worldTileOrigin The chunk's world tile origin
     * @return World coordinate of a walkable cell's center, or tile center if no cell info
     */
    public static haven.Coord2d findWalkableCellWorldCoord(ChunkNavData chunk, int tileX, int tileY, haven.Coord worldTileOrigin) {
        if (chunk == null || worldTileOrigin == null) {
            // Fallback to tile center
            haven.Coord worldTile = worldTileOrigin != null ? worldTileOrigin.add(tileX, tileY) : new haven.Coord(tileX, tileY);
            return worldTile.mul(haven.MCache.tilesz).add(haven.MCache.tilehsz);
        }

        int cellX = tileX * CELLS_PER_TILE;
        int cellY = tileY * CELLS_PER_TILE;

        // Find the walkable cell with the most distance from blocked cells
        // This avoids edge cases where the target is right at a blocked boundary
        int bestCx = -1, bestCy = -1;
        int bestScore = -1;

        int[][] cellOffsets = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};

        for (int[] offset : cellOffsets) {
            int cx = cellX + offset[0];
            int cy = cellY + offset[1];
            if (cx >= 0 && cx < CELLS_PER_EDGE && cy >= 0 && cy < CELLS_PER_EDGE) {
                if (chunk.walkability[cx][cy] == 0) {
                    // Count how many of the 8 neighbors are also walkable (higher = safer)
                    int score = 0;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = cx + dx;
                            int ny = cy + dy;
                            if (nx >= 0 && nx < CELLS_PER_EDGE && ny >= 0 && ny < CELLS_PER_EDGE) {
                                if (chunk.walkability[nx][ny] == 0) {
                                    score++;
                                }
                            }
                        }
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        bestCx = cx;
                        bestCy = cy;
                    }
                }
            }
        }

        if (bestCx >= 0 && bestCy >= 0) {
            // Found a walkable cell - compute its world coordinate (center of the subcell)
            double worldX = worldTileOrigin.x * haven.MCache.tilesz.x + bestCx * haven.MCache.tilehsz.x + haven.MCache.tilehsz.x / 2;
            double worldY = worldTileOrigin.y * haven.MCache.tilesz.y + bestCy * haven.MCache.tilehsz.y + haven.MCache.tilehsz.y / 2;
            return new haven.Coord2d(worldX, worldY);
        }

        // No walkable cell found - fallback to tile center
        haven.Coord worldTile = worldTileOrigin.add(tileX, tileY);
        return worldTile.mul(haven.MCache.tilesz).add(haven.MCache.tilehsz);
    }

    /**
     * Find a path from a starting tile to a target tile.
     * Both tiles are specified as (chunkGridId, localCoord).
     *
     * @return UnifiedPath containing the complete tile-level path, or null if no path exists
     */
    public UnifiedPath findPath(long startChunkId, Coord startLocal, long targetChunkId, Coord targetLocal) {
        // System.out.println("[UnifiedTilePathfinder] findPath called:");
        // System.out.println("  - Start: chunk " + startChunkId + " local " + startLocal);
        // System.out.println("  - Target: chunk " + targetChunkId + " local " + targetLocal);

        if (startChunkId == targetChunkId && startLocal.equals(targetLocal)) {
            // Already at target
            // System.out.println("[UnifiedTilePathfinder] Already at target!");
            UnifiedPath path = new UnifiedPath();
            path.reachable = true;
            path.steps.add(new TileNode(startChunkId, startLocal));
            return path;
        }

        ChunkNavData startChunk = graph.getChunk(startChunkId);
        ChunkNavData targetChunk = graph.getChunk(targetChunkId);

        if (startChunk == null) {
            // System.out.println("[UnifiedTilePathfinder] ERROR: Start chunk " + startChunkId + " not in graph!");
            return null;
        }
        if (targetChunk == null) {
            // System.out.println("[UnifiedTilePathfinder] ERROR: Target chunk " + targetChunkId + " not in graph!");
            return null;
        }

        // System.out.println("[UnifiedTilePathfinder] Start chunk info:");
        // System.out.println("  - Layer: " + startChunk.layer);
        // System.out.println("  - Neighbors: N=" + startChunk.neighborNorth + " S=" + startChunk.neighborSouth +
        //                   " E=" + startChunk.neighborEast + " W=" + startChunk.neighborWest);
        // System.out.println("  - Portals: " + startChunk.portals.size());

        // System.out.println("[UnifiedTilePathfinder] Target chunk info:");
        // System.out.println("  - Layer: " + targetChunk.layer);
        // System.out.println("  - Neighbors: N=" + targetChunk.neighborNorth + " S=" + targetChunk.neighborSouth +
        //                   " E=" + targetChunk.neighborEast + " W=" + targetChunk.neighborWest);
        // System.out.println("  - Portals: " + targetChunk.portals.size());

        // Check if same layer
        // if (!startChunk.layer.equals(targetChunk.layer)) {
        //     System.out.println("[UnifiedTilePathfinder] DIFFERENT LAYERS: " + startChunk.layer + " -> " + targetChunk.layer);
        // }

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

        // System.out.println("[UnifiedTilePathfinder] Starting A* search...");
        // long searchStartTime = System.currentTimeMillis();

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;

            AStarNode current = openSet.poll();
            chunksExplored.add(current.tile.chunkId);

            if (current.tile.equals(targetTile)) {
                // Found path - reconstruct it
                // System.out.println("[UnifiedTilePathfinder] PATH FOUND! Iterations: " + iterations + ", chunks: " + chunksExplored.size());
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

        // Path not found
        // System.out.println("[UnifiedTilePathfinder] NO PATH FOUND! Iterations: " + iterations + ", chunks explored: " + chunksExplored.size());
        // if (openSet.isEmpty()) {
        //     System.out.println("[UnifiedTilePathfinder] Search exhausted - no connection to target");
        // }

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
                // Same chunk - check tile walkability (any of 2x2 cells walkable)
                if (isTileWalkable(chunk, nx, ny)) {
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
                        // Pass source chunk ID so we can verify the exit portal connects back to us
                        Coord exitCoord = findPortalExitCoord(destChunk, portal, tile.chunkId);
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
     * All coordinates are in tile space (0-99).
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

        // Check if target tile is walkable (any of 2x2 cells)
        if (isTileWalkable(neighborChunk, newX, newY)) {
            return new TileNode(neighborChunk.gridId, new Coord(newX, newY));
        }

        return null;
    }

    /**
     * Find the exit coordinate when arriving through a portal.
     * Returns a walkable tile near the matching exit portal.
     *
     * @param destChunk The destination chunk we're entering
     * @param entryPortal The portal we're using to enter
     * @param sourceChunkId The chunk we're coming FROM (to verify reverse connection)
     */
    private Coord findPortalExitCoord(ChunkNavData destChunk, ChunkPortal entryPortal, long sourceChunkId) {
        // BEST: Use stored exit coordinate if available (recorded during traversal)
        // This handles all cases including multiple identical portals (e.g., 2 mineholes to same mine)
        if (entryPortal.exitLocalCoord != null) {
            Coord walkable = findWalkableTileNear(destChunk, entryPortal.exitLocalCoord);
            return walkable != null ? walkable : entryPortal.exitLocalCoord;
        }

        // FALLBACK: Search for matching portal in destination chunk
        // Requires: matching portal type AND connects back to our source chunk
        Coord bestExitCoord = null;

        for (ChunkPortal portal : destChunk.portals) {
            if (portal.localCoord == null) continue;

            // Must connect back to the chunk we came from
            if (portal.connectsToGridId != sourceChunkId) continue;

            // Must be a matching portal pair (e.g., stonestead-door <-> stonestead)
            if (isMatchingPortalPair(entryPortal.gobName, portal.gobName)) {
                Coord walkable = findWalkableTileNear(destChunk, portal.localCoord);
                if (walkable != null) {
                    return walkable;
                }
                // Even if no walkable nearby, this is our exit
                return portal.localCoord;
            }

            // Same gob type is also valid (bidirectional portals like ladders)
            if (entryPortal.gobName != null && entryPortal.gobName.equals(portal.gobName)) {
                bestExitCoord = portal.localCoord;
            }
        }

        // Use best match if found
        if (bestExitCoord != null) {
            Coord walkable = findWalkableTileNear(destChunk, bestExitCoord);
            return walkable != null ? walkable : bestExitCoord;
        }

        // Fallback: center of chunk (shouldn't happen if data is recorded correctly)
        return new Coord(CHUNK_SIZE / 2, CHUNK_SIZE / 2);
    }

    /**
     * Check if two portal types are a matching pair (entry/exit).
     * Uses GateDetector.getDoorPair() for consistent door pair matching.
     */
    private boolean isMatchingPortalPair(String entryGobName, String exitGobName) {
        if (entryGobName == null || exitGobName == null) return false;

        // Use GateDetector's door pair lookup (same as routes system)
        String expectedPair = GateDetector.getDoorPair(entryGobName);
        if (expectedPair != null && expectedPair.equals(exitGobName)) {
            return true;
        }

        // Same type (some portals are bidirectional with same gob)
        return entryGobName.equals(exitGobName);
    }

    /**
     * Find a walkable tile near the given coordinate.
     * Coordinates are in tile space (0-99).
     */
    private Coord findWalkableTileNear(ChunkNavData chunk, Coord target) {
        // Check the target itself first
        if (target.x >= 0 && target.x < CHUNK_SIZE &&
            target.y >= 0 && target.y < CHUNK_SIZE &&
            isTileWalkable(chunk, target.x, target.y)) {
            return target;
        }

        // Search in expanding rings around the target
        int[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int radius = 1; radius <= 3; radius++) {
            for (int[] dir : dirs) {
                int nx = target.x + dir[0] * radius;
                int ny = target.y + dir[1] * radius;
                if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE) {
                    if (isTileWalkable(chunk, nx, ny)) {
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
            // Each chunk is CHUNK_SIZE tiles, estimate walking across half of each
            Coord chunkCenter = new Coord(CHUNK_SIZE / 2, CHUNK_SIZE / 2);
            return neighborDist * CHUNK_SIZE + from.localCoord.dist(chunkCenter) + to.localCoord.dist(chunkCenter);
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
            return from.localCoord.dist(to.localCoord);
        }

        // Cross-chunk movement
        ChunkNavData fromChunk = graph.getChunk(from.chunkId);
        ChunkNavData toChunk = graph.getChunk(to.chunkId);

        if (fromChunk != null && toChunk != null) {
            if (!fromChunk.layer.equals(toChunk.layer)) {
                // Portal traversal - use config cost
                return ChunkNavConfig.PORTAL_TRAVERSAL_COST;
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

                if (currentSegment != null) {
                    // Use cell-level coordinate to target a walkable cell within the tile
                    // This prevents targeting blocked parts of partially-walkable tiles
                    haven.Coord2d cellWorldCoord = findWalkableCellWorldCoord(
                        currentChunk, step.localCoord.x, step.localCoord.y, currentChunk.worldTileOrigin);
                    currentSegment.steps.add(new ChunkPath.TileStep(step.localCoord, cellWorldCoord));
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
