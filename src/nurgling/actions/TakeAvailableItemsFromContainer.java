package nurgling.actions;

import haven.Coord;
import haven.Inventory;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TakeAvailableItemsFromContainer implements Action
{
    Coord target_coord = new Coord(1,1);
    Container cont;
    NAlias name;
    int needed = 0;
    public TakeAvailableItemsFromContainer(Container cont, NAlias names, int needed)
    {
        this.cont = cont;
        this.name = names;
        this.needed = needed;
    }

    boolean took = false;
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NInventory inv = gui.getInventory(cont.cap);
        WItem item = inv.getItem(name);
        if (item != null) {
            target_coord = inv.getItem(name).sz.div(Inventory.sqsz);
            int oldSpace = gui.getInventory().getItems(name).size();
            ArrayList<WItem> items = gui.getInventory(cont.cap).getItems(name);
            int target_size = Math.min(needed,Math.min(gui.getInventory().getNumberFreeCoord(target_coord), items.size()));


            for (int i = 0; i < target_size; i++) {
                items.get(i).item.wdgmsg("transfer", Coord.z);
            }
            WaitItems wi = new WaitItems(gui.getInventory(), name, oldSpace + target_size);
            NUtils.getUI().core.addTask(wi);
            cont.update();
            if (items.size() > target_size) {
                took = false;
                return Results.FAIL();
            }
            count = target_size;
        }
        took = true;
        return Results.SUCCESS();
    }

    public boolean getResult()
    {
        return took;
    }

    public int getCount()
    {
        return count;
    }
    int count = 0;
}
