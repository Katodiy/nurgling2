package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.routes.Route;
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

            if (!running) {
                NConfig.needRoutesUpdate();
                break;
            }

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

                    // Add new waypoint
                    route.addPredefinedWaypoint(predefinedWaypoint, "", "", false);

                    // Get the last two waypoints
                    RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                    RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);

                    // Add connections between them
                    lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);

                    // Add connection for the arch
                    newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), arch.ngob.hash, arch.ngob.name, true);
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

