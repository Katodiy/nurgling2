package nurgling.tasks;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NCore;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;

public class WaitDistance extends NTask {
    Coord2d last;
    double dist;
    Route route;
    Gob oldPlayer;

    public WaitDistance(Coord2d last, double dist, Route route) {
        this.last = last;
        this.dist = dist;
        this.route = route;
        this.oldPlayer = NUtils.player();
    }

    @Override
    public boolean check() {
        NCore.LastActions lastAction = null;
        if(NUtils.getUI() != null && NUtils.getUI().core.getLastActions() != null) {
            lastAction = NUtils.getUI().core.getLastActions();
        }

        Gob player = NUtils.player();

        if (player != this.oldPlayer)
            return true;

        this.oldPlayer = player;

        if (lastAction != null) {
            route.lastAction = lastAction;
        }
        
        if(last == null) {
            return false;
        }

        return player.rc.dist(last) >= dist;
    }

    private RoutePoint getCurrentImaginaryRoutePoint(Coord2d playerRC) {
        Coord tilec = playerRC.div(MCache.tilesz).floor();
        MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);
        Coord localCoord = tilec.sub(grid.ul);

        return new RoutePoint(grid.id, localCoord);
    }
}
