package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItemContent;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Fills waterskins using global water zone with chunk navigation.
 * Will navigate to the global water zone if it exists, otherwise shows an error.
 */
public class FillWaterskinsGlobal implements Action {

    public FillWaterskinsGlobal() {}

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Pair<Coord2d, Coord2d> area = null;
        
        // First try local area
        NArea nArea = NContext.findSpec(Specialisation.SpecName.water.toString());
        if (nArea == null) {
            // Try global area
            nArea = NContext.findSpecGlobal(Specialisation.SpecName.water.toString());
        }
        
        if (nArea == null) {
            return Results.ERROR("No water area found! Please create an area with 'water' specialization.");
        }
        
        // Navigate to the area using chunk navigation
        NUtils.navigateToArea(nArea);
        area = nArea.getRCArea();

        Gob target = null;
        if (area != null) {
            ArrayList<Gob> targets = Finder.findGobs(area, new NAlias("barrel", "cistern", "well"));
            for (Gob cand : targets) {
                if (NParser.isIt(cand, new NAlias("barrel"))) {
                    if (NUtils.barrelHasContent(cand) && NParser.checkName(NUtils.getContentsOfBarrel(cand), "water")) {
                        target = cand;
                        break;
                    }
                } else {
                    target = cand;
                    break;
                }
            }
            if (target == null)
                return Results.ERROR("No containers with water");
        } else {
            return Results.ERROR("No water area");
        }
        
        WItem wbelt = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        boolean needPf = true;
        if (wbelt != null) {
            if (wbelt.item.contents instanceof NInventory) {
                ArrayList<WItem> witems = ((NInventory) wbelt.item.contents).getItems(new NAlias("Waterskin"));
                if (!witems.isEmpty()) {
                    needPf = false;
                    new PathFinder(target).run(gui);
                }
                for (WItem item : witems) {
                    NGItem ngItem = ((NGItem) item.item);
                    if (ngItem.content().isEmpty()) {
                        NUtils.takeItemToHand(item);
                        NUtils.activateItem(target);
                        NUtils.getUI().core.addTask(new WaitItemContent(NUtils.getGameUI().vhand));
                        NUtils.transferToBelt();
                        NUtils.getUI().core.addTask(new HandIsFree(((NInventory) wbelt.item.contents)));
                    }
                }
            }
        }
        if (needPf)
            new PathFinder(target).run(gui);
        refillItemInEquip(gui, NUtils.getEquipment().findItem(NEquipory.Slots.LFOOT.idx), target);
        refillItemInEquip(gui, NUtils.getEquipment().findItem(NEquipory.Slots.RFOOT.idx), target);
        // Refill buckets in hands
        refillBucketInHand(gui, NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx), target);
        refillBucketInHand(gui, NUtils.getEquipment().findItem(NEquipory.Slots.HAND_RIGHT.idx), target);
        return Results.SUCCESS();
    }

    void refillItemInEquip(NGameUI gui, WItem item, Gob target) throws InterruptedException {
        if (NParser.isIt(target, new NAlias("barrel"))) {
            if (!NUtils.barrelHasContent(target) || !NParser.checkName(NUtils.getContentsOfBarrel(target), "water")) {
                return;
            }
        }
        if (item != null && item.item instanceof NGItem && NParser.checkName(((NGItem) item.item).name(), new NAlias("Waterskin", "Glass Jug"))) {
            NGItem ngItem = ((NGItem) item.item);
            if (ngItem.content().isEmpty()) {
                NUtils.takeItemToHand(item);
                NUtils.activateItem(target);
                NUtils.getUI().core.addTask(new WaitItemContent(NUtils.getGameUI().vhand));
                NUtils.getEquipment().wdgmsg("drop", -1);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return NUtils.getGameUI().vhand == null;
                    }
                });
            }
        }
    }

    void refillBucketInHand(NGameUI gui, WItem item, Gob target) throws InterruptedException {
        if (target == null) return;
        if (NParser.isIt(target, new NAlias("barrel"))) {
            if (!NUtils.barrelHasContent(target) || !NParser.checkName(NUtils.getContentsOfBarrel(target), "water")) {
                return;
            }
        }
        if (item != null && item.item instanceof NGItem && NParser.checkName(((NGItem) item.item).name(), "Bucket")) {
            NGItem ngItem = ((NGItem) item.item);
            // Refill if bucket is empty or has water but not full (not "10l")
            boolean needRefill = ngItem.content().isEmpty();
            if (!needRefill) {
                String contentName = ngItem.content().get(0).name();
                // Has water but not full (full bucket shows "10l of Water")
                if (contentName.contains("Water") && !contentName.contains("10l")) {
                    needRefill = true;
                }
            }
            if (needRefill) {
                NUtils.takeItemToHand(item);
                NUtils.activateItem(target);
                NUtils.getUI().core.addTask(new WaitItemContent(NUtils.getGameUI().vhand));
                NUtils.getEquipment().wdgmsg("drop", -1);
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return NUtils.getGameUI().vhand == null;
                    }
                });
            }
        }
    }
}



