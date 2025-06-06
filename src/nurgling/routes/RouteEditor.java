package nurgling.routes;

import java.util.*;
import haven.Coord;

public class RouteEditor {
    private final RouteGraphManager graphManager;

    /**
     * Constructs a RouteEditor for a given RouteGraphManager.
     * @param graphManager The manager that owns all routes and the global route graph.
     */
    public RouteEditor(RouteGraphManager graphManager) {
        this.graphManager = graphManager;
    }

    /**
     * Replaces the last waypoint in a route with two new points (usually for doors or transitions),
     * migrating all connections and neighbors from the deleted point to the first new point,
     * and then connecting both new points bidirectionally.
     *
     * @param route             The route to operate on.
     * @param firstPointToAdd   The new point that inherits connections/neighbors from the deleted point.
     * @param secondPointToAdd  The new point that is connected to the first.
     * @param hash1             Hash to use for the connection from firstPointToAdd to secondPointToAdd.
     * @param name1             Name to use for the connection from firstPointToAdd to secondPointToAdd.
     * @param hash2             Hash to use for the connection from secondPointToAdd to firstPointToAdd.
     * @param name2             Name to use for the connection from secondPointToAdd to firstPointToAdd.
     */
    public void deleteAndReplaceLastWaypoint(
            Route route,
            RoutePoint firstPointToAdd,
            RoutePoint secondPointToAdd,
            String hash1, String name1, String hash2, String name2
    ) {
        int idx = route.waypoints.size() - 1;
        if(idx < 0) return; // Safety

        RoutePoint pointToDelete = route.waypoints.get(idx);
        Collection<RoutePoint.Connection> deletedConnections = pointToDelete.getConnections();
        List<Integer> deletedNeighbors = pointToDelete.getNeighbors();

        route.deleteWaypoint(pointToDelete);
        cleanupReferencesAfterDelete(pointToDelete.id);

        migrateConnectionsAndNeighbors(deletedConnections, deletedNeighbors, firstPointToAdd);

        route.addPredefinedWaypointNoConnections(firstPointToAdd);
        route.addPredefinedWaypointNoConnections(secondPointToAdd);

        addBidirectionalDoorConnection(
                firstPointToAdd, secondPointToAdd,
                hash1, name1, hash2, name2
        );
    }

    /**
     * Creates a bidirectional connection (door/transition) and neighbor relationship between two route points.
     *
     * @param a         The first point.
     * @param b         The second point.
     * @param aToBHash  Hash string for the connection from 'a' to 'b' (typically a gob hash).
     * @param aToBName  Name for the connection from 'a' to 'b'.
     * @param bToAHash  Hash string for the connection from 'b' to 'a'.
     * @param bToAName  Name for the connection from 'b' to 'a'.
     */
    public void addBidirectionalDoorConnection(
            RoutePoint a,
            RoutePoint b,
            String aToBHash, String aToBName,
            String bToAHash, String bToAName
    ) {
        a.addConnection(b.id, String.valueOf(b.id), aToBHash, aToBName, true);
        b.addConnection(a.id, String.valueOf(a.id), bToAHash, bToAName, true);
        a.addNeighbor(b.id);
        b.addNeighbor(a.id);
    }

    /**
     * Migrates all connections and neighbor relationships from one point (typically deleted)
     * to another point, except for self-links.
     *
     * @param fromConnections   Connections to migrate.
     * @param fromNeighbors     Neighbor IDs to migrate.
     * @param toPoint           The new point that will inherit these connections/neighbors.
     */
    public void migrateConnectionsAndNeighbors(
            Collection<RoutePoint.Connection> fromConnections,
            List<Integer> fromNeighbors,
            RoutePoint toPoint
    ) {
        for (RoutePoint.Connection connection : fromConnections) {
            int connId = Integer.parseInt(connection.connectionTo);
            if (toPoint.id != connId) {
                toPoint.addConnection(connId, connection);
            }
        }
        for (Integer neighbor : fromNeighbors) {
            if (toPoint.id != neighbor) {
                toPoint.addNeighbor(neighbor);
            }
        }
    }

    /**
     * Removes all references (neighbors and connections) to a deleted point from all points in all routes managed by the graph.
     *
     * @param deletedId   The ID of the point that was deleted.
     */
    public void cleanupReferencesAfterDelete(int deletedId) {
        Map<Integer, Route> routes = graphManager.getRoutes();
        for(Route route : routes.values()) {
            for (RoutePoint point : route.waypoints) {
                // Remove from neighbors
                point.getNeighbors().removeIf(n -> n == deletedId);

                // Remove from connections
                if (point.connections != null) {
                    point.connections.remove(deletedId);
                }
            }
        }
    }

    /**
     * Replaces all references to oldId with newId in all points in all routes managed by the graph.
     * Used when a point is merged or its ID changes.
     *
     * @param oldId   The ID to be replaced.
     * @param newId   The ID to replace with.
     */
    public void replaceAllReferences(int oldId, int newId) {
        Map<Integer, Route> routes = graphManager.getRoutes();
        for (Route route : routes.values()) {
            for (RoutePoint point : route.waypoints) {
                // Replace in neighbors
                List<Integer> neighbors = point.getNeighbors();
                for (int i = 0; i < neighbors.size(); i++) {
                    if (neighbors.get(i) == oldId) {
                        neighbors.set(i, newId);
                    }
                }
                // Replace in connections
                if (point.connections != null && point.connections.containsKey(oldId)) {
                    RoutePoint.Connection conn = point.connections.remove(oldId);
                    point.connections.put(newId, conn);
                    conn.connectionTo = String.valueOf(newId); // Update the target
                }
                // Also update connectionTo fields in case they're string-based and point to oldId
                for (RoutePoint.Connection conn : point.connections.values()) {
                    if (conn.connectionTo.equals(String.valueOf(oldId))) {
                        conn.connectionTo = String.valueOf(newId);
                    }
                }
            }
        }
    }

    /**
     * Updates a waypoint's position according to object type.
     * For mineholes, applies an offset to prevent the new point from being unreachable.
     * For other objects, just sets the point to the player coordinate.
     *
     * @param wp           The RoutePoint to update.
     * @param typeName     The object type name (e.g. "gfx/terobjs/minehole").
     * @param gridId       The grid ID where the waypoint is located.
     * @param playerCoord  The coordinate in grid-local space.
     * @param angleOpt     The angle (in radians) for the offset; used for mineholes, can be null for others.
     */
    public void applyWaypointOffset(RoutePoint wp, String typeName, long gridId, Coord playerCoord, Double angleOpt) {
        int offset = 2;
        wp.gridId = gridId;
        if ("gfx/terobjs/minehole".equals(typeName)) {
            double angle = (angleOpt != null) ? angleOpt : 0.0;
            Coord newPosition = new Coord(
                    (int)Math.round(playerCoord.x + Math.cos(angle) * offset),
                    (int)Math.round(playerCoord.y + Math.sin(angle) * offset)
            );
            wp.localCoord = newPosition;
        } else {
            wp.localCoord = playerCoord;
        }
    }
}
