package nurgling.routes;

import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class RouteGraphManager {
    private final RouteGraph graph;
    private final Map<Integer, Route> routes = new HashMap<>();
    private boolean needsUpdate = false;
    private Map<Integer, RoutePoint> routePointMap = new HashMap<>();

    public RouteGraphManager() {
        graph = new RouteGraph();
        loadRoutes();
        updateGraph();
        NConfig.needRoutesUpdate();
    }

    public void updateRoute(Route route) {
        routes.put(route.id, route);
        needsUpdate = true;
    }

    public void updateGraph() {
        if (!needsUpdate) return;

        graph.clear();

        for (Route route : routes.values()) {
            graph.addRoute(route);
        }

        refreshDoors();

        needsUpdate = false;
    }

    public RouteGraph getGraph() {
        if (needsUpdate) {
            updateGraph();
        }
        return graph;
    }

    public void loadRoutes() {
        if (new File(NConfig.current.path_routes).exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_routes), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {
            }

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = (JSONArray) main.get("routes");
                for (int i = 0; i < array.length(); i++) {
                    Route route = new Route((JSONObject) array.get(i), this.routePointMap);
                    routes.put(route.id, route);
                }

                // Update the graph after loading routes
                needsUpdate = true;
                updateGraph();
            }
        }
    }

    public Map<Integer, Route> getRoutes() {
        return routes;
    }

    public RoutePoint getHearthFireForCurrentPlayer() {
        for(RoutePoint routePoint : routePointMap.values()) {
            if(routePoint.hearthFirePlayerName.equals(NUtils.getGameUI().getCharInfo().chrid)) {
                return routePoint;
            }
        }

        return null;
    }

    private void refreshDoors() {
        graph.clearDoors();
        for (Route route : this.routes.values()) {
            for (RoutePoint routePoint : route.waypoints) {
                graph.generateDoors(routePoint);
            }
        }
    }

    public void deleteRoute(Route route) {
        ArrayList<String> doorsInRoute = new ArrayList<>();

        for (RoutePoint routePoint : route.waypoints) {
            for (RoutePoint.Connection connection : routePoint.getConnections()) {
                if (connection.isDoor) {
                    doorsInRoute.add(connection.gobHash);
                }
            }
        }

        for (Route remainingRoute : routes.values()) {
            for (RoutePoint routePoint : remainingRoute.waypoints) {
                for (RoutePoint.Connection connection : routePoint.getConnections()) {
                    // Technically don't have to check contains, but its better performance if you have a lot of connections.
                    if (connection.isDoor && doorsInRoute.contains(connection.gobHash)) {
                        doorsInRoute.remove(connection.gobHash);
                    }
                }
            }
        }

        for (String door : doorsInRoute) {
            graph.deleteDoor(door);
        }
        ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().remove(route.id);
    }

    public void deleteRoutePointFromNeighborsAndConnections(RoutePoint routePoint) {
        for (RoutePoint point : graph.points.values()) {
            if (point.neighbors != null && point.neighbors.contains(routePoint.id)) {
                point.neighbors.remove(Integer.valueOf(routePoint.id));
                point.removeConnection(routePoint.id);
            }
        }
    }
    public void updateConnections(RoutePoint newRoutePoint, int newId) {
        String oldIdStr = String.valueOf(newRoutePoint.id);  // Convert oldId to String once

        for (Route route : routes.values()) {
            for (RoutePoint routePoint : route.waypoints) {
                if(routePoint.id == newId) {
                    mergeConnectionsAndNeighbors(newRoutePoint, routePoint);
                } else if (routePoint.id == newRoutePoint.id) {
                    mergeConnectionsAndNeighbors(newRoutePoint, routePoint);
                }
                // Update neighbors list if oldId is found
                for (int i = 0; i < routePoint.neighbors.size(); i++) {
                    if (routePoint.neighbors.get(i) == newRoutePoint.id) {
                        routePoint.neighbors.set(i, newId); // Replace with newId
                    }
                }

                // Use a list to accumulate keys to be removed or updated
                List<Integer> keysToRemove = new ArrayList<>();
                Map<Integer, RoutePoint.Connection> updatedConnections = new HashMap<>();

                // Iterate through the map and collect modifications
                for (Map.Entry<Integer, RoutePoint.Connection> entry : routePoint.connections.entrySet()) {
                    RoutePoint.Connection connection = entry.getValue();

                    // Check if connectionTo matches oldId (using String comparison)
                    if (connection.connectionTo.equals(oldIdStr)) {
                        // Update the connectionTo with the newId
                        connection.connectionTo = String.valueOf(newId);

                        // Mark the current key for removal and collect the updated connection
                        keysToRemove.add(entry.getKey());
                        updatedConnections.put(newId, connection);
                    }
                }

                // Now remove the old entries
                for (Integer key : keysToRemove) {
                    routePoint.connections.remove(key);
                }

                // Add the new entries with the updated key (newId)
                for (Map.Entry<Integer, RoutePoint.Connection> entry : updatedConnections.entrySet()) {
                    routePoint.connections.put(entry.getKey(), entry.getValue());
                }
            }
        }

        this.needsUpdate = true;
        updateGraph();
        NConfig.needRoutesUpdate();
    }

    private void mergeConnectionsAndNeighbors(RoutePoint a, RoutePoint b) {
        // Merge neighbors (b → a)
        List<Integer> bNeighbors = new ArrayList<>(b.neighbors);
        for (int neighbor : bNeighbors) {
            if (!a.neighbors.contains(neighbor) && neighbor != a.id) {
                a.neighbors.add(neighbor);
            }
        }

        // Merge neighbors (a → b)
        List<Integer> aNeighbors = new ArrayList<>(a.neighbors);
        for (int neighbor : aNeighbors) {
            if (!b.neighbors.contains(neighbor) && neighbor != b.id) {
                b.neighbors.add(neighbor);
            }
        }

        // Merge connections (b → a)
        for (Map.Entry<Integer, RoutePoint.Connection> entry : b.connections.entrySet()) {
            a.connections.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Merge connections (a → b)
        for (Map.Entry<Integer, RoutePoint.Connection> entry : a.connections.entrySet()) {
            b.connections.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Merge reachableAreas
        for (int area : b.getReachableAreas()) {
            a.getReachableAreas().add(area);
        }

        for (int area : a.getReachableAreas()) {
            b.getReachableAreas().add(area);
        }
    }
}
