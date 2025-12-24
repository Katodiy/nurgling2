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
     * Validate that a full ChunkPath is actually walkable, including to the target area.
     * Checks intra-chunk reachability between consecutive waypoints/portals.
     *
     * @param path The chunk path to validate
     * @param graph The navigation graph containing chunk data
     * @param startLocal Player's starting local coordinate in the first chunk
     * @param targetAreaLocal Target area's local coordinate in the final chunk (can be null)
     * @param targetGridId Grid ID where the target area is located
     * @return true if the entire path is walkable, false if blocked
     */
    public static boolean validateFullPathWithTarget(ChunkPath path, ChunkNavGraph graph,
                                                     Coord startLocal, Coord targetAreaLocal,
                                                     long targetGridId) {
        // First validate the waypoint path
        if (!validateFullPath(path, graph, startLocal)) {
            return false;
        }

        // If no target area specified, we're done
        if (targetAreaLocal == null || targetGridId == -1) {
            return true;
        }

        // Get the final position after following the path
        Coord finalLocal = startLocal;
        long finalGridId = -1;

        if (!path.waypoints.isEmpty()) {
            ChunkPath.ChunkWaypoint lastWaypoint = path.waypoints.get(path.waypoints.size() - 1);
            finalLocal = lastWaypoint.localCoord;
            finalGridId = lastWaypoint.gridId;
        }

        // Check if the target is in a different grid than where we end up
        if (finalGridId != targetGridId) {
            // Target is in different grid - can't validate intra-chunk reachability
            return true; // Can't validate, assume passable
        }

        // Validate we can reach the target from final path position
        ChunkNavData finalChunk = graph.getChunk(finalGridId);
        if (finalChunk == null) {
            return true; // No chunk data, assume passable
        }

        if (finalLocal != null) {
            IntraPath toTargetPath = findPath(finalLocal, targetAreaLocal, finalChunk);
            if (!toTargetPath.reachable) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate that a full ChunkPath is actually walkable.
     * Checks intra-chunk reachability between consecutive waypoints/portals.
     *
     * @param path The chunk path to validate
     * @param graph The navigation graph containing chunk data
     * @param startLocal Player's starting local coordinate in the first chunk
     * @return true if the entire path is walkable, false if blocked
     */
    public static boolean validateFullPath(ChunkPath path, ChunkNavGraph graph, Coord startLocal) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        Coord currentLocal = startLocal;
        long currentGridId = -1;

        // Get the first waypoint's grid as starting grid
        if (!path.waypoints.isEmpty()) {
            currentGridId = path.waypoints.get(0).gridId;
        }

        for (int i = 0; i < path.waypoints.size(); i++) {
            ChunkPath.ChunkWaypoint waypoint = path.waypoints.get(i);

            // If we changed grids (via portal), update current position to portal exit
            if (waypoint.gridId != currentGridId) {
                // Portal traversal - assume we land near the portal on the other side
                // The portal's localCoord is where we need to go in the new grid
                currentLocal = waypoint.localCoord;
                currentGridId = waypoint.gridId;
                continue;
            }

            // Same grid - validate we can walk from current position to waypoint
            // EXCEPTION: Skip validation for portal and walk waypoints
            // Portal waypoints: we trust that recorded portals are reachable
            // Walk waypoints (edge crossings): trust chunk-level graph - we may not have walked
            // across the entire chunk, but if chunks are connected, PathFinder will find a way
            if (waypoint.type == ChunkPath.WaypointType.PORTAL_ENTRY ||
                waypoint.type == ChunkPath.WaypointType.WALK) {
                currentLocal = waypoint.localCoord;
                continue;
            }

            // Only validate DESTINATION waypoints (final target)
            ChunkNavData chunk = graph.getChunk(currentGridId);
            if (chunk == null) {
                // No chunk data - can't validate, assume passable
                currentLocal = waypoint.localCoord;
                continue;
            }

            // Check if we can reach this waypoint from current position
            if (currentLocal != null && waypoint.localCoord != null) {
                IntraPath intraPath = findPath(currentLocal, waypoint.localCoord, chunk);
                if (!intraPath.reachable) {
                    return false;
                }
            }

            currentLocal = waypoint.localCoord;
        }

        return true;
    }

    /**
     * Validate reachability from a position to a target area within the same chunk.
     *
     * @param fromLocal Starting local coordinate
     * @param targetLocal Target local coordinate
     * @param chunk The chunk data
     * @return true if reachable, false if blocked
     */
    public static boolean canReachInChunk(Coord fromLocal, Coord targetLocal, ChunkNavData chunk) {
        if (chunk == null || fromLocal == null || targetLocal == null) {
            return true; // No data to validate, assume passable
        }
        IntraPath path = findPath(fromLocal, targetLocal, chunk);
        return path.reachable;
    }

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

        // Check if start or end is blocked
        if (chunk.getWalkability(fromCell.x, fromCell.y) == 2) {
            // Start is fully blocked - but we're standing here, so allow it
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
     * Check if a path exists between two points without returning the full path.
     * More efficient for just checking reachability.
     */
    public static boolean canReach(Coord fromLocal, Coord toLocal, ChunkNavData chunk) {
        IntraPath path = findPath(fromLocal, toLocal, chunk);
        return path.reachable;
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
        float moveCost = baseCost;
        float tentativeG = current.g + moveCost;

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
     * Convert cell coordinate to local tile coordinate.
     * With tile-level resolution (COARSE_CELL_SIZE=1), this is an identity operation.
     */
    public static Coord cellToLocal(Coord cell) {
        return new Coord(
            cell.x * COARSE_CELL_SIZE + COARSE_CELL_SIZE / 2,
            cell.y * COARSE_CELL_SIZE + COARSE_CELL_SIZE / 2
        );
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
