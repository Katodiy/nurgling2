package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RouteEditor;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static nurgling.NUtils.player;

public class RouteAutoRecorder implements Runnable {
    private final Route route;
    private final RouteEditor routeEditor;
    private boolean running = true;
    private final GateDetector gateDetector;

    public RouteAutoRecorder(Route route) {
        this.route = route;
        this.routeEditor = new RouteEditor(((NMapView) NUtils.getGameUI().map).routeGraphManager);
        this.gateDetector = new GateDetector();
    }

    public void stop() {
        running = false;
    }

    /**
     * Main recording loop for automatic route creation.
     *
     * <p>This method monitors player movement, adds regular waypoints, and handles
     * special cases such as passing through gates or doors. It delegates all
     * scenario-specific logic to private helper methods, keeping the main loop readable.
     *
     * <p>The loop terminates if recording is stopped or an interrupt is detected.
     */
    @Override
    public void run() {
        Coord2d playerRC = player().rc;

        // add waypoint where the recording started
        route.addWaypoint();

        while (running) {
            // Wait for player to move to next point (or until interrupted)
            try {
                NUtils.getUI().core.addTask(new WaitNextPointForRouteAutoRecorder(playerRC, this.route));
            } catch (InterruptedException e) {
                NUtils.getGameUI().msg("Stopped route recording for: " + route.name);
                running = false;
            }

            if (!running) break;

            Gob gob = null;

            // update player RC to current
            Gob playerGob = player();
            if(playerGob != null) {
                playerRC = playerGob.rc;
                gob = Finder.findGob(playerGob.ngob.hash);
            } else {
                try {
                    NUtils.getUI().core.addTask(new WaitPlayerNotNull());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                playerRC = player().rc;
            }

            // get the hash of the last clicked gob (door, minehole, ladder, cellar, stairs, gate)
            String lastClickedGobHash = route.lastAction != null ? route.lastAction.gob.ngob.hash : null;
            String lastClickedGobName = route.lastAction != null ? route.lastAction.gob.ngob.name : null;
            Gob gobForCachedRoutePoint = route.lastAction != null ? route.lastAction.gob : null;


            // Handle scenario: player has just passed through a gate
            if(route.hasPassedGate) {
                handleGatePassed();
                continue;

            // Handle scenario: player is near a gate (wait, do not record a waypoint yet)
            } else if(gateDetector.isNearGate()) {
                continue;

            // Handle all door transitions (loading, non-loading, new, existing, etc.)
            } else if((gob == null && !GateDetector.isLastActionNonLoadingDoor()) || GateDetector.isLastActionNonLoadingDoor()) {
                try {
                    if(!GateDetector.isLastActionNonLoadingDoor()) {
                        NUtils.getUI().core.addTask(new WaitForNoGobWithHash(lastClickedGobHash));
                        NUtils.getUI().core.addTask(new WaitForMapLoadNoCoord(NUtils.getGameUI()));
                    } else if (GateDetector.isLastActionNonLoadingDoor()) {
                        NUtils.getUI().core.addTask(new WaitForDoorGob());
                    }

                    Gob player = NUtils.player();
                    Coord2d rc = player.rc;

                    // Create a temporary waypoint to get its hash
                    RoutePoint predefinedWaypoint = new RoutePoint(rc, NUtils.getGameUI().ui.sess.glob.map);

                    Gob archGob = Finder.findGob(player().rc, new NAlias(
                            GateDetector.getDoorPair(gobForCachedRoutePoint.ngob.name)
                    ), null, 100);

                    // For the mine hole we have to add an offset, otherwise the mine hole point gets created right on
                    // top of the mine hole causing it to be unreachable with PF.
                    Coord tilec = rc.div(MCache.tilesz).floor();
                    MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);
                    Coord mineLocalCoord = tilec.sub(grid.ul);
                    routeEditor.applyWaypointOffset(predefinedWaypoint, archGob.ngob.name, grid.id, mineLocalCoord, archGob.a);

                    RouteGraph graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();

                    if(graph.points.containsKey(predefinedWaypoint.id)) {
                        predefinedWaypoint = graph.points.get(predefinedWaypoint.id);
                    }

                    // Completely new door
                    if(!graph.getDoors().containsKey(lastClickedGobHash) && !graph.getDoors().containsKey(archGob.ngob.hash)) {
                        handleCompletelyNewDoor(graph, predefinedWaypoint, lastClickedGobHash, lastClickedGobName, archGob);

                    // Already existing door. We need to simply swap points to existing points.
                    } else if (graph.getDoors().containsKey(lastClickedGobHash) && graph.getDoors().containsKey(archGob.ngob.hash)) {

                        handleExistingDoor(graph, lastClickedGobHash, lastClickedGobName, archGob);

                    // Entering a new door right after an existing door. We need to swap out the outside
                    // door and create a new door point on the inside. We then connect the points the same way we
                    // always do.
                    } else if (graph.getDoors().containsKey(lastClickedGobHash)) {
                        handleEnteringNewDoorAfterExisting(graph, predefinedWaypoint, lastClickedGobHash, lastClickedGobName, archGob, rc);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            // Handle regular waypoint recording for non-special movement
            } else {
                if(NUtils.player() != null && NUtils.player().rc != null) {
                    route.addWaypoint();
                }
            }

            route.lastAction = null;
        }
    }

    /**
     * Handles the event where the player has passed through a gate.
     *
     * - Adjusts the position of the cached route point based on movement direction.
     * - Adds waypoints for both sides of the gate.
     * - Connects the two waypoints with a door connection.
     * - Clears all temporary gate-related state.
     *
     * Preconditions: route.cachedRoutePoint is not null.
     * Side effects: Modifies route waypoints and gate-related fields.
     */
    private void handleGatePassed() {
        if (route.cachedRoutePoint == null)
            return;
        // Calculate position for the point before the gate
        Coord tilec = player().rc.div(MCache.tilesz).floor();
        MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);
        Coord playerLocalCoord = tilec.sub(grid.ul);
        Coord preRecordedCoord = route.cachedRoutePoint.localCoord;

        // Offset to avoid overlapping gate
        Coord newCoordForAfterGate = preRecordedCoord.add(
                (playerLocalCoord.x > preRecordedCoord.x ? -1 : playerLocalCoord.x < preRecordedCoord.x ? 1 : 0),
                (playerLocalCoord.y > preRecordedCoord.y ? -1 : playerLocalCoord.y < preRecordedCoord.y ? 1 : 0)
        );
        route.cachedRoutePoint.setLocalCoord(newCoordForAfterGate);

        // Add the waypoint before the gate
        route.addPredefinedWaypoint(route.cachedRoutePoint, "", "", false);
        // Add waypoint after the gate
        route.addWaypoint();

        // Connect the two waypoints with a door connection
        RoutePoint lastWaypoint = route.getSecondToLastWaypoint();
        RoutePoint newWaypoint = route.getLastWaypoint();
        if (route.lastPassedGate != null) {
            lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id),
                    route.lastPassedGate.ngob.hash, route.lastPassedGate.ngob.name, true);
            newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id),
                    route.lastPassedGate.ngob.hash, route.lastPassedGate.ngob.name, true);
        }

        // Clear gate-related state
        route.cachedRoutePoint = null;
        route.hasPassedGate = false;
        route.lastPassedGate = null;
    }

    /**
     * Handles the case where both sides of a door are new and not present in the route graph.
     *
     * - Adds a new waypoint for the inside of the door.
     * - Offsets the previous waypoint to prevent overlap with the door.
     * - Updates references and migrates connections/neighbors to avoid duplication.
     * - Creates a bidirectional connection between the new waypoints.
     *
     * @param graph The route graph for reference and modification.
     * @param predefinedWaypoint The waypoint representing the inside of the new door.
     * @param hash The hash of the outside door.
     * @param name The name of the outside door.
     * @param arch The Gob representing the inside door.
     */
    private void handleCompletelyNewDoor(
            RouteGraph graph,
            RoutePoint predefinedWaypoint,
            String hash,
            String name,
            Gob arch
    ) {
        predefinedWaypoint.updateHashCode();
        route.addPredefinedWaypointNoConnections(predefinedWaypoint);

        // Get the last two waypoints
        RoutePoint lastWaypoint = route.getSecondToLastWaypoint();
        RoutePoint newWaypoint = route.getLastWaypoint();

        // Offset the previous waypoint so it is not inside the door
        routeEditor.applyWaypointOffset(lastWaypoint, name, graph.getLastPlayerGridId(), graph.getLastPlayerCoord(), graph.getLastMovementDirection() + Math.PI);

        Collection<RoutePoint.Connection> deletedConnections = route.getSecondToLastWaypoint().getConnections();
        List<Integer> deletedNeighbors = route.getSecondToLastWaypoint().getNeighbors();

        // Prevent duplicate doors by checking if a point already exists at the new position
        int lastWaypointHash = hashCode(lastWaypoint.gridId, lastWaypoint.localCoord);
        if(graph.points.containsKey(lastWaypointHash)) {
            int oldId = lastWaypoint.id;
            int newId = lastWaypointHash;

            if(oldId != newId) {
                ((NMapView) NUtils.getGameUI().map).routeGraphManager.deleteRoutePointFromNeighborsAndConnections(lastWaypoint);
            }

            lastWaypoint = graph.getPoint(newId);

            routeEditor.replaceAllReferences(oldId, newId);
            routeEditor.migrateConnectionsAndNeighbors(deletedConnections, deletedNeighbors, lastWaypoint);
        } else if (graph.points.containsKey(lastWaypoint.id)) {
            lastWaypoint.updateHashCode();
        } else {
            lastWaypoint.updateHashCode();
        }

        routeEditor.addBidirectionalDoorConnection(
                lastWaypoint, newWaypoint,
                hash, name, arch.ngob.hash, arch.ngob.name
        );
    }

    /**
     * Handles traversing an already existing door (both sides are known in the route graph).
     *
     * - Swaps or connects waypoints as needed, based on whether the previous point is a door.
     * - Ensures route continuity when traversing doors encountered previously.
     *
     * @param graph The route graph for lookups.
     * @param hash The hash of the outside door.
     * @param name The name of the outside door.
     * @param arch The Gob representing the inside door.
     */
    private void handleExistingDoor(RouteGraph graph, String hash, String name, Gob arch) {
        boolean needToDeleteLastPoint = shouldDeleteLastWaypoint(route, graph);

        RoutePoint firstPointToAdd = graph.getDoors().get(hash);
        RoutePoint secondPointToAdd = graph.getDoors().get(arch.ngob.hash);

        if (needToDeleteLastPoint) {
            // Delete and replace last waypoint with door points
            routeEditor.deleteAndReplaceLastWaypoint(
                    route,
                    firstPointToAdd,
                    secondPointToAdd,
                    hash, name,
                    arch.ngob.hash, arch.ngob.name
            );
        } else {
            // Add and connect the inside door point to the existing outside
            RoutePoint existingOutsideRoutePoint = route.getLastWaypoint();
            route.addPredefinedWaypointNoConnections(secondPointToAdd);

            routeEditor.addBidirectionalDoorConnection(
                    existingOutsideRoutePoint, secondPointToAdd,
                    hash, name,
                    arch.ngob.hash, arch.ngob.name
            );
        }
    }

    /**
     * Handles the case where the player enters a new door immediately after passing an existing door.
     *
     * - Swaps or adds the new inside waypoint depending on route context.
     * - Applies any special offsets if needed (e.g., for mineholes).
     * - Handles reference updates and bidirectional connections.
     *
     * @param graph The route graph for reference and updates.
     * @param predefinedWaypoint The inside point to be created or adjusted.
     * @param hash The hash of the outside door.
     * @param name The name of the outside door.
     * @param arch The Gob representing the inside door.
     * @param rc The player's current coordinate.
     */
    private void handleEnteringNewDoorAfterExisting(RouteGraph graph, RoutePoint predefinedWaypoint, String hash, String name, Gob arch, Coord2d rc) {
        boolean needToDeleteLastPoint = shouldDeleteLastWaypoint(route, graph);

        if (needToDeleteLastPoint) {
            RoutePoint firstPointToAdd = graph.getDoors().get(hash);
            RoutePoint secondPointToAdd = predefinedWaypoint;
            secondPointToAdd.updateHashCode();

            routeEditor.deleteAndReplaceLastWaypoint(
                    route,
                    firstPointToAdd,
                    secondPointToAdd,
                    hash, name,
                    arch.ngob.hash, arch.ngob.name
            );
        } else {
            RoutePoint existingOutsideRoutePoint = route.getLastWaypoint();
            RoutePoint secondPointToAdd = predefinedWaypoint;

            // Only needed for long routes (more than 1 point before door)
            Coord tilec = rc.div(MCache.tilesz).floor();
            MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);
            Coord mineLocalCoord = tilec.sub(grid.ul);

            routeEditor.applyWaypointOffset(secondPointToAdd, arch.ngob.name, grid.id, mineLocalCoord, arch.a);

            int oldId = secondPointToAdd.id;
            int newId = hashCode(secondPointToAdd.gridId, secondPointToAdd.localCoord);

            if (graph.points.containsKey(newId) && oldId != newId) {
                secondPointToAdd = graph.getPoint(newId);
                routeEditor.replaceAllReferences(oldId, newId);
            } else if (graph.points.containsKey(oldId)) {
                secondPointToAdd = graph.getPoint(oldId);
            } else {
                secondPointToAdd.updateHashCode();
            }

            route.addPredefinedWaypointNoConnections(secondPointToAdd);

            routeEditor.addBidirectionalDoorConnection(
                    existingOutsideRoutePoint, secondPointToAdd,
                    hash, name,
                    arch.ngob.hash, arch.ngob.name
            );
        }
    }

    /**
     * Determines if the last waypoint in the route should be deleted,
     * based on whether it's already registered as a door in the route graph.
     *
     * @param route The current route being recorded.
     * @param graph The route graph for checking door status.
     * @return true if the last waypoint should be deleted; false otherwise.
     */
    private boolean shouldDeleteLastWaypoint(Route route, RouteGraph graph) {
        RoutePoint last = route.getLastWaypoint();
        for (RoutePoint door : graph.getDoors().values()) {
            if (door.id == last.id) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes a unique hash code for a given grid ID and local coordinate.
     *
     * @param gridId The grid identifier.
     * @param localCoord The local coordinate within the grid.
     * @return An integer hash representing the combination of grid and coordinate.
     */
    public int hashCode(long gridId, Coord localCoord) {
        return Objects.hash(gridId, localCoord);
    }
}

