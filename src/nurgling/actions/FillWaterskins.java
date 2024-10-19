package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.actions.bots.SelectArea;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitItemContent;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class FillWaterskins implements Action {
    boolean oz;
    public FillWaterskins(boolean only_area){oz = only_area;}

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Pair<Coord2d,Coord2d> area = null;
        NArea nArea = NArea.findSpec(Specialisation.SpecName.water.toString());
        if(nArea!=null)
        {
            area = nArea.getRCArea();
        }
        if(area==null) {
            if(oz)
                return Results.ERROR("no water area");

            SelectArea insa;
            NUtils.getGameUI().msg("Please, select area with cistern or barrel");
            (insa = new SelectArea(Resource.loadsimg("baubles/waterRefiller"))).run(gui);
            area = insa.getRCArea();
        }
        Gob target = null;
        if(area!=null)
        {
            target = Finder.findGob(area,new NAlias("barrel", "cistern"));
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
                    if(ngItem.content()==null)
                    {
                        NUtils.takeItemToHand(item);
                        NUtils.activateItem(target);
                        NUtils.getUI().core.addTask(new WaitItemContent(NUtils.getGameUI().vhand));
                        NUtils.transferToBelt();
                        NUtils.getUI().core.addTask(new HandIsFree(((NInventory) wbelt.item.contents)));
                    }
                }
            }
            AutoDrink.waitRefil.set(false);
        }
        return Results.SUCCESS();
    }

    public static boolean checkIfNeed() throws InterruptedException {
        WItem wbelt = NUtils.getEquipment().findItem(NEquipory.Slots.BELT.idx);
        if (wbelt != null) {
            if (wbelt.item.contents instanceof NInventory) {
                ArrayList<WItem> witems = ((NInventory) wbelt.item.contents).getItems(new NAlias("Waterskin"));
                if (!witems.isEmpty()) {
                    for (WItem item : witems) {
                        NGItem ngItem = ((NGItem) item.item);
                        if (ngItem.content() != null) {
                            if (ngItem.content().name().contains("Water"))
                                return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
