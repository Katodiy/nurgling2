package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import nurgling.tasks.WaitForMapLoad;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.List;

import static haven.OCache.posres;

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
            RoutePoint previousPoint = null;
            RoutePoint nextPoint = null;
            if(i != 0) {
                previousPoint = path.get(i-1);
            }
            if(i<path.size()-1) {
                nextPoint = path.get(i+1);
            }

            Coord2d target = path.get(i).toCoord2d(gui.map.glob.map);
            if (target == null) continue;

            new PathFinder(target).run(gui);


            // We open the door only when the current point is special and the next point in the path is unreachable
            if(currentPoint.isDoor && nextPoint != null && nextPoint.toCoord2d(gui.map.glob.map) == null) {
                openDoor(gui);
                // Wait until we can safely get coordinates for the next waypoint
                NUtils.getUI().core.addTask(new WaitForMapLoad(nextPoint, gui));
            }
        }

        return Results.SUCCESS();
    }

    private void openDoor(NGameUI gui) throws InterruptedException {
        Gob arch = Finder.findGob(NUtils.player().rc, new NAlias("gfx/terobjs/arch/stonestead", "gfx/terobjs/arch/stonemansion", "gfx/terobjs/arch/greathall", "gfx/terobjs/arch/primitivetent", "gfx/terobjs/arch/windmill", "gfx/terobjs/arch/stonetower", "gfx/terobjs/arch/logcabin", "gfx/terobjs/arch/timberhouse", "gfx/terobjs/minehole", "gfx/terobjs/ladder"), null, 100);
        if (arch != null) {
            if (NParser.checkName(arch.ngob.name, "gfx/terobjs/arch/greathall")) {
                Coord2d A = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.begin.y).rot(arch.a).add(arch.rc);
                Coord2d B = new Coord2d(arch.ngob.hitBox.end.x, arch.ngob.hitBox.end.y).rot(arch.a).add(arch.rc);
                Coord2d C = B.sub(A).div(2).add(A);
                double a = A.add(B.sub(A).div(4)).dist(NUtils.player().rc);
                double b = B.add(A.sub(B).div(4)).dist(NUtils.player().rc);
                double c = C.dist(NUtils.player().rc);
                if (a < b && a < c)
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 18);
                else if (b < c && b < a)
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 16);
                else
                    gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                            0, 17);
            } else {
                gui.map.wdgmsg("click", Coord.z, arch.rc.floor(posres), 3, 0, 1, (int) arch.id, arch.rc.floor(posres),
                        0, 16);
            }
        }
    }
} 