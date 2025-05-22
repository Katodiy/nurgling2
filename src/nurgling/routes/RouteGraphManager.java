package nurgling.routes;

import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class RouteGraphManager {
    private final RouteGraph graph;
    private final Map<Integer, Route> routes = new HashMap<>();
    private boolean needsUpdate = false;

    public RouteGraphManager() {
        graph = new RouteGraph();
        loadRoutes();
        updateGraph();
        NConfig.needRoutesUpdate();
    }

    public void updateRoute(Route route) {
        routes.put(route.id, route);
        needsUpdate = true;
    }

    public void updateGraph() {
        if (!needsUpdate) return;

        graph.clear();

        for (Route route : routes.values()) {
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

    public void loadRoutes() {
        if(new File(NConfig.current.path_routes).exists())
        {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_routes), StandardCharsets.UTF_8))
            {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }
            catch (IOException ignore)
            {
            }

            if (!contentBuilder.toString().isEmpty())
            {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = (JSONArray) main.get("routes");
                for (int i = 0; i < array.length(); i++)
                {
                    Route route = new Route((JSONObject) array.get(i));
                    routes.put(route.id, route);
                }

                // Update the graph after loading routes
                needsUpdate = true;
                updateGraph();
            }
        }
    }

    public Map<Integer, Route> getRoutes() {
        return routes;
    }
} 