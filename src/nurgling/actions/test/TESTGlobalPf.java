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

public class TESTGlobalPf implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        RouteGraph graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();

        while(true) {
            RoutePoint randomRoutePoint = getRandomRoutePoint(graph);
            gui.msg(String.format("Testing finding path to %s", randomRoutePoint.id));
            new RoutePointNavigator(randomRoutePoint).run(NUtils.getGameUI());
        }
    }

    private RoutePoint getRandomRoutePoint(RouteGraph graph) {
        Random random = new Random();

        List<RoutePoint> pointsList = new ArrayList<>(graph.points.values());

        int randomIndex = random.nextInt(pointsList.size());

        return pointsList.get(randomIndex);
    }
}
