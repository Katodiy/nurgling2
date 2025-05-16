package nurgling.actions;

import haven.Coord2d;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tasks.NTask;

public class RouteWalker implements Action {

    private final Route route;
    private final boolean forward;

    public RouteWalker(Route route, boolean forward) {
        this.route = route;
        this.forward = forward;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (route == null || route.waypoints == null || route.waypoints.isEmpty())
            return Results.ERROR("No route or waypoints defined.");

        if (forward) {
            RoutePoint last = route.waypoints.getLast();
            Thread t = new Thread(() -> {
                try {
                    new RoutePointNavigator(last).run(NUtils.getGameUI());
                } catch (InterruptedException e) {
                    NUtils.getGameUI().error("Navigation interrupted by the user");
                }
            }, "RoutePointNavigator");
            t.start();
            NUtils.getGameUI().biw.addObserve(t);
            t.join();
        }

        return Results.SUCCESS();
    }

    private void goToStart(NGameUI gui, int lastVisited) throws InterruptedException {
        for (int j = lastVisited; j >= 0; j--) {
            RoutePoint backtrackPoint = route.waypoints.get(j);
            Coord2d backtrackTarget = backtrackPoint.toCoord2d(gui.map.glob.map);
            if (backtrackTarget != null)
                new PathFinder(backtrackTarget).run(gui);
        }
    }

    private void goToEnd(NGameUI gui, int lastVisited) throws InterruptedException {
        for (int j = lastVisited + 1; j < route.waypoints.size(); j++) {
            RoutePoint forwardPoint = route.waypoints.get(j);
            Coord2d forwardTarget = forwardPoint.toCoord2d(gui.map.glob.map);
            if (forwardTarget != null)
                new PathFinder(forwardTarget).run(gui);
        }
    }

    private void returnToLastVisited(NGameUI gui, int lastVisited) throws InterruptedException {
        for (int j = 1; j <= lastVisited; j++) {
            Coord2d resume = route.waypoints.get(j).toCoord2d(gui.map.glob.map);
            if (resume != null)
                new PathFinder(resume).run(gui);
        }
    }
}
