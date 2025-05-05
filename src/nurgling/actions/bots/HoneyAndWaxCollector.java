package nurgling.actions.bots;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.routes.Route;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static nurgling.NUtils.getGameUI;

public class HoneyAndWaxCollector implements Action {

    HashMap<Integer, Route> routes = new HashMap<>();
    Route route = null;
    int currentWaypointIndex = 0;
    int backtrackStartIndex = 0;
    Container waxContainer = null;
    Gob honeyBarrel = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        currentWaypointIndex = 0;
        backtrackStartIndex = 0;
        waxContainer = null;
        honeyBarrel = null;
        boolean needToFindBucket = false;

        loadRoutes();

        for (Route route : routes.values()) {
            if (route.name.contains("bee")) {
                this.route = route;
                break;
            }
        }

        if (this.route == null) {
            return fail("No route found!");
        }

        getGameUI().msg("Found route for bee keeping, going to starting point.");
        goToRoutePoint(this.route.waypoints.get(0));

        // Check if bucket is equipped or in inventory
        if (!hasBucketEquipped()) {
            getGameUI().msg("No bucket equipped!");
            if (hasBucketInInventory()) {
                getGameUI().msg("No bucket in inventory!");
                new EquipFromInventory(new NAlias("bucket")).run(gui);
            } else {
                needToFindBucket = true;
            }
        }

        // If no bucket, fetch one from container
        if (needToFindBucket) {
            getGameUI().msg("Looking for bucket container!");
            Container bucketContainer = findContainer(NArea.findOut("Bucket", 1));
            if (bucketContainer == null)
                return fail("No bucket container found!");

            takeBucketFromContainer(bucketContainer, gui);
        }

        // Find Honey Barrel
        honeyBarrel = findBarrel(NArea.findSpec(new NArea.Specialisation(Specialisation.SpecName.barrel.toString(), "Honey")));
        if (honeyBarrel == null)
            return fail("No honey barrel found!");

        // Find Wax Container
        waxContainer = findContainer(NArea.findOut("Beeswax", 1));
        if (waxContainer == null)
            return fail("No wax container found!");

        getGameUI().msg("Starting the route!");
        for (int i = currentWaypointIndex; i < route.waypoints.size(); i++) {
            currentWaypointIndex = i;
            goToRoutePoint(route.waypoints.get(i));

            // Find Bee Skeps
            ArrayList<Gob> beeSkeps = Finder.findGobs(new NAlias("gfx/terobjs/beehive"));
            if (beeSkeps.isEmpty()) {
                getGameUI().msg("No bee skeps found, continuing to route!");
                continue;
            }

            List<Gob> honeyAndWaxSkeps = NUtils.sortByNearest(
                    beeSkeps.stream().filter(this::hasHoneyOrWax).toList(),
                    NUtils.player().rc
            );

            if (honeyAndWaxSkeps.isEmpty()) {
                getGameUI().msg("No bee skeps containing wax or honey found, continuing to route!");
                continue;
            }

            for (Gob skep : honeyAndWaxSkeps) {
                getGameUI().msg("Found bee skeps containing wax or honey, harvesting!");
                if (!ensureBucketEquipped()) return Results.FAIL();

                if (hasWax(skep)) harvestWax(skep, gui);
                if (hasHoney(skep)) useBucket(skep, gui);

                if (isInventoryFull() || hasHoney(skep)) {
                    backtrackStartIndex = i;
                    performBacktrack(gui, true);
                }

                if (hasWax(skep)) {
                    harvestWax(skep, gui);
                }

                if (hasHoney(skep)) {
                    useBucket(skep, gui);
                }

                NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
            }

        }

        performBacktrack(gui, false);

        return storeBucketBack(gui);
    }

    private boolean hasBucketEquipped() throws InterruptedException {
        return NUtils.getEquipment().findBucket("Empty") != null ||
                NUtils.getEquipment().findBucket("Honey") != null;
    }

    private boolean hasBucketInInventory() throws InterruptedException {
        return getGameUI().getInventory().getItem("bucket") != null;
    }

    private boolean ensureBucketEquipped() throws InterruptedException {
        if (!hasBucketEquipped()) {
            getGameUI().msg("Lost bucket!");
            return false;
        }
        return true;
    }

    private void takeBucketFromContainer(Container container, NGameUI gui) throws InterruptedException {
        new PathFinder(container.gob).run(gui);
        new OpenTargetContainer(container).run(gui);
        new EquipFromInventory(new NAlias("bucket"), gui.getInventory(container.cap)).run(gui);
        new CloseTargetContainer(container).run(gui);
    }

    private Gob findBarrel(NArea area) throws InterruptedException {
        if (area == null) return null;
        ArrayList<Gob> barrels = Finder.findGobs(area, new NAlias("barrel"));
        return barrels.isEmpty() ? null : barrels.get(0);
    }

    private Container findContainer(NArea area) throws InterruptedException {
        if (area == null) return null;
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        for (Gob gob : gobs) {
            Container container = new Container();
            container.gob = gob;
            container.cap = Context.contcaps.get(gob.ngob.name);
            return container;
        }
        return null;
    }

    private void useBucket(Gob target, NGameUI gui) throws InterruptedException {
        new PathFinder(target).run(gui);

        WItem bucket = NUtils.getEquipment().findBucket("Empty");
        if (bucket == null) bucket = NUtils.getEquipment().findBucket("Honey");
        if (bucket == null) return;

        NUtils.takeItemToHand(bucket);
        NUtils.activateItem(target);

        NUtils.getUI().core.addTask(new WaitBucketInHandContentQuantityChange(bucket));

        NUtils.getEquipment().wdgmsg("drop", -1);
        NUtils.getUI().core.addTask(new WaitItemInEquip(bucket, new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}));
    }

    private void harvestWax(Gob skep, NGameUI gui) throws InterruptedException {
        new PathFinder(skep).run(gui);
        new SelectFlowerAction("Harvest wax", skep).run(gui);
        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/bushpickan"));
        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
    }

    private void dropOffWax(Container container, NGameUI gui) throws InterruptedException {
        new PathFinder(container.gob).run(gui);
        new OpenTargetContainer(container).run(gui);
        new SimpleTransferToContainer(gui.getInventory(container.cap), gui.getInventory().getItems(new NAlias("Beeswax"))).run(gui);
        new SimpleTransferToContainer(gui.getInventory(container.cap), gui.getInventory().getItems(new NAlias("Bee Larvae"))).run(gui);
        new CloseTargetContainer(container).run(gui);
    }

    private Results storeBucketBack(NGameUI gui) throws InterruptedException {
        Container bucketContainer = findContainer(NArea.findOut("Bucket", 1));
        if (bucketContainer == null)
            return Results.SUCCESS(); // no container — just finish

        new PathFinder(bucketContainer.gob).run(gui);
        new OpenTargetContainer(bucketContainer).run(gui);

        WItem lhand = NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx);
        if (lhand != null) {
            NUtils.takeItemToHand(lhand);
            gui.getInventory(bucketContainer.cap).dropOn(gui.getInventory(bucketContainer.cap).findFreeCoord(getGameUI().vhand));
        }

        new CloseTargetContainer(bucketContainer).run(gui);
        return Results.SUCCESS();
    }

    private boolean hasHoneyOrWax(Gob skep) {
        long attr = skep.ngob.getModelAttribute();
        return attr == 7 || attr == 6 || attr == 3;
    }

    private boolean hasWax(Gob skep) {
        long attr = skep.ngob.getModelAttribute();
        return attr == 7 || attr == 6;
    }

    private boolean hasHoney(Gob skep) {
        long attr = skep.ngob.getModelAttribute();
        return attr == 7 || attr == 3;
    }

    private boolean isInventoryFull() {
        return getGameUI().getInventory().calcNumberFreeCoord(new Coord(1, 2)) == 0;
    }

    private Results fail(String message) {
        getGameUI().msg(message);
        return Results.FAIL();
    }

    private void loadRoutes() {
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
                    this.routes.put(route.id, route);
                }
            }
        }
    }

    private void goToRoutePoint(RoutePoint routePoint) throws InterruptedException {
        new PathFinder(routePoint.toCoord2d(NUtils.getGameUI().map.glob.map)).run(NUtils.getGameUI());
    }

    private void performBacktrack(NGameUI gui, boolean withReturn) throws InterruptedException {
        getGameUI().msg("Backtracking all the way to start! Returning: " + withReturn);
        // Walk all the way back to route point 0
        for (int i = currentWaypointIndex; i >= 0; i--) {
            goToRoutePoint(route.waypoints.get(i));
        }

        dropOffWax(waxContainer, gui);
        useBucket(honeyBarrel, gui);

        if(withReturn) {
            getGameUI().msg("Returning to farthest visited waypoint.");
            // Return to backtrack start
            for (int i = 1; i <= this.backtrackStartIndex; i++) {
                goToRoutePoint(route.waypoints.get(i));
            }
        }
    }
}
