package nurgling.actions.bots;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.routes.Route;
import nurgling.tasks.*;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
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
import java.util.stream.Stream;

import static nurgling.NUtils.getGameUI;


public class HoneyAndWaxCollector implements Action {

    HashMap<Integer, Route> routes = new HashMap<>();
    Route route = null;
    Container waxContainer = null;
    Gob honeyBarrel = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean needToFindBucket = false;

        loadRoutes();

        for (Route route : routes.values()) {
            if (route.hasSpecialization("honey")) {
                this.route = route;
                break;
            }
        }

        if (this.route == null) {
            getGameUI().msg("No honey route found!");
            return Results.FAIL();
        }

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
            Container bucketContainer = findContainer(NContext.findOut("Bucket", 1));
            if (bucketContainer == null) {
                getGameUI().msg("No bucket container found!");
                return Results.FAIL();
            }
            takeBucketFromContainer(bucketContainer, gui);
        }

        // Find Honey Barrel
        honeyBarrel = findBarrel(NContext.findSpec(new NArea.Specialisation(Specialisation.SpecName.barrel.toString(), "Honey")));
        if (honeyBarrel == null) {
            getGameUI().msg("No honey barrel found!");
            return Results.FAIL();
        }

        // Find Wax Container
        waxContainer = findContainer(NContext.findOut("Beeswax", 1));
        if (waxContainer == null) {
            getGameUI().msg("No wax container found!");
            return Results.FAIL();
        }

        Action mainAction = new HarvestBeehivesAction();
        NTask predicate = new InventoryOrBucketFull();
        Action returnAction = new DropOffHoneyAndWax(waxContainer, honeyBarrel, false);
        Action finalAction = new DropOffHoneyAndWax(waxContainer, honeyBarrel, true);

        RouteWorker worker = new RouteWorker(mainAction, this.route, true, predicate, returnAction, finalAction);
        return worker.run(gui);

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

    private boolean hasBucketEquipped() throws InterruptedException {
        return NUtils.getEquipment().findBucket("Empty") != null ||
                NUtils.getEquipment().findBucket("Honey") != null;
    }

    private boolean hasBucketInInventory() throws InterruptedException {
        return getGameUI().getInventory().getItem("bucket") != null;
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
}
