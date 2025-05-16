package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.Arrays;
import java.util.List;

import static nurgling.NUtils.player;

public class RouteAutoRecorder implements Runnable {
    private final Route route;
    private boolean running = true;
    private final double interval = 77.0;

    public RouteAutoRecorder(Route route) {
        this.route = route;
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
                    NUtils.getUI().core.addTask( new WaitDistance(playerRC, interval, this.route));
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
                String hash = route.lastAction.gob.ngob.hash;
                String name = route.lastAction.gob.ngob.name;
                Gob gobForCachedRoutePoint = route.lastAction.gob;

                // creating one of the doors. Could be a gate, a door or stairs.
                if(veryCloseToAGate() || (gob == null && !isNonLoadingDoor()) || isNonLoadingDoor()) {
                    System.out.println(hash);
                    System.out.println(name);

                    try {
                         if (veryCloseToAGate()) {
                            // gate is closed

                             // 2 - closed; 1 - opened;
                            if(gobForCachedRoutePoint.ngob.getModelAttribute() == 1) {

                                // Wait for gate to be opened
                                NUtils.getUI().core.addTask(new WaitGobModelAttr(gobForCachedRoutePoint, 1));

                                // Wait for gate to be closed
                                NUtils.getUI().core.addTask(new WaitGobModelAttr(gobForCachedRoutePoint, 2));
                            } else {
                                continue;
                            }
                        } else if(gob == null && !isNonLoadingDoor()) {
                            // wait for map to load
                             NUtils.getUI().core.addTask(new WaitForNoGobWithHash(hash));
                            NUtils.getUI().core.addTask(new WaitForMapLoadNoCoord(NUtils.getGameUI()));
                        } else if (isNonLoadingDoor()) {
                            // wait for down/up stairs to show up
                            NUtils.getUI().core.addTask(new WaitForDoorGob());
                        }

                        // Add new waypoint
                        route.addWaypoint();

                        // Get the last two waypoints
                        RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                        RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);

                        if(veryCloseToAGate()) {
                            // Add connections between them
                            lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);

                            // Add connection for the arch
                            newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), hash, name, true);
                        } else {
                            // Add connections between them
                            lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);

                            Gob arch = Finder.findGob(player().rc, new NAlias(
                                    getPair(gobForCachedRoutePoint.ngob.name)
                            ), null, 100);

                            if(arch.ngob.name.equals("gfx/terobjs/minehole")) {
                                double angle = arch.a;
                                double offset = 1;

                                Coord newPosition = new Coord(
                                        (int)Math.round(newWaypoint.localCoord.x + Math.cos(angle) * offset),
                                        (int)Math.round(newWaypoint.localCoord.y +  Math.sin(angle) * offset)
                                );

                                route.setWaypointCoord(newWaypoint, newPosition);
                            }

                            // Add connection for the arch
                            newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), arch.ngob.hash, arch.ngob.name, true);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    route.lastAction = null;
                } else {
                    route.addWaypoint();
                    route.lastAction = null;
                }
            }
    }

    private boolean isNonLoadingDoor() {
        if(NUtils.getUI().core.getLastActions() == null) {
            return false;
        }

        List<String> listOfDoors = Arrays.asList("stairs");
        for (String door : listOfDoors) {
            if(NUtils.getUI().core.getLastActions().gob.ngob.name.contains(door) && !NUtils.getUI().core.getLastActions().gob.ngob.name.contains("cellar")) {
                return true;
            }
        }

        return false;
    }

    private boolean veryCloseToAGate() {
        try {
            String[] gateNames = {"gfx/terobjs/arch/polebiggate", "gfx/terobjs/arch/drystonewallbiggate", "gfx/terobjs/arch/polegate", "gfx/terobjs/arch/drystonewallgate"};
            Gob gate = Finder.findGob(player().rc, new NAlias(gateNames), null, 30);

            if(gate != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPair(String input) {
        String[][] pairs = {
                {"gfx/terobjs/arch/stonestead-door", "gfx/terobjs/arch/stonestead"},
                {"gfx/terobjs/arch/stonemansion-door", "gfx/terobjs/arch/stonemansion"},
                {"gfx/terobjs/arch/greathall-door", "gfx/terobjs/arch/greathall"},
                {"gfx/terobjs/arch/primitivetent-door", "gfx/terobjs/arch/primitivetent"},
                {"gfx/terobjs/arch/windmill-door", "gfx/terobjs/arch/windmill"},
                {"gfx/terobjs/arch/stonetower-door", "gfx/terobjs/arch/stonetower"},
                {"gfx/terobjs/arch/logcabin-door", "gfx/terobjs/arch/logcabin"},
                {"gfx/terobjs/arch/timberhouse-door", "gfx/terobjs/arch/timberhouse"},

                {"gfx/terobjs/minehole", "gfx/terobjs/ladder"},
                {"gfx/terobjs/arch/upstairs", "gfx/terobjs/arch/downstairs"},
                {"gfx/terobjs/arch/cellardoor", "gfx/terobjs/arch/cellarstairs"}
        };

        for (String[] pair : pairs) {
            if (pair[0].equals(input)) return pair[1];
            if (pair[1].equals(input)) return pair[0];
        }

        return null; // Or handle unknown input appropriately
    }
}

