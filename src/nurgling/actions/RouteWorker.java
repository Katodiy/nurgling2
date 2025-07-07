package nurgling.actions;

import haven.Coord2d;
import nurgling.NGameUI;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tasks.NTask;

public class RouteWorker implements Action {

    private final Route route;
    private final Action action;
    private final boolean backtrack;

    // When to come back to the start (Optional);
    private NTask predicate = null;
    // What to do when you come back to first waypoint (Optional);
    private Action returnAction = null;
    private Action finalAction = null;

    public RouteWorker(Action action, Route route, boolean backtrack) {
        this.route = route;
        this.action = action;
        this.backtrack = backtrack;
    }

    public RouteWorker(Action action, Route route, boolean backtrack, NTask predicate, Action returnAction, Action finalAction) {
        this.route = route;
        this.action = action;
        this.backtrack = backtrack;
        this.predicate = predicate;
        this.returnAction = returnAction;
        this.finalAction = finalAction;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (route == null || route.waypoints.isEmpty())
            return Results.ERROR("No route or waypoints defined.");

        int lastVisited = 0;

        for (int i = 0; i < route.waypoints.size(); i++) {
            RoutePoint rp = route.waypoints.get(i);
            Coord2d target = rp.toCoord2d(gui.map.glob.map);
            if (target == null) continue;

            new PathFinder(target).run(gui);
            lastVisited = i;

            action.run(gui);

            if (predicate != null && predicate.check()) {
                gui.msg("Predicate triggered. Backtracking to start.");

                goToStart(gui, lastVisited);

                if (returnAction != null) {
                    returnAction.run(gui);
                }

                if (lastVisited > 0) {
                    returnToLastVisited(gui, lastVisited);
                }
                i=i-1;
            }
        }

        if (backtrack) {
            goToStart(gui, lastVisited);
        }

        if (finalAction != null) {
            finalAction.run(gui);
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

    private void returnToLastVisited(NGameUI gui, int lastVisited) throws InterruptedException {
        for (int j = 1; j <= lastVisited; j++) {
            Coord2d resume = route.waypoints.get(j).toCoord2d(gui.map.glob.map);
            if (resume != null)
                new PathFinder(resume).run(gui);
        }
    }
}
