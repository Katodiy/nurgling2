package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.actions.TravelToHearthFire;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.areas.NGlobalCoord;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
import nurgling.tools.Finder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoutePointNavigator implements Action {
    private final RoutePoint targetPoint;
    private final RouteGraph graph;
    private int areaId;

    public RoutePointNavigator(RoutePoint targetPoint) {
        this.targetPoint = targetPoint;
        this.graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();
    }

    public RoutePointNavigator(RoutePoint targetPoint, int areaId) {
        this.targetPoint = targetPoint;
        this.graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();
        this.areaId = areaId;
    }

    public RoutePointNavigator(Map<String, Object> settings) {
        Object areaIdObj = settings.get("areaId");
        if (areaIdObj == null) {
            throw new IllegalArgumentException("RoutePointNavigator requires areaId in settings.");
        }
        if (!(areaIdObj instanceof Integer)) {
            throw new IllegalArgumentException("RoutePointNavigator: areaId must be an Integer.");
        }

        this.areaId = (Integer) areaIdObj;
        NArea area = NContext.findAreaById(areaId);
        this.graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();
        this.targetPoint = this.graph.findAreaRoutePoint(area);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui == null || gui.map == null || gui.map.glob == null || gui.map.glob.map == null || this.targetPoint == null) {
            return Results.FAIL();
        }

        // Get player's current position
        Gob player = gui.map.player();
        if (player == null) {
            return Results.FAIL();
        }

        // Get player's grid and local coordinates
        Coord playerTile = player.rc.floor(gui.map.glob.map.tilesz);
        MCache.Grid playerGrid = gui.map.glob.map.getgridt(playerTile);
        long playerGridId = playerGrid.id;
        Coord playerLocalCoord = playerTile.sub(playerGrid.ul);

        // Find nearest waypoint to player
        RoutePoint startPoint = graph.findNearestPoint(playerGridId, playerLocalCoord);
        if (startPoint == null) {
            gui.error("No nearby waypoints found");
            return Results.FAIL();
        }

        // Find path to target
        List<RoutePoint> path = new ArrayList<>();
        List<RoutePoint> walkingPath = graph.findPath(startPoint, targetPoint);
        List<RoutePoint> alternativePathWithHF = new ArrayList<>();

        // Calculate path from hearthfire
        if (((Boolean) NConfig.get(NConfig.Key.useHFinGlobalPF) && walkingPath.size() > 10)) {
            RoutePoint hfRoutePoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getHearthFireForCurrentPlayer();
            if(hfRoutePoint != null) {
                alternativePathWithHF = graph.findPath(hfRoutePoint, targetPoint);
            }
        }

        if ((walkingPath == null || walkingPath.isEmpty()) && (alternativePathWithHF == null || alternativePathWithHF.isEmpty())) {
            gui.error(String.format("No path found to target waypoint. Start point: %s, end point: %s", startPoint.id, targetPoint.id));
            System.out.printf("No path found to target waypoint. Start point: %s, end point: %s%n", startPoint.id, targetPoint.id);
            return Results.FAIL();
        }

        if(walkingPath == null || walkingPath.isEmpty() || (alternativePathWithHF != null && !alternativePathWithHF.isEmpty() && (walkingPath.size() * 0.5 > alternativePathWithHF.size()))) {
            path = alternativePathWithHF;
            new TravelToHearthFire().run(gui);
            NUtils.getUI().core.addTask(new WaitForMapLoad(alternativePathWithHF.get(0), gui));
        } else {
            path = walkingPath;
        }

        // Navigate the path
        for (int i = 0; i<path.size(); i++) {
            RoutePoint currentPoint = path.get(i);
            RoutePoint previousPoint = null;
            RoutePoint nextPoint = null;

            if(i<path.size()-1) {
                nextPoint = path.get(i+1);
            }

            if(i-1 >= 0) {
                previousPoint = path.get(i-1);
            }

            Coord2d target = path.get(i).toCoord2d(gui.map.glob.map);
            if (target == null) {

                for(RoutePoint point : path) {
                    System.out.println(point.id);
                }

                gui.error(String.format("Target coord %s is null", path.get(i).id));
                System.out.printf("Target coord %s is null%n", path.get(i).id);
                continue;
            }

            if (NUtils.getGameUI().map.player() == null) {
                gui.error("Player was null, waiting for player");
                NUtils.getUI().core.addTask(new WaitPlayerNotNull());
            }

            new PathFinder(target).run(gui);

            // Handle door closing
            if(previousPoint != null) {
                RoutePoint.Connection prevConn = currentPoint.getConnection(previousPoint.id);
                if(prevConn != null && prevConn.isDoor && previousPoint.toCoord2d(gui.map.glob.map) != null) {
                    Gob gob = Finder.findGob(prevConn.gobHash);
                    if(gob != null && !GateDetector.isGobDoor(gob) && GateDetector.isDoorOpen(gob)) {
                        NUtils.openDoorOnAGob(gui, gob);
                        NUtils.getUI().core.addTask(new WaitGobModelAttrChange(gob, gob.ngob.getModelAttribute()));
                    }
                }
            }

            // Handle door opening
            if(nextPoint != null) {
                RoutePoint.Connection nextConn = currentPoint.getConnection(nextPoint.id);
                if(nextConn != null && nextConn.isDoor && needToPassDoor(nextConn, nextPoint, gui)) {
                    Gob gob = Finder.findGob(nextConn.gobHash);

                    if(gob == null) {
                        gui.error("Door not found.");
                        return Results.FAIL();
                    }

                    if (GateDetector.isGobDoor(gob)) {
                        // enter through the door
                        NUtils.openDoorOnAGob(gui, gob);
                        // Wait until we can safely get coordinates for the next waypoint
                        NUtils.getUI().core.addTask(new WaitForMapLoad(nextPoint, gui));
                        NUtils.getUI().core.addTask(new WaitForGobWithHash(nextPoint.getConnection(currentPoint.id).gobHash));
                    } else {
                        // open gate if its closed
                        if(!GateDetector.isDoorOpen(gob)) {
                            NUtils.openDoorOnAGob(gui, gob);
                            NUtils.getUI().core.addTask(new WaitGobModelAttrChange(gob, gob.ngob.getModelAttribute()));
                        }
                    }
                }
            }

            if(currentPoint.getReachableAreas().contains(areaId)) {
                return Results.SUCCESS();
            }
        }

        return Results.SUCCESS();
    }

    private boolean needToPassDoor(RoutePoint.Connection conn, RoutePoint nextPoint, NGameUI gui) {
        Gob gob = Finder.findGob(conn.gobHash);
        if (gob == null) {
            return false;
        }
        return nextPoint.toCoord2d(gui.map.glob.map) == null || gob.ngob.name.contains("stairs") || gob.ngob.name.contains("gate");
    }
}
