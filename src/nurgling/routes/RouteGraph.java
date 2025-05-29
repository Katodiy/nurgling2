package nurgling.routes;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.PathFinder;
import nurgling.areas.NArea;
import nurgling.tools.Finder;

import java.util.*;

public class RouteGraph {
    private final int MAX_DISTANCE_FOR_NEIGHBORS = 250;
    private final Map<Integer, RoutePoint> points = new HashMap<>();
    private final Map<Long, ArrayList<RoutePoint>> pointsByGridId = new HashMap<>();

    public void addRoute(Route route) {
        for (RoutePoint point : route.waypoints) {
            points.put(point.id, point);
            pointsByGridId.computeIfAbsent(point.gridId, k -> new ArrayList<>()).add(point);
        }
    }

    public void clear() {
        points.clear();
    }

    public void generateNeighboringConnections(RoutePoint waypoint) throws InterruptedException {
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;
        Coord2d waypointRelativeCoords = waypoint.toCoord2d(cache);

        for (RoutePoint point : points.values()) {
            Coord2d pointRelativeCoords = point.toCoord2d(cache);
            if (pointRelativeCoords != null) {
                double distanceToAVisibleNode = waypointRelativeCoords.dist(pointRelativeCoords);
                boolean isReachable = PathFinder.isAvailable(waypointRelativeCoords, pointRelativeCoords, true);
                if (distanceToAVisibleNode <= MAX_DISTANCE_FOR_NEIGHBORS && isReachable) {
                    // Add neighbors
                    waypoint.addNeighbor(point.id);
                    point.addNeighbor(waypoint.id);
                    
                    // Add connections
                    waypoint.addConnection(point.id, String.valueOf(point.id), "", "", false);
                    point.addConnection(waypoint.id, String.valueOf(waypoint.id), "", "", false);
                }
            }
        }
    }

    public RoutePoint findNearestPointToPlayer(NGameUI gui) {
        Gob player = gui.map.player();

        Coord playerTile = player.rc.floor(gui.map.glob.map.tilesz);
        MCache.Grid playerGrid = gui.map.glob.map.getgridt(playerTile);

        long playerGridId = playerGrid.id;
        Coord playerLocalCoord = playerTile.sub(playerGrid.ul);

        return findNearestPoint(playerGridId, playerLocalCoord);
    }

    public RoutePoint findNearestPoint(long gridId, Coord localCoord) {
        RoutePoint nearestPoint = null;
        double currentDistanceToClosestPoint = Double.MAX_VALUE;
        
        // First check points in the same grid
        for (RoutePoint point : points.values()) {
            if (point.gridId == gridId) {
                double dist = point.localCoord.dist(localCoord);
                if (dist < currentDistanceToClosestPoint) {
                    currentDistanceToClosestPoint = dist;
                    nearestPoint = point;
                }
            }
        }
        
        // If no points found in the same grid, check all points
        if (nearestPoint == null) {
            for (RoutePoint point : points.values()) {
                Coord2d pointCoords = new Coord2d(point.localCoord);
                Coord2d playerCoords = NUtils.player().rc;

                double dist = pointCoords.dist(playerCoords);

                if (dist < currentDistanceToClosestPoint) {
                    currentDistanceToClosestPoint = dist;
                    nearestPoint = point;
                }
            }
        }
        
        return nearestPoint;
    }

    public ArrayList<RoutePoint> findNearestPoints() {
        ArrayList<RoutePoint> nearestPoints = new ArrayList<>();

        for (RoutePoint point : points.values()) {
            Coord2d pointCoords = new Coord2d(point.localCoord);

            if (pointCoords != null) {
                nearestPoints.add(point);
            }
        }

        return nearestPoints;
    }

    public List<RoutePoint> findPath(RoutePoint start, RoutePoint end) {
        if (start == null || end == null) return null;
        
        // Use breadth-first search to find the shortest path
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        
        queue.add(start.id);
        visited.add(start.id);
        
        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();
            
            if (currentId.equals(end.id)) {
                // Found the end point, reconstruct the path
                return reconstructPath(start.id, end.id, cameFrom);
            }
            
            // Get the current point and check all its neighbors
            RoutePoint current = points.get(currentId);
            if (current != null) {
                for (Integer neighborId : current.getNeighbors()) {
                    if (!visited.contains(neighborId)) {
                        visited.add(neighborId);
                        cameFrom.put(neighborId, currentId);
                        queue.add(neighborId);
                    }
                }
            }
        }
        
        return null;
    }

    public RoutePoint findAreaRoutePoint(NArea area) {
        RoutePoint end = null;

        for(RoutePoint point : points.values()) {
            if(point.getReachableAreas().contains(area.id)) {
                end = point;
            }
        }

        return end;
    }

    public void deleteWaypoint(RoutePoint waypoint) {
        points.remove(waypoint.id);

        for(RoutePoint point : points.values()) {
            point.getNeighbors().remove(Integer.valueOf(waypoint.id));
            point.removeConnection(waypoint.id);
        }
    }

    public void connectAreaToRoutePoints(NArea area) {
        if(area == null) {
            return;
        }

        ArrayList<RoutePoint> points = findNearestPoints();
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;
        for (RoutePoint point : points) {
            boolean isReachable = false;

            try {
                Pair<Coord2d, Coord2d> testrc = area.getRCArea();
                if(testrc != null) {
                    ArrayList<Gob> gobs = Finder.findGobs(area);

                    if(gobs.isEmpty()) {
                        if(point.toCoord2d(cache) != null) {
                            isReachable = PathFinder.isAvailable(testrc.a, point.toCoord2d(cache), false) || PathFinder.isAvailable(testrc.b, point.toCoord2d(cache), false);
                        } else {
                            isReachable = false;
                        }
                    } else {
                        for(Gob gob : gobs) {
                            if(point.toCoord2d(cache) != null) {
                                if (PathFinder.isAvailable(point.toCoord2d(cache), gob.rc, true)) {
                                    isReachable = true;
                                    break;
                                }
                            }
                        }
                    }
                }

            } catch (InterruptedException e) {
                NUtils.getGameUI().error("Unable to determine route point reachability from point to area. Skipping point: " + point.id);
            }

            if(isReachable) {
                point.addReachableArea(area.id);
            }
        }
    }

    public void deleteAreaFromRoutePoints(int areaId) {
        for(RoutePoint point : points.values()) {
            point.deleteReachableArea(areaId);
        }
        NConfig.needRoutesUpdate();
    }

    private List<RoutePoint> reconstructPath(Integer start, Integer end, Map<Integer, Integer> cameFrom) {
        List<RoutePoint> path = new ArrayList<>();
        Integer current = end;
        
        while (current != null) {
            path.add(0, points.get(current));
            current = cameFrom.get(current);
        }
        
        return path;
    }

    public Collection<RoutePoint> getPoints() {
        synchronized (points) {
            return new ArrayList<>(points.values());
        }
    }

    public RoutePoint getPoint(Integer id) {
        return points.get(id);
    }
}