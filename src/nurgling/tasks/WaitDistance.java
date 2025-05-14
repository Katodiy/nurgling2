package nurgling.tasks;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NCore;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;

import java.util.Arrays;
import java.util.List;

public class WaitDistance extends NTask {
    Coord2d last;
    double dist;
    Route route;
    RoutePoint routePointLeftBehindDoor = null;
    Gob oldPlayer;

    public WaitDistance(Coord2d last, double dist, Route route) {
        this.last = last;
        this.dist = dist;
        this.route = route;
        this.oldPlayer = NUtils.player();
    }

    @Override
    public boolean check() {
        String currentActionGobName = "";
        NCore.LastActions lastAction = null;
        if(NUtils.getUI() != null && NUtils.getUI().core.getLastActions() != null) {
            lastAction = NUtils.getUI().core.getLastActions();
            currentActionGobName = lastAction.gob.ngob.name;
        }

        Gob player = NUtils.player();

        if(route.lastAction != null && isNonLoadingDoor(route.lastAction.gob.ngob.name) && route.lastAction.gob.ngob.name != currentActionGobName) {
            route.lastAction = null;
            return true;
        }

        route.lastAction = lastAction;

        if (player != this.oldPlayer)
            return true;

        this.oldPlayer = player;

        this.routePointLeftBehindDoor = getCurrentImaginaryRoutePoint(player.rc);

        if(NUtils.getUI().core.getLastActions() != null) {

            this.route.cachedRoutePoint = this.routePointLeftBehindDoor;
        }

        if(last == null) {
            return true;
        }

        return player.rc.dist(last) >= dist;
    }

    private RoutePoint getCurrentImaginaryRoutePoint(Coord2d playerRC) {
        Coord tilec = playerRC.div(MCache.tilesz).floor();
        MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);
        Coord localCoord = tilec.sub(grid.ul);

        return new RoutePoint(grid.id, localCoord, false, "");
    }

    private boolean isNonLoadingDoor(String lastClickedDoor) {
        List<String> listOfDoors = Arrays.asList("stairs");
        for (String door : listOfDoors) {
            if(lastClickedDoor.contains(door) && !lastClickedDoor.contains("cellar")) {
                return true;
            }
        }

        return false;
    }
}
