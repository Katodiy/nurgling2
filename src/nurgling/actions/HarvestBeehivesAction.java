package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitBucketInHandContentQuantityChange;
import nurgling.tasks.WaitItemInEquip;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;
import java.util.List;

public class HarvestBeehivesAction implements Action {
    boolean bucketIsFull = false;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> beeSkeps = Finder.findGobs(new NAlias("gfx/terobjs/beehive"));
        if (beeSkeps.isEmpty())
            return Results.SUCCESS();

        List<Gob> honeyAndWaxSkeps = NUtils.sortByNearest(
                beeSkeps.stream().filter(this::hasHoneyOrWax).toList(),
                NUtils.player().rc
        );

        for (Gob skep : honeyAndWaxSkeps) {
            if (!hasHoneyOrWax(skep)) continue;
            if (!ensureBucketEquipped()) return Results.ERROR("Bucket is not equipped");

            if (hasWax(skep)) {
                harvestWax(skep, gui);
            }

            if (hasHoney(skep)) {
                useBucket(skep, gui);
                if(bucketIsFull) {
                    return Results.SUCCESS();
                }
            }

            NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
        }

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

    private boolean ensureBucketEquipped() throws InterruptedException {
        return NUtils.getEquipment().findBucket("Empty") != null ||
                NUtils.getEquipment().findBucket("Honey") != null;
    }

    private void harvestWax(Gob skep, NGameUI gui) throws InterruptedException {
        new PathFinder(skep).run(gui);
        new SelectFlowerAction("Harvest wax", skep).run(gui);
        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/bushpickan"));
        NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
    }

    private void useBucket(Gob target, NGameUI gui) throws InterruptedException {
        new PathFinder(target).run(gui);

        WItem bucket = NUtils.getEquipment().findBucket("Empty");
        if (bucket == null) bucket = NUtils.getEquipment().findBucket("Honey");
        if (bucket == null) return;

        if(NUtils.bucketIsFull(bucket)) {
            bucketIsFull = true;
            return;
        }

        NUtils.takeItemToHand(bucket);
        NUtils.activateItem(target);

        NUtils.getUI().core.addTask(new WaitBucketInHandContentQuantityChange(bucket));

        NUtils.getEquipment().wdgmsg("drop", -1);
        NUtils.getUI().core.addTask(new WaitItemInEquip(bucket, new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}));
    }
}
