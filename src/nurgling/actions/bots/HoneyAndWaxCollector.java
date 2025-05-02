package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitItemInEquip;
import nurgling.tasks.WaitPose;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;

import static nurgling.NUtils.getGameUI;

public class HoneyAndWaxCollector implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean needToFindBucket = false;

        // Check if bucket is equipped or in inventory
        if (!hasBucketEquipped()) {
            if (hasBucketInInventory()) {
                new EquipFromInventory(new NAlias("bucket")).run(gui);
            } else {
                needToFindBucket = true;
            }
        }

        // If no bucket, fetch one from container
        if (needToFindBucket) {
            Container bucketContainer = findContainer(NArea.findOut("Bucket", 1));
            if (bucketContainer == null)
                return fail("No bucket container found!");

            takeBucketFromContainer(bucketContainer, gui);
        }

        // Find Honey Barrel
        Gob honeyBarrel = findBarrel(NArea.findSpec(new NArea.Specialisation(Specialisation.SpecName.barrel.toString(), "Honey")));
        if (honeyBarrel == null)
            return fail("No honey barrel found!");

        // Find Wax Container
        Container waxContainer = findContainer(NArea.findOut("Beeswax", 1));
        if (waxContainer == null)
            return fail("No wax container found!");

        // Find Bee Skeps
        ArrayList<Gob> beeSkeps = Finder.findGobs(new NAlias("gfx/terobjs/beehive"));
        if (beeSkeps.isEmpty())
            return fail("No bee skeps found!");

        List<Gob> honeyAndWaxSkeps = NUtils.sortByNearest(
                beeSkeps.stream().filter(this::hasHoneyOrWax).toList(),
                NUtils.player().rc
        );

        if (honeyAndWaxSkeps.isEmpty()) {
            getGameUI().msg("No bee skeps containing wax or honey found!");
            return Results.SUCCESS();
        }

        for (Gob skep : honeyAndWaxSkeps) {
            if (!ensureBucketEquipped()) return Results.FAIL();

            if (hasWax(skep)) harvestWax(skep, gui);
            if (hasHoney(skep)) useBucket(skep, gui);

            if (isInventoryFull())
                dropOffWax(waxContainer, gui);

            if (hasHoney(skep)) {
                useBucket(honeyBarrel, gui);
                useBucket(skep, gui);
            }
            if (hasWax(skep))
                harvestWax(skep, gui);

            NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
        }

        dropOffWax(waxContainer, gui);
        useBucket(honeyBarrel, gui);
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
        Thread.sleep(500);
        NUtils.activateItem(target);
        Thread.sleep(500);

        NUtils.getEquipment().wdgmsg("drop", -1);
        NUtils.getUI().core.addTask(new WaitItemInEquip(bucket, new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}));
    }

    private void harvestWax(Gob skep, NGameUI gui) throws InterruptedException {
        new PathFinder(skep).run(gui);
        NUtils.activateGob(skep);
        Thread.sleep(400);
        new SelectFlowerAction("Harvest wax", skep).run(gui);
        Thread.sleep(500);
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
}
