package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

import java.util.*;
public class SimpleTransferToContainer implements Action
{
    Widget target = null;
    ArrayList<WItem> items;
    int count = -1;

    public SimpleTransferToContainer(NInventory inv, ArrayList<WItem> items)
    {
        this.target = inv;
        this.items = items;
    }

    public SimpleTransferToContainer(NInventory inv, ArrayList<WItem> items, int count)
    {
        this(inv, items);
        this.count = count;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(!items.isEmpty())
        {
            if(target instanceof NInventory)
            {
                NInventory inv = (NInventory) target;
                int oldSize = inv.getItems(items.get(0).item).size();
                int target_size = (count == -1) ? items.size() : Math.min(count, items.size());
                target_size = Math.min(target_size, inv.getNumberFreeCoord(items.get(0)));
                for (int i = 0; i < target_size; i++)
                {
                    items.get(i).item.wdgmsg("transfer", Coord.z);
                }
                gui.ui.core.addTask(new WaitItems(inv, items.get(0).item, oldSize + target_size));
            }
        }
        return Results.SUCCESS();
    }
}
