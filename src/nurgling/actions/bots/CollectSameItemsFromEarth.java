package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Resource;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.WaitItemInHand;
import nurgling.tasks.WaitItems;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectSameItemsFromEarth implements Action {

    NAlias itemName;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectGob selgob;
        NUtils.getGameUI().msg("Please select item for pile");
        (selgob = new SelectGob(Resource.loadsimg("baubles/selectItem"))).run(gui);
        Gob target = selgob.result;
        if(target==null)
        {
            return Results.ERROR("Item not found");
        }
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area with items");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);
        SelectArea outsa;
        NUtils.getGameUI().msg("Please select area for piles");
        (outsa = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);

        new PathFinder(target).run(gui);
        ArrayList<WItem> oldItems = NUtils.getGameUI().getInventory().getItems();
        NUtils.rclickGob(target);
        WaitItems wi = new WaitItems(NUtils.getGameUI().getInventory(),oldItems.size() + 1);
        NUtils.addTask(wi);
        for(WItem wItem : wi.getResult())
        {
            if(!oldItems.contains(wItem))
            {
                itemName = new NAlias(((NGItem)wItem.item).name());
                itemName.keys.add(target.ngob.name);
                break;
            }
        }
        new CollectItemsToPile(insa.getRCArea(),outsa.getRCArea(),itemName).run(gui);
        return Results.SUCCESS();
    }
}
