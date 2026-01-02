package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.actions.bots.SelectArea;
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
import java.util.List;

public class FillWaterskins implements Action {
    boolean oz;
    public FillWaterskins(boolean only_area){oz = only_area;}
    public FillWaterskins(){oz = false;}

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Pair<Coord2d,Coord2d> area = null;
        NArea nArea = NContext.findSpec(Specialisation.SpecName.water.toString());
        if(nArea==null)
        {
            nArea = NContext.findSpecGlobal(Specialisation.SpecName.water.toString());
            if(nArea!=null)
            {
                NUtils.navigateToArea(nArea);
                area = nArea.getRCArea();
            }
        }
        else
        {
            area = nArea.getRCArea();
        }

        Gob target = null;
        if(area!=null)
        {
            ArrayList<Gob> targets = Finder.findGobs(area,new NAlias("barrel", "cistern", "well"));
            for(Gob cand: targets)
            {
                if(NParser.isIt(cand,new NAlias("barrel")))
                {
                    if(NUtils.barrelHasContent(cand) && NParser.checkName(NUtils.getContentsOfBarrel(cand), "water")) {
                        target = cand;
                        break;
                    }
                }
                else {
                    target = cand;
                    break;
                }
            }
            if(target==null)
                return Results.ERROR("No containers with water");
        }
        WItem wbelt = NUtils.getEquipment().findItem (NEquipory.Slots.BELT.idx);
        if(wbelt!=null)
        {
            if(wbelt.item.contents instanceof NInventory)
            {
                ArrayList<WItem> witems = ((NInventory) wbelt.item.contents).getItems(new NAlias("Waterskin"));
                if(!witems.isEmpty() && target!=null)
                    new PathFinder(target).run(gui);
                for(WItem item : witems)
                {
                    NGItem ngItem = ((NGItem)item.item);
                    if(ngItem.content().isEmpty())
                    {
                        NUtils.takeItemToHand(item);
                        NUtils.activateItem(target);
                        NUtils.getUI().core.addTask(new WaitItemContent(NUtils.getGameUI().vhand));
                        NUtils.transferToBelt();
                        NUtils.getUI().core.addTask(new HandIsFree(((NInventory) wbelt.item.contents)));
                    }
                }
            }
        }
        refillItemInEquip(gui,NUtils.getEquipment().findItem(NEquipory.Slots.LFOOT.idx),target);
        refillItemInEquip(gui,NUtils.getEquipment().findItem(NEquipory.Slots.RFOOT.idx),target);
        // Refill buckets in hands
        refillBucketInHand(gui,NUtils.getEquipment().findItem(NEquipory.Slots.HAND_LEFT.idx),target);
        refillBucketInHand(gui,NUtils.getEquipment().findItem(NEquipory.Slots.HAND_RIGHT.idx),target);
        return Results.SUCCESS();
    }

    void refillItemInEquip(NGameUI gui, WItem item, Gob target) throws InterruptedException
    {
        if(NParser.isIt(target,new NAlias("barrel")))
        {
            if(!NUtils.barrelHasContent(target) || !NParser.checkName(NUtils.getContentsOfBarrel(target), "water")) {
                return;
            }
        }
        if(item!=null && item.item instanceof NGItem && NParser.checkName(((NGItem)item.item).name(), new NAlias("Waterskin", "Glass Jug"))) {
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

    void refillBucketInHand(NGameUI gui, WItem item, Gob target) throws InterruptedException
    {
        if(target == null) return;
        if(NParser.isIt(target,new NAlias("barrel")))
        {
            if(!NUtils.barrelHasContent(target) || !NParser.checkName(NUtils.getContentsOfBarrel(target), "water")) {
                return;
            }
        }
        if(item!=null && item.item instanceof NGItem && NParser.checkName(((NGItem)item.item).name(), "Bucket")) {
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


    public static boolean checkIfNeed() throws InterruptedException {
        boolean hasWaterskin = false;
        boolean hasWaterInWaterskin = false;
        
        WItem wbelt = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (wbelt != null) {
            if (wbelt.item.contents instanceof NInventory) {
                ArrayList<WItem> witems = ((NInventory) wbelt.item.contents).getItems(new NAlias("Waterskin"));
                if (!witems.isEmpty()) {
                    hasWaterskin = true;
                    for (WItem item : witems) {
                        NGItem ngItem = ((NGItem) item.item);
                        if (!ngItem.content().isEmpty()) {
                            if (ngItem.content().get(0).name().contains("Water")) {
                                hasWaterInWaterskin = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Check buckets in hands
        boolean hasBucket = false;
        boolean hasWaterInBucket = false;
        WItem bucket = NUtils.getEquipment().findBucket("Water");
        if (bucket != null) {
            hasBucket = true;
            NGItem ngItem = ((NGItem) bucket.item);
            if (!ngItem.content().isEmpty() && ngItem.content().get(0).name().contains("Water")) {
                hasWaterInBucket = true;
            }
        }
        
        // Need refill if we have containers but none of them have water
        if (hasWaterskin || hasBucket) {
            return !hasWaterInWaterskin && !hasWaterInBucket;
        }
        return false;
    }
}
