package nurgling.navigation;

import java.util.*;

/**
 * Generic A* pathfinder that can be parameterized for different node types.
 *
 * Used by:
 * - ChunkNavIntraPathfinder (Coord nodes within a single chunk)
 * - UnifiedTilePathfinder (TileNode nodes across all chunks)
 * - ChunkNavPlanner (Long gridId nodes at chunk level)
 *
 * @param <N> The node type (must have proper equals/hashCode)
 */
public class AStarPathfinder<N> {

    /**
     * Interface for providing graph structure and costs to A*.
     */
    public interface NodeProvider<N> {
        /**
         * Get all neighbor nodes from the given node.
         */
        List<N> getNeighbors(N node);

        /**
         * Calculate the movement cost from one node to an adjacent node.
         */
        double moveCost(N from, N to);

        /**
         * Estimate the cost from a node to the goal (heuristic).
         * Must be admissible (never overestimate) for optimal paths.
         */
        double heuristic(N from, N goal);

        /**
         * Check if a node is the goal.
         * Default implementation uses equals().
         */
        default boolean isGoal(N node, N goal) {
            return node.equals(goal);
        }
    }

    /**
     * Result of A* pathfinding.
     */
    public static class PathResult<N> {
        public final List<N> path;
        public final double cost;
        public final boolean found;
        public final int iterations;

        private PathResult(List<N> path, double cost, boolean found, int iterations) {
            this.path = path;
            this.cost = cost;
            this.found = found;
            this.iterations = iterations;
        }

        public static <N> PathResult<N> success(List<N> path, double cost, int iterations) {
            return new PathResult<>(path, cost, true, iterations);
        }

        public static <N> PathResult<N> failure(int iterations) {
            return new PathResult<>(Collections.emptyList(), Double.MAX_VALUE, false, iterations);
        }

        public boolean isEmpty() {
            return path.isEmpty();
        }
    }

    /**
     * Internal A* node wrapper.
     */
    private static class AStarNode<N> {
        final N node;
        AStarNode<N> parent;
        double g; // Cost from start
        double h; // Heuristic to goal
        double f; // Total (g + h)

        AStarNode(N node) {
            this.node = node;
            this.g = Double.MAX_VALUE;
            this.h = 0;
            this.f = Double.MAX_VALUE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AStarNode<?> that = (AStarNode<?>) o;
            return node.equals(that.node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }
    }

    private final int maxIterations;

    /**
     * Create a pathfinder with default max iterations (100,000).
     */
    public AStarPathfinder() {
        this(100000);
    }

    /**
     * Create a pathfinder with specified max iterations.
     */
    public AStarPathfinder(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Find a path from start to goal using the provided node provider.
     *
     * @param start The starting node
     * @param goal The goal node
     * @param provider Provides neighbors, costs, and heuristics
     * @return PathResult containing the path if found, or empty path if not reachable
     */
    public PathResult<N> findPath(N start, N goal, NodeProvider<N> provider) {
        if (start.equals(goal)) {
            List<N> singlePath = new ArrayList<>();
            singlePath.add(start);
            return PathResult.success(singlePath, 0, 0);
        }

        PriorityQueue<AStarNode<N>> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.f)
        );
        Map<N, AStarNode<N>> allNodes = new HashMap<>();
        Set<N> closedSet = new HashSet<>();

        // Initialize start node
        AStarNode<N> startNode = new AStarNode<>(start);
        startNode.g = 0;
        startNode.h = provider.heuristic(start, goal);
        startNode.f = startNode.g + startNode.h;

        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;

            AStarNode<N> current = openSet.poll();

            if (provider.isGoal(current.node, goal)) {
                // Found path - reconstruct it
                return PathResult.success(reconstructPath(current), current.g, iterations);
            }

            closedSet.add(current.node);

            // Expand neighbors
            List<N> neighbors = provider.getNeighbors(current.node);

            for (N neighborNode : neighbors) {
                if (closedSet.contains(neighborNode)) {
                    continue;
                }

                double tentativeG = current.g + provider.moveCost(current.node, neighborNode);

                AStarNode<N> neighbor = allNodes.get(neighborNode);
                if (neighbor == null) {
                    neighbor = new AStarNode<>(neighborNode);
                    allNodes.put(neighborNode, neighbor);
                }

                if (tentativeG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = tentativeG;
                    neighbor.h = provider.heuristic(neighborNode, goal);
                    neighbor.f = neighbor.g + neighbor.h;

                    // Update position in priority queue
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        // No path found
        return PathResult.failure(iterations);
    }

    /**
     * Reconstruct the path from goal node back to start.
     */
    private List<N> reconstructPath(AStarNode<N> goalNode) {
        List<N> path = new ArrayList<>();
        AStarNode<N> current = goalNode;

        while (current != null) {
            path.add(0, current.node);
            current = current.parent;
        }

        return path;
    }
}
