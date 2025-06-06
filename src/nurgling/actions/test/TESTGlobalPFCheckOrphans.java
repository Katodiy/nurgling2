package nurgling.actions.test;

import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
