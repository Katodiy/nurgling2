package nurgling.actions.test;

import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An action for testing global pathfinding between the player and random route points.
 *
 * <p>When run, this bot will repeatedly select random RoutePoints from the global RouteGraph
 * and attempt to find a path to each using the RoutePointNavigator. This is useful for
 * stress-testing pathfinding logic and ensuring all points are reachable.
 *
 * <p>Each attempted path is announced in the in-game chat window with the target RoutePoint's ID.
 */
public class TESTGlobalPf implements Action {

    /**
     * Continuously picks a random RoutePoint from the global RouteGraph and tests
     * finding a path to it using RoutePointNavigator. Displays a message in the UI
     * for each target RoutePoint.
     *
     * <p>This method will run indefinitely (intended for stress and robustness testing).
     *
     * @param gui The game UI instance for displaying status messages.
     * @return Results.SUCCESS() (though the loop is infinite unless interrupted).
     * @throws InterruptedException if interrupted during operation.
     */
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        RouteGraph graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();

        while(true) {
            RoutePoint randomRoutePoint = getRandomRoutePoint(graph);
            gui.msg(String.format("Testing finding path to %s", randomRoutePoint.id));
            new RoutePointNavigator(randomRoutePoint).run(NUtils.getGameUI());
        }
    }

    /**
     * Selects a random RoutePoint from the provided RouteGraph.
     *
     * @param graph The RouteGraph containing all available RoutePoints.
     * @return A randomly selected RoutePoint from the graph.
     */
    private RoutePoint getRandomRoutePoint(RouteGraph graph) {
        Random random = new Random();

        List<RoutePoint> pointsList = new ArrayList<>(graph.points.values());

        int randomIndex = random.nextInt(pointsList.size());

        return pointsList.get(randomIndex);
    }
}
