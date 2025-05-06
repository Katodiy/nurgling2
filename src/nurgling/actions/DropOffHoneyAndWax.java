package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.WaitBucketInHandContentQuantityChange;
import nurgling.tasks.WaitItemInEquip;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.Container;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;

import static nurgling.NUtils.getGameUI;

public class DropOffHoneyAndWax implements Action {
    private Container waxContainer;
    private Gob honeyBarrel;
    private boolean isStoreBucket = false;

    public DropOffHoneyAndWax(Container waxContainer, Gob honeyBarrel, boolean isStoreBucket) {
        this.waxContainer = waxContainer;
        this.honeyBarrel = honeyBarrel;
        this.isStoreBucket = isStoreBucket;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (waxContainer != null)
            dropOffWax(waxContainer, gui);

        if (honeyBarrel != null)
            useBucket(honeyBarrel, gui);

        if(isStoreBucket) {
            storeBucketBack(gui);
        }

        return Results.SUCCESS();
    }

    private void dropOffWax(Container container, NGameUI gui) throws InterruptedException {
        new PathFinder(container.gob).run(gui);
        new OpenTargetContainer(container).run(gui);
        new SimpleTransferToContainer(gui.getInventory(container.cap), gui.getInventory().getItems(new NAlias("Beeswax"))).run(gui);
        new SimpleTransferToContainer(gui.getInventory(container.cap), gui.getInventory().getItems(new NAlias("Bee Larvae"))).run(gui);
        new CloseTargetContainer(container).run(gui);
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
}
