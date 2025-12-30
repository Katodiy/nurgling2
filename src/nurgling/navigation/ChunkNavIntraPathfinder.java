package nurgling.navigation;

import haven.Coord;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Pathfinder for navigating within a single chunk using the walkability grid.
 * Uses A* on the coarse 25x25 cell grid to find paths between two points.
 * Also provides cross-chunk path validation.
 */
public class ChunkNavIntraPathfinder {

    /**
     * Result of intra-chunk pathfinding.
     */
    public static class IntraPath {
        public final List<Coord> cellPath;      // Path in coarse cell coordinates (0-24)
        public final List<Coord> localPath;     // Path in local tile coordinates (0-99)
        public final boolean reachable;
        public final float cost;

        public IntraPath(List<Coord> cellPath, boolean reachable, float cost) {
            this.cellPath = cellPath;
            this.reachable = reachable;
            this.cost = cost;

            // Convert cell path to local tile coordinates (center of each cell)
            this.localPath = new ArrayList<>();
            for (Coord cell : cellPath) {
                // Center of the cell in local tile coordinates
                int localX = cell.x * COARSE_CELL_SIZE + COARSE_CELL_SIZE / 2;
                int localY = cell.y * COARSE_CELL_SIZE + COARSE_CELL_SIZE / 2;
                this.localPath.add(new Coord(localX, localY));
            }
        }

        public boolean isEmpty() {
            return cellPath.isEmpty();
        }

        public int size() {
            return cellPath.size();
        }
    }

    /**
     * Find a path between two local tile coordinates within a chunk.
     *
     * @param fromLocal Starting point in local tile coordinates (0-99)
     * @param toLocal   Target point in local tile coordinates (0-99)
     * @param chunk     The chunk data containing walkability grid
     * @return IntraPath with the path if reachable, or empty path if not
     */
    public static IntraPath findPath(Coord fromLocal, Coord toLocal, ChunkNavData chunk) {
        if (chunk == null) {
            return new IntraPath(Collections.emptyList(), false, Float.MAX_VALUE);
        }

        // Convert local tile coords to coarse cell coords
        Coord fromCell = localToCell(fromLocal);
        Coord toCell = localToCell(toLocal);

        // Validate bounds
        if (!isValidCell(fromCell) || !isValidCell(toCell)) {
            return new IntraPath(Collections.emptyList(), false, Float.MAX_VALUE);
        }

        if (chunk.getWalkability(toCell.x, toCell.y) == 2) {
            // Destination is fully blocked
            return new IntraPath(Collections.emptyList(), false, Float.MAX_VALUE);
        }

        // Same cell - already there
        if (fromCell.equals(toCell)) {
            List<Coord> path = new ArrayList<>();
            path.add(fromCell);
            return new IntraPath(path, true, 0);
        }

        // A* pathfinding on the coarse grid
        return aStarPath(fromCell, toCell, chunk);
    }

    /**
     * A* pathfinding on the coarse cell grid.
     */
    private static IntraPath aStarPath(Coord fromCell, Coord toCell, ChunkNavData chunk) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Coord, Node> allNodes = new HashMap<>();
        Set<Coord> closedSet = new HashSet<>();

        Node startNode = new Node(fromCell);
        startNode.g = 0;
        startNode.h = heuristic(fromCell, toCell);
        startNode.f = startNode.g + startNode.h;
        openSet.add(startNode);
        allNodes.put(fromCell, startNode);

        // Direction offsets (4-directional movement)
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        // Also allow diagonal movement
        int[][] diagonals = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        int iterations = 0;
        int maxIterations = CELLS_PER_EDGE * CELLS_PER_EDGE * 4; // Reasonable limit for 100x100 grid

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();

            if (current.pos.equals(toCell)) {
                // Found path - reconstruct it
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            // Expand cardinal directions
            for (int[] dir : directions) {
                expandNeighbor(current, dir[0], dir[1], 1.0f, toCell, chunk, openSet, allNodes, closedSet);
            }

            // Expand diagonals (slightly higher cost, and only if both adjacent cardinals are walkable)
            for (int[] diag : diagonals) {
                // Check if diagonal movement is valid (both adjacent cells must be walkable)
                byte adj1 = chunk.getWalkability(current.pos.x + diag[0], current.pos.y);
                byte adj2 = chunk.getWalkability(current.pos.x, current.pos.y + diag[1]);
                if (adj1 <= 1 && adj2 <= 1) {
                    expandNeighbor(current, diag[0], diag[1], 1.414f, toCell, chunk, openSet, allNodes, closedSet);
                }
            }
        }

        // No path found
        return new IntraPath(Collections.emptyList(), false, Float.MAX_VALUE);
    }

    /**
     * Try to expand to a neighboring cell.
     */
    private static void expandNeighbor(Node current, int dx, int dy, float baseCost,
                                       Coord toCell, ChunkNavData chunk,
                                       PriorityQueue<Node> openSet, Map<Coord, Node> allNodes,
                                       Set<Coord> closedSet) {
        Coord neighborPos = new Coord(current.pos.x + dx, current.pos.y + dy);

        if (!isValidCell(neighborPos)) return;
        if (closedSet.contains(neighborPos)) return;

        byte walkability = chunk.getWalkability(neighborPos.x, neighborPos.y);
        if (walkability != 0) return; // Blocked (tile-level: only 0 is walkable)

        // With tile-level resolution, no partial walkability - uniform cost
        float tentativeG = current.g + baseCost;

        Node neighbor = allNodes.get(neighborPos);
        if (neighbor == null) {
            neighbor = new Node(neighborPos);
            neighbor.g = Float.MAX_VALUE;
            allNodes.put(neighborPos, neighbor);
        }

        if (tentativeG < neighbor.g) {
            neighbor.parent = current;
            neighbor.g = tentativeG;
            neighbor.h = heuristic(neighborPos, toCell);
            neighbor.f = neighbor.g + neighbor.h;

            openSet.remove(neighbor);
            openSet.add(neighbor);
        }
    }

    /**
     * Heuristic function - Euclidean distance.
     */
    private static float heuristic(Coord from, Coord to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Reconstruct path from goal node.
     */
    private static IntraPath reconstructPath(Node goalNode) {
        List<Coord> path = new ArrayList<>();
        Node current = goalNode;
        while (current != null) {
            path.add(0, current.pos);
            current = current.parent;
        }
        return new IntraPath(path, true, goalNode.g);
    }

    /**
     * Convert local tile coordinate to cell coordinate.
     * With tile-level resolution (COARSE_CELL_SIZE=1), this is an identity operation.
     */
    public static Coord localToCell(Coord local) {
        return new Coord(local.x / COARSE_CELL_SIZE, local.y / COARSE_CELL_SIZE);
    }

    /**
     * Check if a cell coordinate is valid.
     */
    private static boolean isValidCell(Coord cell) {
        return cell.x >= 0 && cell.x < CELLS_PER_EDGE && cell.y >= 0 && cell.y < CELLS_PER_EDGE;
    }

    /**
     * A* node for pathfinding.
     */
    private static class Node {
        final Coord pos;
        Node parent;
        float g; // Cost from start
        float h; // Heuristic to goal
        float f; // Total (g + h)

        Node(Coord pos) {
            this.pos = pos;
            this.g = Float.MAX_VALUE;
            this.h = 0;
            this.f = Float.MAX_VALUE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return pos.equals(node.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }
}
