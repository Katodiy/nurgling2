package nurgling.routes;

import nurgling.NUtils;

import java.util.*;

public class RouteGraphManager {
    private final RouteGraph graph;
    private final Map<Integer, Route> routes = new HashMap<>();
    private boolean needsUpdate = false;

    public RouteGraphManager() {
        graph = new RouteGraph();
        updateGraph();
    }

    public void updateRoute(Route route) {
        routes.put(route.id, route);
        needsUpdate = true;
    }

    public void updateGraph() {
        if (!needsUpdate) return;

        graph.clear();

        for (Route route : NUtils.getGameUI().routesWidget.routes.values()) {
            graph.addRoute(route);
        }

        needsUpdate = false;
    }

    public RouteGraph getGraph() {
        if (needsUpdate) {
            updateGraph();
        }
        return graph;
    }
} 