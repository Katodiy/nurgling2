package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import nurgling.NUtils;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tasks.WaitDistance;
import nurgling.tasks.WaitForDoorGob;
import nurgling.tasks.WaitForMapLoadNoCoord;
import nurgling.tasks.WaitForNoGobWithHash;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.Arrays;
import java.util.List;

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
            Coord2d playerRC = NUtils.player().rc;

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
                if(NUtils.player() != null) {
                    playerRC = NUtils.player().rc;

                    gob = Finder.findGob(NUtils.player().ngob.hash);
                } else {
                    playerRC = null;
                }

                // if player gob is not found that means we've entered a new map
                if (gob == null && !isNonLoadingDoor()) {
                    // get the hash of the last clicked gob (door, minehole, ladder)
                    String hash = NUtils.getUI().core.getLastActions().gob.ngob.hash;
                    String name = NUtils.getUI().core.getLastActions().gob.ngob.name;
                    Gob gobForCachedRoutePoint = NUtils.getUI().core.getLastActions().gob;

                    System.out.println(hash);
                    System.out.println(name);

                    try {
                        NUtils.getUI().core.addTask(new WaitForNoGobWithHash(hash));
                        NUtils.getUI().core.addTask(new WaitForMapLoadNoCoord(NUtils.getGameUI()));

                        // Add new waypoint
                        route.addWaypoint();
                        
                        // Get the last two waypoints
                        RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                        RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);
                        
                        // Add connections between them
                        lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);

                        Gob arch = Finder.findGob(NUtils.player().rc, new NAlias(
                                getPair(gobForCachedRoutePoint.ngob.name)
                                ), null, 100);

                        // Add connection for the arch
                        newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), arch.ngob.hash, arch.ngob.name, true);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else if(isNonLoadingDoor()) {
                    // get the hash of the last clicked gob (door, minehole, ladder)
                    String hash = NUtils.getUI().core.getLastActions().gob.ngob.hash;
                    String name = NUtils.getUI().core.getLastActions().gob.ngob.name;
                    Gob gobForCachedRoutePoint = NUtils.getUI().core.getLastActions().gob;
                    System.out.println(hash);
                    System.out.println(name);

                    try {
                        NUtils.getUI().core.addTask(new WaitForNoGobWithHash(hash));
                        NUtils.getUI().core.addTask(new WaitForDoorGob());

                        // Add new waypoint
                        route.addWaypoint();
                        
                        // Get the last two waypoints
                        RoutePoint lastWaypoint = route.waypoints.get(route.waypoints.size() - 2);
                        RoutePoint newWaypoint = route.waypoints.get(route.waypoints.size() - 1);
                        
                        // Add connections between them
                        lastWaypoint.addConnection(newWaypoint.id, String.valueOf(newWaypoint.id), hash, name, true);

                        Gob arch = Finder.findGob(NUtils.player().rc, new NAlias(
                                getPair(gobForCachedRoutePoint.ngob.name)
                        ), null, 50);

                        // Add connection for the arch
                        newWaypoint.addConnection(lastWaypoint.id, String.valueOf(lastWaypoint.id), arch.ngob.hash, arch.ngob.name, true);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    route.addWaypoint();
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

