package nurgling.actions.bots;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
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
    Container waxContainer = null;
    Gob honeyBarrel = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean needToFindBucket = false;


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

        return Results.SUCCESS();

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
            return new Container(gob,Context.contcaps.get(gob.ngob.name));
        }
        return null;
    }

    private void takeBucketFromContainer(Container container, NGameUI gui) throws InterruptedException {
        new PathFinder(Finder.findGob(container.gobid)).run(gui);
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
