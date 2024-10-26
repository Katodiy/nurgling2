package nurgling.actions;

import haven.Coord;
import haven.Inventory;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

public class TakeItemsFromContainer implements Action
{
    Coord target_coord = new Coord(1,1);
    Container cont;
    HashSet<String> names;
    int minSize = Integer.MAX_VALUE;
    public TakeItemsFromContainer(Container cont, HashSet<String> names)
    {
        this.cont = cont;
        this.names = names;
    }
    int target_size = 0;
    boolean took = false;
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NInventory inv = gui.getInventory(cont.cap);
        for(String name: names) {
            WItem item = inv.getItem(name);
            if (item != null) {

                target_coord = inv.getItem(name).sz.div(Inventory.sqsz);
                int oldSpace = gui.getInventory().getItems(name).size();
                ArrayList<WItem> items = gui.getInventory(cont.cap).getItems(name,1);
                target_size = Math.min(minSize,Math.min(gui.getInventory().getNumberFreeCoord(target_coord), items.size()));


                for (int i = 0; i < target_size; i++) {
                    items.get(i).item.wdgmsg("transfer", Coord.z);
                }
                WaitItems wi = new WaitItems(gui.getInventory(), new NAlias(name), oldSpace + target_size);
                NUtils.getUI().core.addTask(wi);
                cont.update();
                if(items.size()>target_size) {
                    took = false;
                    return Results.FAIL();
                }
            }
        }
        took = true;
        return Results.SUCCESS();
    }

    public boolean getResult()
    {
        return took;
    }

    public int getTarget_size() {
        return target_size;
    }
}
