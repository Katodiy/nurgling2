package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.Inventory;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NISBox;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tasks.WaitItems;
import nurgling.tasks.WaitItemsFromPile;
import nurgling.tools.Context;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TakeItemsFromContainer implements Action
{
    NInventory inv;
    Context.Container cont;

    int took = 0;

    int target_size = Integer.MAX_VALUE;

    Coord target_coord = new Coord(1,1);

    String name;
    public TakeItemsFromContainer(Context.Container cont, String name, int th)
    {
        this.cont = cont;
        this.name = name;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NInventory inv = gui.getInventory(cont.cap);
        WItem item = inv.getItem(name);
        if (item != null) {
            target_coord = inv.getItem(name).sz.div(Inventory.sqsz);
            int oldSpace = gui.getInventory().getItems(name).size();
            ArrayList<WItem> items = gui.getInventory(cont.cap).getItems(name);
            if (target_size != Integer.MAX_VALUE) {
                target_size =Math.min(Math.min(target_size, gui.getInventory().getNumberFreeCoord(target_coord)),items.size());
            } else {
                target_size = Math.min(gui.getInventory().getNumberFreeCoord(target_coord),items.size());
            }


            for (int i = 0; i < target_size; i++) {
                items.get(i).item.wdgmsg("transfer", Coord.z);
            }
            WaitItems wi = new WaitItems(gui.getInventory(), new NAlias(name), oldSpace + target_size);
            NUtils.getUI().core.addTask(wi);
            took = target_size;
            items = inv.getItems(name);
//            if(items.isEmpty())
//                cont.itemInfo.remove(name);
//            else
//                cont.itemInfo.put(name, items.size());
        }
        return Results.SUCCESS();
    }

    public int getResult()
    {
        return took;
    }
}
