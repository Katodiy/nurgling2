package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import nurgling.NCore;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;

public class WaitDistance extends NTask {
    Coord2d last;
    double dist;
    Route route;
    Gob oldPlayer;
    private final GateDetector gateDetector;

    public WaitDistance(Coord2d last, double dist, Route route) {
        this.last = last;
        this.dist = dist;
        this.route = route;
        this.oldPlayer = NUtils.player();
        this.gateDetector = new GateDetector();
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
            return true;
        }

        // Check if we've passed through a gate
        if(gateDetector.hasPassedGate()) {
            // Signal that we've passed through by returning true
            // RouteAutoRecorder will handle the waypoint creation
            route.hasPassedGate = true;
            route.lastPassedGate = gateDetector.getLastNearbyGate();
            return true;
        }

        // Check if we're near a gate
        if(gateDetector.isNearGate()) {
            // Store the current position in route's cachedRoutePoint
            route.cachedRoutePoint = new RoutePoint(player.rc, NUtils.getGameUI().ui.sess.glob.map);

            return false;
        }

        // Check if we're moving away from a gate without passing through
        if(gateDetector.isMovingAwayFromGate()) {
            // Clear the cached point since we didn't pass through
            route.cachedRoutePoint = null;
            route.hasPassedGate = false;
            route.lastPassedGate = null;
            gateDetector.reset();
            return false;
        }

        return player.rc.dist(last) >= dist;
    }
}
