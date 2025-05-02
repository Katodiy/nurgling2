package nurgling.actions.bots;

import haven.Coord2d;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.tasks.WaitAnotherAmount;
import nurgling.tasks.WaitDistance;

public class RouteAutoRecorder implements Runnable {
    private final Route route;
    private boolean running = true;
    private final double interval = 77.0;

    public RouteAutoRecorder(Route route) {
        this.route = route;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
            Coord2d last = NUtils.player().rc;
            route.addWaypoint();

            while (running) {
                try {
                    NUtils.getUI().core.addTask( new WaitDistance(last, interval));
                } catch (InterruptedException e) {
                    NUtils.getGameUI().msg("Stopped route recording for: " + route.name);
                }

                if (!running) break;
                last = NUtils.player().rc;
                route.addWaypoint();
            }
    }
}

