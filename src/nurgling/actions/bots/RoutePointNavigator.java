package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import nurgling.tasks.WaitForMapLoad;

import java.util.List;

public class RoutePointNavigator implements Action {
    private final RoutePoint targetPoint;
    private final RouteGraph graph;

    public RoutePointNavigator(RoutePoint targetPoint) {
        this.targetPoint = targetPoint;
        this.graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui == null || gui.map == null || gui.map.glob == null || gui.map.glob.map == null) {
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
        List<RoutePoint> path = graph.findPath(startPoint, targetPoint);
        if (path == null || path.isEmpty()) {
            gui.error("No path found to target waypoint");
            return Results.FAIL();
        }

        // Navigate the path
        for (int i = 0; i<path.size(); i++) {
            RoutePoint currentPoint = path.get(i);
            RoutePoint nextPoint = null;

            if(i<path.size()-1) {
                nextPoint = path.get(i+1);
            }

            Coord2d target = path.get(i).toCoord2d(gui.map.glob.map);
            if (target == null) continue;

            new PathFinder(target).run(gui);


            // We open the door only when the current point is special and the next point in the path is unreachable
            if(currentPoint.isDoor && nextPoint != null && nextPoint.toCoord2d(gui.map.glob.map) == null) {
                NUtils.openDoor(gui);
                // Wait until we can safely get coordinates for the next waypoint
                NUtils.getUI().core.addTask(new WaitForMapLoad(nextPoint, gui));
            }
        }

        return Results.SUCCESS();
    }
} 