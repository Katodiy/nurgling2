package nurgling.routes;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NCore;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class Route {
    public int id;
    public String name;
    public String path = "";
    public final ArrayList<RoutePoint> waypoints = new ArrayList<>();
    public ArrayList<RouteSpecialization> spec = new ArrayList<>();
    public NCore.LastActions lastAction = null;
    public RoutePoint cachedRoutePoint = null;
    public boolean hasPassedGate = false;
    public Gob lastPassedGate = null;


    public static class RouteSpecialization {
        public String name;
        public String subtype = null;

        public RouteSpecialization(String name, String subtype) {
            this.name = name;
            this.subtype = subtype;
        }

        public RouteSpecialization(String name) {
            this.name = name;
        }
    }

    public Route(String name) {
        this.name = name;
    }

    public void addHearthFireWaypoint(String name) {
        Gob player = NUtils.player();
        Coord2d rc = player.rc;
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;

        if(player == null || rc == null || cache == null) {
            return;
        }

        // Create a temporary waypoint to get its hash
        RoutePoint tempWaypoint = new RoutePoint(rc, cache, name);

        // Check if this waypoint already exists in the graph
        RoutePoint existingWaypoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().getPoint(tempWaypoint.id);

        // Use existing waypoint if found, otherwise use the temporary one
        RoutePoint waypointToAdd = existingWaypoint != null ? existingWaypoint : tempWaypoint;

        // Add the waypoint with default connection values
        try {
            try {
                waypointToAdd.addReachableAreas();
            } catch (InterruptedException e) {
                NUtils.getGameUI().error("Unable to determine reachable areas.");
            }

            ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().generateNeighboringConnections(waypointToAdd);
            synchronized (waypoints) {
                this.waypoints.add(waypointToAdd);
            }
        } catch (Exception e) {
            NUtils.getGameUI().msg("Failed to add waypoint: " + e.getMessage());
        }

        ((NMapView) NUtils.getGameUI().map).createRouteLabel(this.id);
    }

    public void addWaypoint() {
        Gob player = NUtils.player();
        Coord2d rc = player.rc;
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;

        if(player == null || rc == null || cache == null) {
            return;
        }

        // Create a temporary waypoint to get its hash
        RoutePoint tempWaypoint = new RoutePoint(rc, cache);
        
        // Check if this waypoint already exists in the graph
        RoutePoint existingWaypoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().getPoint(tempWaypoint.id);
        
        // Use existing waypoint if found, otherwise use the temporary one
        RoutePoint waypointToAdd = existingWaypoint != null ? existingWaypoint : tempWaypoint;

        // Add the waypoint with default connection values
        addPredefinedWaypoint(waypointToAdd, "", "", false);
    }

    public void addPredefinedWaypoint(RoutePoint routePoint, String doorHash, String doorName, boolean isDoor) {
        try {
            if(!waypoints.isEmpty()) {
                RoutePoint existingWaypoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().getPoint(routePoint.id);

                routePoint = existingWaypoint != null ? existingWaypoint : routePoint;

                RoutePoint lastRoutePoint = waypoints.get(waypoints.size() - 1);

                // Add neighbors if they do not already exist.
                if(!routePoint.getNeighbors().contains(lastRoutePoint.id)) {
                    routePoint.addNeighbor(lastRoutePoint.id);
                }

                if(!lastRoutePoint.getNeighbors().contains(routePoint.id)) {
                    lastRoutePoint.addNeighbor(routePoint.id);
                }

                // Add connections between the points if connections do not already exists
                if(!routePoint.connections.containsKey(lastRoutePoint.id)) {
                    routePoint.addConnection(lastRoutePoint.id, String.valueOf(lastRoutePoint.id), doorHash, doorName, isDoor);
                }

                if(!lastRoutePoint.connections.containsKey(routePoint.id)) {
                    lastRoutePoint.addConnection(routePoint.id, String.valueOf(routePoint.id), doorHash, doorName, isDoor);
                }
            }

            try {
                routePoint.addReachableAreas();
            } catch (InterruptedException e) {
                NUtils.getGameUI().error("Unable to determine reachable areas.");
            }

            ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().generateNeighboringConnections(routePoint);
            synchronized (waypoints) {
                this.waypoints.add(routePoint);
            }
            NUtils.getGameUI().msg("Waypoint added: " + routePoint);
            NUtils.getGameUI().msg("Neighbors: " + routePoint.getNeighbors());
        } catch (Exception e) {
            NUtils.getGameUI().msg("Failed to add waypoint: " + e.getMessage());
        }

        ((NMapView) NUtils.getGameUI().map).createRouteLabel(this.id);
    }

    public void addPredefinedWaypointNoConnections(RoutePoint routePoint) {
        try {
            if(!waypoints.isEmpty()) {
                RoutePoint existingWaypoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().getPoint(routePoint.id);

                routePoint = existingWaypoint != null ? existingWaypoint : routePoint;
            }


            ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().generateNeighboringConnections(routePoint);

            try {
                routePoint.addReachableAreas();
            } catch (InterruptedException e) {
                NUtils.getGameUI().error("Unable to determine reachable areas.");
            }

            synchronized (waypoints) {
                this.waypoints.add(routePoint);
            }
            NUtils.getGameUI().msg("Waypoint added: " + routePoint);
            NUtils.getGameUI().msg("Neighbors: " + routePoint.getNeighbors());
        } catch (InterruptedException e) {
            NUtils.getGameUI().msg("Failed to add waypoint: " + e.getMessage());
        }

        ((NMapView) NUtils.getGameUI().map).createRouteLabel(this.id);
    }

    public void addRandomWaypoint() {
        Gob player = NUtils.player();
        Coord2d rc = player.rc;

        // Create a temporary waypoint to get its hash
        RoutePoint tempWaypoint = new RoutePoint(rc, NUtils.getGameUI().ui.sess.glob.map);

        // Check if this waypoint already exists in the graph
        RoutePoint existingWaypoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().getPoint(tempWaypoint.id);

        // Use existing waypoint if found, otherwise use the temporary one
        RoutePoint waypointToAdd = existingWaypoint != null ? existingWaypoint : tempWaypoint;

        try {
            ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().generateNeighboringConnections(waypointToAdd);
            waypointToAdd.addReachableAreas();
            synchronized (waypoints) {
                this.waypoints.add(waypointToAdd);
            }
            NUtils.getGameUI().msg("Waypoint added: " + waypointToAdd);
            NUtils.getGameUI().msg("Neighbors: " + waypointToAdd.getNeighbors());
        } catch (Exception e) {
            NUtils.getGameUI().msg("Failed to add waypoint: " + e.getMessage());
        }

        ((NMapView) NUtils.getGameUI().map).createRouteLabel(this.id);
    }

    public void deleteWaypoint(RoutePoint waypoint) {
        List<RoutePoint> toRemove = new ArrayList<>();

        for (RoutePoint point : waypoints) {
            List<Integer> neighbors = point.getNeighbors();
            if (neighbors != null && neighbors.contains(waypoint.id)) {
                neighbors.remove(Integer.valueOf(waypoint.id));
                point.removeConnection(waypoint.id);
            }

            if (point.id == waypoint.id) {
                toRemove.add(point);
            }
        }

        synchronized (waypoints) {
            waypoints.removeAll(toRemove);
        }
        ((NMapView) NUtils.getGameUI().map).routeGraphManager.deleteRoutePointFromNeighborsAndConnections(waypoint);
        ((NMapView) NUtils.getGameUI().map).routeGraphManager.updateRoute(this);
        ((NMapView) NUtils.getGameUI().map).routeGraphManager.updateGraph();
    }

    public RoutePoint getLastWaypoint() {
        if (waypoints.isEmpty()) return null;
        return waypoints.get(waypoints.size() - 1);
    }

    public RoutePoint getSecondToLastWaypoint() {
        if (waypoints.size() < 2) return null;
        return waypoints.get(waypoints.size() - 2);
    }

    public Route(JSONObject obj) {
        this.name = obj.getString("name");
        this.id = obj.getInt("id");

        if (obj.has("path")) {
            this.path = obj.getString("path");
        } else if (obj.has("dir")) {
            this.path = "/" + obj.getString("path");
        }

        this.waypoints.clear();
        if (obj.has("waypoints")) {
            JSONArray arr = obj.getJSONArray("waypoints");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject point = arr.getJSONObject(i);
                long gridId = point.getLong("gridId");
                JSONObject localCoord = point.getJSONObject("localCoord");
                int x = localCoord.getInt("x");
                int y = localCoord.getInt("y");
                String hearthFirePlayerName = "";
                if(point.has("hearthFirePlayerName")) {
                    hearthFirePlayerName = point.getString("hearthFirePlayerName");
                }

                RoutePoint waypoint = new RoutePoint(gridId, new Coord(x, y), hearthFirePlayerName);
                
                // Load original position if it exists
                if (point.has("originalGridId") && point.has("originalLocalCoord")) {
                    waypoint.originalGridId = point.getLong("originalGridId");
                    JSONObject originalLocalCoord = point.getJSONObject("originalLocalCoord");
                    waypoint.originalLocalCoord = new Coord(originalLocalCoord.getInt("x"), originalLocalCoord.getInt("y"));
                }
                
                // Load neighbors if they exist
                if (point.has("neighbors")) {
                    JSONArray neighbors = point.getJSONArray("neighbors");
                    for (int j = 0; j < neighbors.length(); j++) {
                        waypoint.addNeighbor(neighbors.getInt(j));
                    }
                }
                
                // Load connections if they exist
                if (point.has("connections")) {
                    JSONObject connections = point.getJSONObject("connections");
                    for (String neighborHash : connections.keySet()) {
                        JSONObject conn = connections.getJSONObject(neighborHash);
                        String connectionTo = conn.has("connectionTo") ? conn.getString("connectionTo") : "";
                        String connGobHash = conn.has("gobHash") ? conn.getString("gobHash") : "";
                        String connGobName = conn.has("gobName") ? conn.getString("gobName") : "";
                        boolean isDoor = conn.has("isDoor") ? conn.getBoolean("isDoor") : false;
                        waypoint.addConnection(Integer.parseInt(neighborHash), connectionTo, connGobHash, connGobName, isDoor);
                    }
                }

                if (point.has("reachableAreas")) {
                    JSONArray reachableAreas = point.getJSONArray("reachableAreas");
                    for (int j = 0; j < reachableAreas.length(); j++) {
                        if(reachableAreas.get(j) instanceof JSONObject) {
                            JSONObject jra = (JSONObject) reachableAreas.get(j);
                            waypoint.addReachableArea(jra.getInt("id"), jra.getDouble("dist"));
                        }
                    }
                }
                synchronized (waypoints) {
                    waypoints.add(waypoint);
                }
            }
        }

        this.spec = new ArrayList<>();
        if (obj.has("specializations")) {
            JSONArray arr = obj.getJSONArray("specializations");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject spec = arr.getJSONObject(i);
                String name = spec.getString("name");
                String subtype = spec.has("subtype") ? spec.getString("subtype") : null;
                this.spec.add(new RouteSpecialization(name, subtype));
            }
        }
    }

    public Route(JSONObject obj, Map<Integer, RoutePoint> routePointMap) {
        this.name = obj.getString("name");
        this.id = obj.getInt("id");

        if (obj.has("path")) {
            this.path = obj.getString("path");
        } else if (obj.has("dir")) {
            this.path = "/" + obj.getString("path");
        }

        this.waypoints.clear();
        if (obj.has("waypoints")) {
            JSONArray arr = obj.getJSONArray("waypoints");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject point = arr.getJSONObject(i);
                int id = point.getInt("id");
                long gridId = point.getLong("gridId");
                String hearthFirePlayerName = "";
                if(point.has("hearthFirePlayerName")) {
                    hearthFirePlayerName = point.getString("hearthFirePlayerName");
                }

                RoutePoint waypoint;
                if (routePointMap.containsKey(id)) {
                    waypoint = routePointMap.get(id);
                } else {
                    JSONObject localCoord = point.getJSONObject("localCoord");
                    int x = localCoord.getInt("x");
                    int y = localCoord.getInt("y");
                    waypoint = new RoutePoint(gridId, new Coord(x, y), hearthFirePlayerName);

                    // Load original position if it exists
                    if (point.has("originalGridId") && point.has("originalLocalCoord")) {
                        waypoint.originalGridId = point.getLong("originalGridId");
                        JSONObject originalLocalCoordObj = point.getJSONObject("originalLocalCoord");
                        waypoint.originalLocalCoord = new Coord(originalLocalCoordObj.getInt("x"), originalLocalCoordObj.getInt("y"));
                    }

                    // Load neighbors if they exist
                    if (point.has("neighbors")) {
                        JSONArray neighbors = point.getJSONArray("neighbors");
                        for (int j = 0; j < neighbors.length(); j++) {
                            waypoint.addNeighbor(neighbors.getInt(j));
                        }
                    }

                    // Load connections if they exist
                    if (point.has("connections")) {
                        JSONObject connections = point.getJSONObject("connections");
                        for (String neighborHash : connections.keySet()) {
                            JSONObject conn = connections.getJSONObject(neighborHash);
                            String connectionTo = conn.has("connectionTo") ? conn.getString("connectionTo") : "";
                            String connGobHash = conn.has("gobHash") ? conn.getString("gobHash") : "";
                            String connGobName = conn.has("gobName") ? conn.getString("gobName") : "";
                            boolean isDoor = conn.has("isDoor") ? conn.getBoolean("isDoor") : false;
                            waypoint.addConnection(Integer.parseInt(neighborHash), connectionTo, connGobHash, connGobName, isDoor);
                        }
                    }

                    if (point.has("reachableAreas")) {
                        JSONArray reachableAreas = point.getJSONArray("reachableAreas");
                        for (int j = 0; j < reachableAreas.length(); j++) {
                            if(reachableAreas.get(j) instanceof JSONObject) {
                                JSONObject jra = (JSONObject) reachableAreas.get(j);
                                waypoint.addReachableArea(jra.getInt("id"), jra.getDouble("dist"));
                            }
                        }
                    }
                    routePointMap.put(id, waypoint);

                }
                synchronized (waypoints) {
                    waypoints.add(waypoint);
                }
            }
        }

        this.spec = new ArrayList<>();
        if (obj.has("specializations")) {
            JSONArray arr = obj.getJSONArray("specializations");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject spec = arr.getJSONObject(i);
                String name = spec.getString("name");
                String subtype = spec.has("subtype") ? spec.getString("subtype") : null;
                this.spec.add(new RouteSpecialization(name, subtype));
            }
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("path", path);
        
        // Save waypoints
        JSONArray waypointsArray = new JSONArray();
        for (RoutePoint waypoint : waypoints) {
            JSONObject waypointJson = new JSONObject();
            waypointJson.put("id", waypoint.id);
            waypointJson.put("gridId", waypoint.gridId);
            waypointJson.put("hearthFirePlayerName", waypoint.hearthFirePlayerName);
            waypointJson.put("localCoord", new JSONObject()
                .put("x", waypoint.localCoord.x)
                .put("y", waypoint.localCoord.y));
            
            // Save original position for drag limiting
            waypointJson.put("originalGridId", waypoint.originalGridId);
            waypointJson.put("originalLocalCoord", new JSONObject()
                .put("x", waypoint.originalLocalCoord.x)
                .put("y", waypoint.originalLocalCoord.y));
            
            // Save neighbors
            JSONArray neighborsArray = new JSONArray();
            for (int neighborId : waypoint.getNeighbors()) {
                neighborsArray.put(neighborId);
            }
            waypointJson.put("neighbors", neighborsArray);
            
            // Save connections
            JSONObject connectionsJson = new JSONObject();
            for (int neighborHash : waypoint.getConnectedNeighbors()) {
                RoutePoint.Connection conn = waypoint.getConnection(neighborHash);
                if (conn != null) {
                    JSONObject connJson = new JSONObject();
                    connJson.put("connectionTo", conn.connectionTo);
                    connJson.put("gobHash", conn.gobHash);
                    connJson.put("gobName", conn.gobName);
                    connJson.put("isDoor", conn.isDoor);
                    connectionsJson.put(String.valueOf(neighborHash), connJson);
                }
            }
            waypointJson.put("connections", connectionsJson);

            // Save reachable areas
            JSONArray reachableAreas = new JSONArray();
            for (int reachableArea : waypoint.getReachableAreas()) {
                JSONObject jra = new JSONObject();
                jra.put("id",reachableArea);
                jra.put("dist",waypoint.getDistanceToArea(reachableArea));
                reachableAreas.put(jra);
            }
            waypointJson.put("reachableAreas", reachableAreas);
            
            waypointsArray.put(waypointJson);
        }
        json.put("waypoints", waypointsArray);
        
        // Save specializations
        JSONArray specArray = new JSONArray();
        for (RouteSpecialization s : spec) {
            JSONObject specJson = new JSONObject();
            specJson.put("name", s.name);
            if (s.subtype != null) {
                specJson.put("subtype", s.subtype);
            }
            specArray.put(specJson);
        }
        json.put("specializations", specArray);
        
        return json;
    }

    public boolean hasSpecialization(String name) {
        for (RouteSpecialization s : spec) {
            if (s.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean removeSpecialization(String name) {
        for (int i = 0; i < spec.size(); i++) {
            if (spec.get(i).name.equals(name)) {
                spec.remove(i);
                return true;
            }
        }
        return false;
    }
}
