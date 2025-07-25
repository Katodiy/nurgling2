package nurgling.routes;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.PathFinder;
import nurgling.areas.NArea;
import nurgling.areas.NGlobalCoord;
import nurgling.tools.Finder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RouteGraph {
    private final int MAX_DISTANCE_FOR_NEIGHBORS = 250;
    public final Map<Integer, RoutePoint> points = new ConcurrentHashMap<>();
    private final Map<Long, ArrayList<RoutePoint>> pointsByGridId = new HashMap<>();
    private final Map<String, RoutePoint> doors = new HashBMap<>();

    private long lastPlayerGridId;
    private Coord lastPlayerCoord;
    private double lastMovementDirection;

    public long getLastPlayerGridId() {
        return lastPlayerGridId;
    }

    public void setLastPlayerGridId(long lastPlayerGridId) {
        this.lastPlayerGridId = lastPlayerGridId;
    }

    public Coord getLastPlayerCoord() {
        return lastPlayerCoord;
    }

    public void setLastPlayerCoord(Coord lastPlayerCoord) {
        this.lastPlayerCoord = lastPlayerCoord;
    }

    public double getLastMovementDirection() {
        return lastMovementDirection;
    }

    public void setLastMovementDirection(double lastMovementDirection) {
        this.lastMovementDirection = lastMovementDirection;
    }

    public void addRoute(Route route) {
        for (RoutePoint point : route.waypoints) {
            points.put(point.id, point);
            pointsByGridId.computeIfAbsent(point.gridId, k -> new ArrayList<>()).add(point);
            generateDoors(point);
        }
    }

    public void clear() {
        points.clear();
    }

    public void generateNeighboringConnections(RoutePoint waypoint) throws InterruptedException {
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;
        Coord2d waypointRelativeCoords = waypoint.toCoord2d(cache);

        if(waypointRelativeCoords == null) {
            return;
        }

        for (RoutePoint point : points.values()) {
            Coord2d pointRelativeCoords = point.toCoord2d(cache);
            if (pointRelativeCoords != null) {
                double distanceToAVisibleNode = waypointRelativeCoords.dist(pointRelativeCoords);
                boolean isReachable = PathFinder.isAvailable(waypointRelativeCoords, pointRelativeCoords, true);
                if (distanceToAVisibleNode <= MAX_DISTANCE_FOR_NEIGHBORS && isReachable && waypoint.id != point.id) {

                    // Add neighbors if they do not already exist.
                    if(!waypoint.getNeighbors().contains(point.id)) {
                        waypoint.addNeighbor(point.id);
                    }

                    if(!point.getNeighbors().contains(waypoint.id)) {
                        point.addNeighbor(waypoint.id);
                    }
                    
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
            Coord2d playerCoords = NUtils.player().rc;
            for (RoutePoint point : points.values()) {
                Coord2d pointCoords = new NGlobalCoord(point.gridId, point.localCoord).getCurrentCoord();
                if(pointCoords!=null) {
                    double dist = pointCoords.dist(playerCoords);

                    if (dist < currentDistanceToClosestPoint) {
                        currentDistanceToClosestPoint = dist;
                        nearestPoint = point;
                    }
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

        PriorityQueue<AStarNode> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Integer, Double> gScore = new HashMap<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();

        open.add(new AStarNode(start.id, heuristic(start, end)));
        gScore.put(start.id, 0.0);

        while (!open.isEmpty()) {
            AStarNode cur = open.poll();
            if (cur.id == end.id) return reconstructPath(end.id, cameFrom);

            for (Integer neighborId : points.get(cur.id).getNeighbors()) {
                RoutePoint neighbor = points.get(neighborId);
                if (neighbor == null) continue;

                double tentativeG = gScore.get(cur.id) + dist(cur.id, neighborId);
                if (tentativeG < gScore.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    cameFrom.put(neighborId, cur.id);
                    gScore.put(neighborId, tentativeG);
                    open.add(new AStarNode(neighborId, tentativeG + heuristic(neighbor, end)));
                }
            }
        }
        return null;
    }

    private double heuristic(RoutePoint a, RoutePoint b) {
        Coord2d ca = a.toCoord2d(NUtils.getGameUI().ui.sess.glob.map);
        Coord2d cb = b.toCoord2d(NUtils.getGameUI().ui.sess.glob.map);
        return ca == null || cb == null ? 0 : ca.dist(cb);
    }

    private double dist(int aId, int bId) {
        return heuristic(points.get(aId), points.get(bId));
    }

    private static class AStarNode {
        final int id; double f;
        AStarNode(int id, double f) {
            this.id = id;
            this.f = f;
        }
    }

    public RoutePoint findAreaRoutePoint(NArea area) {
        RoutePoint end = null;
        double dist = Double.MAX_VALUE;
        for(RoutePoint point : points.values()) {
            if(point.getReachableAreas().contains(area.id)) {
                double distCand = point.getDistanceToArea(area.id);
                if(distCand<dist) {
                    dist = distCand;
                    end = point;
                }
            }
        }

        return end;
    }

    public void connectAreaToRoutePoints(NArea area) {
        if (area == null) {
            return;
        }

        ArrayList<RoutePoint> points = findNearestPoints();
        MCache cache = NUtils.getGameUI().ui.sess.glob.map;
        Pair<Coord2d, Coord2d> testrc = area.getRCArea();
        try {
            if (testrc != null) {
                ArrayList<Gob> gobs = Finder.findGobs(area);
                for (RoutePoint point : points) {
                    Coord2d rcpoint = point.toCoord2d(cache);
                    if (point.toCoord2d(cache) != null) {
                        boolean isReachable = false;


                        if (gobs.isEmpty()) {
                                isReachable = PathFinder.isAvailable(testrc.a, rcpoint, false) || PathFinder.isAvailable(testrc.b, rcpoint, false);
                        } else {
                            for (Gob gob : gobs) {
                                if (PathFinder.isAvailable(rcpoint, gob, true)) {
                                    isReachable = true;
                                    break;
                                }
                            }
                        }
                        if (isReachable) {
                            point.addReachableArea(area.id, area.getDistance(rcpoint));
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            NUtils.getGameUI().error("Unable to determine route point reachability from point to area. Skipping point");
        }
    }


    public ArrayList<RoutePoint> findNearestRoutePoints(NArea area) {
        ArrayList<RoutePoint> result = new ArrayList<>();
        if(area == null) {
            return result;
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

            if(isReachable || true) {
                result.add(point);
            }
        }
        return result;
    }

    public void deleteAreaFromRoutePoints(int areaId) {
        for(RoutePoint point : points.values()) {
            point.deleteReachableArea(areaId);
        }
        NConfig.needRoutesUpdate();
    }

    private List<RoutePoint> reconstructPath(Integer end, Map<Integer, Integer> cameFrom) {
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

    public void generateDoors(RoutePoint routePoint) {
        for(RoutePoint.Connection connection : routePoint.getConnections()) {
            if(connection.isDoor && !doors.containsKey(connection.gobHash)) {
                doors.put(connection.gobHash, routePoint);
            }
        }
    }

    public Map<String, RoutePoint> getDoors() {
        return this.doors;
    }

    public void deleteDoor(String door) {
        this.doors.remove(door);
    }

    public void clearDoors() {
        this.doors.clear();
    }
}
