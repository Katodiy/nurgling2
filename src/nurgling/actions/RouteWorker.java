package nurgling.actions;

import haven.Coord2d;
import nurgling.NGameUI;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;

public class RouteWorker implements Action {

    private final Action action;
    private final Route route;
    private final boolean backtrack;

    public RouteWorker(Action action, Route route, boolean backtrack)
    {
        this.action = action;
        this.route = route;
        this.backtrack = backtrack;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (route == null || route.waypoints == null || route.waypoints.isEmpty())
            return Results.ERROR("No route or waypoints defined.");
        
        for (RoutePoint rp : route.waypoints) {
            Coord2d target = rp.toCoord2d(gui.map.glob.map);
            if (target == null)
                continue;

            new PathFinder(target).run(gui);

            action.run(gui);
        }

        if (backtrack) {
            Coord2d start = route.waypoints.get(0).toCoord2d(gui.map.glob.map);
            if (start != null)
                new PathFinder(start).run(gui);
        }

        return Results.SUCCESS();
    }
}
