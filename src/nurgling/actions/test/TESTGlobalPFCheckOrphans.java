package nurgling.actions.test;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.routes.Route;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * An action that scans the global RouteGraph for "orphan" references.
 *
 * <p>An orphan is a neighbor or connection reference from a RoutePoint to an ID
 * that does not exist in the current graph. This check helps identify broken or
 * stale links that can lead to pathfinding errors or data inconsistencies.
 *
 * <p>When run, the bot will display a message in the in-game chat window
 * listing any orphan IDs it found, or a success message if none are found.
 */
public class TESTGlobalPFCheckOrphans implements Action {

    /**
     * Scans all route points in the current RouteGraph, checking for references
     * to neighbor or connection IDs that do not exist in the graph.
     *
     * @param gui The game UI instance for displaying results.
     * @return Results.SUCCESS() when the check completes.
     * @throws InterruptedException if interrupted during operation.
     */
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        File f = new File(nurgling.NConfig.current.path_routes);
        Set<Integer> trueDuplicates = new HashSet<>();
        Map<Integer, String> idToConnections = new HashMap<>();
        Map<Integer, String> idToNeighbors = new HashMap<>();

        if (f.exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try {
                List<String> lines = Files.readAllLines(Paths.get(NConfig.current.path_routes), StandardCharsets.UTF_8);
                for (String s : lines) {
                    contentBuilder.append(s).append("\n");
                }
            } catch (Exception ignore) {}

            if (!contentBuilder.toString().isEmpty()) {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray routesArray = main.getJSONArray("routes");
                for (int i = 0; i < routesArray.length(); i++) {
                    JSONObject routeObj = routesArray.getJSONObject(i);
                    if (!routeObj.has("waypoints")) continue;
                    JSONArray waypoints = routeObj.getJSONArray("waypoints");
                    for (int j = 0; j < waypoints.length(); j++) {
                        JSONObject rp = waypoints.getJSONObject(j);
                        int id = rp.getInt("id");
                        String conns = rp.opt("connections") == null ? "" : rp.get("connections").toString();
                        String neighs = rp.opt("neighbors") == null ? "" : rp.get("neighbors").toString();
                        boolean duplicate = false;
                        if (idToConnections.containsKey(id)) {
                            if (!idToConnections.get(id).equals(conns) || !idToNeighbors.get(id).equals(neighs)) {
                                trueDuplicates.add(id);
                                duplicate = true;
                            }
                        }
                        if (!duplicate) {
                            idToConnections.put(id, conns);
                            idToNeighbors.put(id, neighs);
                        }
                    }
                }
            }
        }

        if (trueDuplicates.isEmpty()) {
            gui.msg("No duplicate RoutePoints with same id but different connections/neighbors found in JSON.");
        } else {
            gui.msg("Duplicate RoutePoint ids with different connections/neighbors in JSON: " + trueDuplicates);
        }

        RouteGraph graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();

        Set<Integer> allIds = new HashSet<>();
        Set<Integer> orphans = new HashSet<>();

        // Collect all IDs from all points in the graph
        for (RoutePoint rp : graph.points.values()) {
            allIds.add(rp.id);
        }

        // Check all neighbors and connections for orphans
        for (RoutePoint rp : graph.points.values()) {
            // Neighbors
            List<Integer> neighbors = rp.getNeighbors();
            if (neighbors != null) {
                for (int neighborId : neighbors) {
                    if (!allIds.contains(neighborId)) {
                        orphans.add(neighborId);
                    }
                }
            }

            // Connections
            if (rp.connections != null) {
                for (Integer connId : rp.connections.keySet()) {
                    if (!allIds.contains(connId)) {
                        orphans.add(connId);
                    }
                }
            }
        }

        if (orphans.isEmpty()) {
            gui.msg("No orphan route point IDs found!");
        } else {
            gui.msg("Orphan route point IDs found: " + orphans);
        }

        return Results.SUCCESS();
    }
}
