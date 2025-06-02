package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import static nurgling.NUtils.player;

public class RouteAutoRecorder implements Runnable {
    private final Route route;
    private boolean running = true;
    private final double interval = 77.0;
    private final GateDetector gateDetector;

    public RouteAutoRecorder(Route route) {
        this.route = route;
        this.gateDetector = new GateDetector();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        Coord2d playerRC = player().rc;

        // add waypoint where the recording started
        route.addWaypoint();

        while (running) {
            try {
                NUtils.getUI().core.addTask(new WaitNextPointForRouteAutoRecorder(playerRC, interval, this.route));
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
                playerRC = null;
            }

            // get the hash of the last clicked gob (door, minehole, ladder, cellar, stairs, gate)
            String hash = route.lastAction != null ? route.lastAction.gob.ngob.hash : null;
            String name = route.lastAction != null ? route.lastAction.gob.ngob.name : null;
            Gob gobForCachedRoutePoint = route.lastAction != null ? route.lastAction.gob : null;


            // Handle gate detection and waypoint creation
            if(route.hasPassedGate) {
                // We've passed through a gate, add both waypoints
                if (route.cachedRoutePoint != null) {
                    // Calculate position for the point before the gate
                    Coord tilec = player().rc.div(MCache.tilesz).floor();
                    MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);
                    Coord playerLocalCoord = tilec.sub(grid.ul);
                    Coord preRecordedCoord = route.cachedRoutePoint.localCoord;

                    // based on position of the player and preRecordedCoords we figure out which direction to offset
                    // the preRecordedCoord. The reason for this is that the point gets recorded right on top of the
                    // gate.
                    Coord newCoordForAfterGate = preRecordedCoord.add((playerLocalCoord.x > preRecordedCoord.x ? -1 : playerLocalCoord.x < preRecordedCoord.x ? 1 : 0), (playerLocalCoord.y > preRecordedCoord.y ? -1 : playerLocalCoord.y < preRecordedCoord.y ? 1 : 0));

                    // Update the coords
                    route.cachedRoutePoint.setLocalCoord(newCoordForAfterGate);

                    // Add the waypoint.
                    route.addPredefinedWaypoint(route.cachedRoutePoint, "", "", false);

                    route.addWaypoint();
                    // Get the last two waypoints (one before gate, one after)
                    RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                    RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);

                    // Add connections between them through the gate we passed
                    if(route.lastPassedGate != null) {
                        lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id),
                                route.lastPassedGate.ngob.hash, route.lastPassedGate.ngob.name, true);
                        newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id),
                                route.lastPassedGate.ngob.hash, route.lastPassedGate.ngob.name, true);
                    }

                    // Clear the cached point
                    route.cachedRoutePoint = null;
                    route.hasPassedGate = false;
                    route.lastPassedGate = null;
                    continue;
                }
            } else if(gateDetector.isNearGate()) {
                continue;
            } else if(gob == null && !GateDetector.isLastActionNonLoadingDoor()) {
                // Handle loading doors
                try {
                    NUtils.getUI().core.addTask(new WaitForNoGobWithHash(hash));
                    NUtils.getUI().core.addTask(new WaitForMapLoadNoCoord(NUtils.getGameUI()));

                    Gob player = NUtils.player();
                    Coord2d rc = player.rc;

                    // Create a temporary waypoint to get its hash
                    RoutePoint predefinedWaypoint = new RoutePoint(rc, NUtils.getGameUI().ui.sess.glob.map);

                    Gob arch = Finder.findGob(player().rc, new NAlias(
                            GateDetector.getDoorPair(gobForCachedRoutePoint.ngob.name)
                    ), null, 100);

                    // For the minehole we have to add an offset, otherwise the minehole point gets created right on
                    // top of the minehole causing it to be unreachable with PF.
                    if(arch != null) {
                        if(arch.ngob.name.equals("gfx/terobjs/minehole")) {
                            double angle = arch.a;
                            double offset = 2;

                            Coord tilec = rc.div(MCache.tilesz).floor();
                            MCache.Grid grid = NUtils.getGameUI().ui.sess.glob.map.getgridt(tilec);

                            Coord mineLocalCoord = tilec.sub(grid.ul);

                            Coord newPosition = new Coord(
                                    (int)Math.round(mineLocalCoord.x + Math.cos(angle) * offset),
                                    (int)Math.round(mineLocalCoord.y +  Math.sin(angle) * offset)
                            );

                            predefinedWaypoint.setLocalCoord(newPosition);
                        }
                    }

                    RouteGraph graph = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph();

                    if(graph.points.containsKey(predefinedWaypoint.id)) {
                        predefinedWaypoint = graph.points.get(predefinedWaypoint.id);
                    }

                    // Completely new door
                    if(!graph.getDoors().containsKey(hash) && !graph.getDoors().containsKey(arch.ngob.hash)) {
                        // Add new waypoint
                        route.addPredefinedWaypointNoConnections(predefinedWaypoint);

                        // Get the last two waypoints
                        RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                        RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);

                        // Add connections between them
                        lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);
                        // Add connection for the arch
                        newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), arch.ngob.hash, arch.ngob.name, true);
                    } else if (graph.getDoors().containsKey(hash) && graph.getDoors().containsKey(arch.ngob.hash)) {
                        // Already existing door with less than 2 elements in the route. We've just started recording
                        // before the door and entered the door. We need to simply swap points to existing points.
                        if (route.waypoints.size() <= 1) {

                            // We need to make sure that the outside point is not an actual door that is stored in the
                            // graph. If it is we cannot use delete and have to simply swap. If it's a new
                            // point that does not exist in graph doors we can safely delete it.
                            boolean needToDeleteLastPoint = true;

                            RoutePoint veryLastPointBeforeEnteringTheDoor = route.waypoints.get(route.waypoints.size() - 1);

                            for(RoutePoint routePoint : graph.getDoors().values()) {
                                if (routePoint.id == veryLastPointBeforeEnteringTheDoor.id) {
                                    needToDeleteLastPoint = false;
                                    break;
                                }
                            }

                            if(needToDeleteLastPoint) {
                                route.deleteWaypoint(route.waypoints.get(route.waypoints.size() - 1));

                                RoutePoint firstPointToAdd = graph.getDoors().get(hash);
                                RoutePoint secondPointToadd = graph.getDoors().get(arch.ngob.hash);

                                route.addPredefinedWaypointNoConnections(firstPointToAdd);
                                route.addPredefinedWaypointNoConnections(secondPointToadd);



                                firstPointToAdd.addConnection(secondPointToadd.id, String.valueOf(secondPointToadd.id), hash, name, true);
                                secondPointToadd.addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), arch.ngob.hash, arch.ngob.name, true);
                            } else {
                                RoutePoint existingOutsideRoutePoint = route.waypoints.get(route.waypoints.size() - 1);
                                RoutePoint secondPointToAdd = graph.getDoors().get(arch.ngob.hash);

                                route.addPredefinedWaypointNoConnections(secondPointToAdd);

                                existingOutsideRoutePoint.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(existingOutsideRoutePoint.id, String.valueOf(existingOutsideRoutePoint.id), arch.ngob.hash, arch.ngob.name, true);
                            }
                        } else {
                            // Already existing door with more than 2 elements in the route. We've started recording
                            // more than 1 point before the door so we have to swap the points but also connect the
                            // outside point to the rest of the route

                            // We need to make sure that the outside point is not an actual door that is stored in the
                            // graph. If it is we cannot use delete and have to simply swap. If it's a new
                            // point that does not exist in graph doors we can safely delete it.
                            boolean needToDeleteLastPoint = true;

                            RoutePoint veryLastPointBeforeEnteringTheDoor = route.waypoints.get(route.waypoints.size() - 1);

                            for(RoutePoint routePoint : graph.getDoors().values()) {
                                if (routePoint.id == veryLastPointBeforeEnteringTheDoor.id) {
                                    needToDeleteLastPoint = false;
                                    break;
                                }
                            }

                            if(needToDeleteLastPoint) {
                                route.deleteWaypoint(route.waypoints.get(route.waypoints.size() - 1));

                                RoutePoint firstPointToAdd = graph.getDoors().get(hash);
                                RoutePoint secondPointToAdd = graph.getDoors().get(arch.ngob.hash);

                                if(!route.waypoints.get(route.waypoints.size() - 1).connections.keySet().stream().toList().contains(firstPointToAdd.id)) {
                                    route.waypoints.get(route.waypoints.size() - 1).addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), "", "", false);
                                }

                                if(!firstPointToAdd.connections.keySet().stream().toList().contains(route.waypoints.get(route.waypoints.size() - 2).id)) {
                                    firstPointToAdd.addConnection(route.waypoints.get(route.waypoints.size() - 3).id, String.valueOf(route.waypoints.get(route.waypoints.size() - 3).id), "", "", false);
                                }

                                route.addPredefinedWaypointNoConnections(firstPointToAdd);
                                route.addPredefinedWaypointNoConnections(secondPointToAdd);

                                firstPointToAdd.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), arch.ngob.hash, arch.ngob.name, true);
                            } else {
                                RoutePoint existingOutsideRoutePoint = route.waypoints.get(route.waypoints.size() - 1);
                                RoutePoint secondPointToAdd = graph.getDoors().get(arch.ngob.hash);

                                route.addPredefinedWaypointNoConnections(secondPointToAdd);

                                existingOutsideRoutePoint.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(existingOutsideRoutePoint.id, String.valueOf(existingOutsideRoutePoint.id), arch.ngob.hash, arch.ngob.name, true);
                            }
                        }
                    } else if (graph.getDoors().containsKey(hash)) {
                        // Entering a new door right after an existing door. We need to swap out the outside
                        // door and create a new door point on the inside. We then connect the points the same way we
                        // always do.
                        if (route.waypoints.size() <= 1) {
                            boolean needToDeleteLastPoint = true;

                            RoutePoint veryLastPointBeforeEnteringTheDoor = route.waypoints.get(route.waypoints.size() - 1);

                            for(RoutePoint routePoint : graph.getDoors().values()) {
                                if (routePoint.id == veryLastPointBeforeEnteringTheDoor.id) {
                                    needToDeleteLastPoint = false;
                                    break;
                                }
                            }

                            if(needToDeleteLastPoint) {
                                route.deleteWaypoint(route.waypoints.get(route.waypoints.size() - 1));

                                RoutePoint firstPointToAdd = graph.getDoors().get(hash);
                                RoutePoint secondPointToAdd = predefinedWaypoint;

                                if(!route.waypoints.get(route.waypoints.size() - 1).connections.keySet().stream().toList().contains(firstPointToAdd.id)) {
                                    route.waypoints.get(route.waypoints.size() - 1).addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), "", "", false);
                                }

                                if(!firstPointToAdd.connections.keySet().stream().toList().contains(route.waypoints.get(route.waypoints.size() - 2).id)) {
                                    firstPointToAdd.addConnection(route.waypoints.get(route.waypoints.size() - 3).id, String.valueOf(route.waypoints.get(route.waypoints.size() - 3).id), "", "", false);
                                }

                                route.addPredefinedWaypointNoConnections(firstPointToAdd);
                                route.addPredefinedWaypointNoConnections(secondPointToAdd);


                                firstPointToAdd.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), arch.ngob.hash, arch.ngob.name, true);
                            } else {
                                RoutePoint secondPointToAdd = predefinedWaypoint;

                                route.addPredefinedWaypointNoConnections(secondPointToAdd);

                                predefinedWaypoint.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(graph.getDoors().get(hash).id, String.valueOf(graph.getDoors().get(hash).id), arch.ngob.hash, arch.ngob.name, true);
                            }
                        } else {

                            boolean needToDeleteLastPoint = true;

                            RoutePoint veryLastPointBeforeEnteringTheDoor = route.waypoints.get(route.waypoints.size() - 1);

                            for(RoutePoint routePoint : graph.getDoors().values()) {
                                if (routePoint.id == veryLastPointBeforeEnteringTheDoor.id) {
                                    needToDeleteLastPoint = false;
                                    break;
                                }
                            }

                            if(needToDeleteLastPoint) {
                                route.deleteWaypoint(route.waypoints.get(route.waypoints.size() - 1));

                                RoutePoint firstPointToAdd = graph.getDoors().get(hash);
                                RoutePoint secondPointToAdd = predefinedWaypoint;

                                if(!route.waypoints.get(route.waypoints.size() - 1).connections.keySet().stream().toList().contains(firstPointToAdd.id)) {
                                    route.waypoints.get(route.waypoints.size() - 1).addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), "", "", false);
                                }

                                if(!firstPointToAdd.connections.keySet().stream().toList().contains(route.waypoints.get(route.waypoints.size() - 1).id)) {
                                    firstPointToAdd.addConnection(route.waypoints.get(route.waypoints.size() - 2).id, String.valueOf(route.waypoints.get(route.waypoints.size() - 3).id), "", "", false);
                                }

                                route.addPredefinedWaypointNoConnections(firstPointToAdd);
                                route.addPredefinedWaypointNoConnections(secondPointToAdd);

                                firstPointToAdd.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(firstPointToAdd.id, String.valueOf(firstPointToAdd.id), arch.ngob.hash, arch.ngob.name, true);
                            } else {
                                RoutePoint existingOutsideRoutePoint = route.waypoints.get(route.waypoints.size() - 1);
                                RoutePoint secondPointToAdd = predefinedWaypoint;

                                route.addPredefinedWaypointNoConnections(secondPointToAdd);

                                existingOutsideRoutePoint.addConnection(secondPointToAdd.id, String.valueOf(secondPointToAdd.id), hash, name, true);
                                secondPointToAdd.addConnection(existingOutsideRoutePoint.id, String.valueOf(existingOutsideRoutePoint.id), arch.ngob.hash, arch.ngob.name, true);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (GateDetector.isLastActionNonLoadingDoor()) {
                // Handle non-loading doors
                try {
                    NUtils.getUI().core.addTask(new WaitForDoorGob());

                    // Add new waypoint
                    route.addWaypoint();

                    // Get the last two waypoints
                    RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                    RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);

                    // Add connections between them
                    lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);

                    Gob arch = Finder.findGob(player().rc, new NAlias(
                            GateDetector.getDoorPair(gobForCachedRoutePoint.ngob.name)
                    ), null, 100);

                    // Add connection for the arch
                    newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), arch.ngob.hash, arch.ngob.name, true);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Regular distance-based waypoint
                route.addWaypoint();
            }

            route.lastAction = null;
        }
    }
}

