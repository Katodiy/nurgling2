package nurgling.actions;

import haven.Coord;
import haven.Inventory;
import haven.WItem;
import nurgling.*;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.tools.StackSupporter;

import java.util.ArrayList;
import java.util.HashSet;

public class TakeItemsFromContainer implements Action
{
    Coord target_coord = new Coord(1,1);
    Container cont;
    HashSet<String> names;
    NAlias pattern;
    int minSize = Integer.MAX_VALUE;
    public TakeItemsFromContainer(Container cont, HashSet<String> names, NAlias pattern)
    {
        this.cont = cont;
        this.names = names;
        this.pattern = pattern;
    }

    double targetq = -1;
    public TakeItemsFromContainer(Container cont, HashSet<String> names, NAlias pattern, double q)
    {
        this.cont = cont;
        this.names = names;
        this.pattern = pattern;
        this.targetq = q;
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
                ArrayList<WItem> items = getItems(gui, name);
                int items_size = items.size();
                target_size = Math.min(minSize,Math.min(gui.getInventory().getNumberFreeCoord(target_coord.swapXY())*StackSupporter.getMaxStackSize(name), items.size()));


                int temptr = target_size;
                for (int i = 0; i < temptr; i++) {
                    TransferToContainer.transfer(items.get(i), gui.getInventory(), target_size);
                    items = getItems(gui, name);
                    temptr=Math.min(minSize,Math.min(gui.getInventory().getNumberFreeCoord(target_coord.swapXY())*StackSupporter.getMaxStackSize(name), items.size()));
                    i = -1;
                }
                WaitItems wi = new WaitItems(gui.getInventory(), new NAlias(name), oldSpace + target_size);
                NUtils.getUI().core.addTask(wi);
                cont.update();
                if(items_size>target_size) {
                    took = false;
                    return Results.FAIL();
                }
            }
        }
        took = true;
        return Results.SUCCESS();
    }

    private ArrayList<WItem> getItems(NGameUI gui, String name) throws InterruptedException
    {
        ArrayList<WItem> items = gui.getInventory(cont.cap).getItems(name,1);
        HashSet<WItem> forRemove = new HashSet<>();

        for(WItem item1: items) {
            if (pattern != null) {
                if (!NParser.checkName(((NGItem) item1.item).name(), pattern)) {
                    forRemove.add(item1);
                }
            }
            if(targetq!=-1)
            {
                if(((NGItem) item1.item).quality>targetq)
                {
                    forRemove.add(item1);
                }
            }
        }
        items.removeAll(forRemove);
        return items;
    }

    public boolean getResult()
    {
        return took;
    }

    public int getTarget_size() {
        return target_size;
    }
}
