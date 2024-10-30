package nurgling.actions.bots;

import haven.Coord;
import haven.Inventory;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Equip;
import nurgling.actions.Results;
import nurgling.actions.SelectFlowerAction;
import nurgling.actions.test.TESTtakehanddporop;
import nurgling.overlays.NCheckResult;
import nurgling.tasks.GetCurs;
import nurgling.tasks.WaitItemContent;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import static haven.OCache.posres;

public class CheckWater implements Action {
    NAlias cups = new NAlias("Wooden Cup", "Kuksa");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        WItem item = gui.getInventory().getItem(cups);
        Coord pos = item.c.div(Inventory.sqsz);
        NUtils.takeItemToHand(item);
        gui.map.wdgmsg("itemact", Coord.z, NUtils.player().rc.floor(posres), 3, 0);
        NUtils.addTask(new WaitItemContent(gui.vhand));
        String water = ((NGItem)gui.vhand.item).content().get(0).type();
        double quality = ((NGItem)gui.vhand.item).content().get(0).quality();
        NUtils.player().addcustomol(new NCheckResult(NUtils.player(),quality,water));
        gui.getInventory().dropOn(pos,cups);
        for(WItem titem : gui.getInventory().getItems(cups))
        {
            if(!((NGItem)titem.item).content().isEmpty() && ((NGItem)titem.item).content().get(0).type().equals(water))
            {
                new SelectFlowerAction("Empty",titem).run(gui);
            }
        }

        return Results.SUCCESS();
    }
}
