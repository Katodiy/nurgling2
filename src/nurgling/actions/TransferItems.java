package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

import java.util.*;

public class TransferItems implements Action
{
    NInventory inv = null;
    ArrayList<WItem> items;
    int count = -1;

    public TransferItems(NInventory inv, ArrayList<WItem> items)
    {
        this.inv = inv;
        this.items = items;
    }

    public TransferItems(NInventory inv, ArrayList<WItem> items, int count)
    {
        this(inv, items);
        this.count = count;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(items.size()>0)
        {
            int oldSize = inv.getItems(items.get(0).item).size();
            int target_size = (count == -1) ? items.size() : Math.min(count, items.size());
            for (int i = 0; i < target_size; i++)
            {
                items.get(i).item.wdgmsg("transfer", Coord.z);
            }
            gui.tickmsg("command sended: " + target_size);
            gui.ui.core.addTask(new WaitItems(inv, items.get(0).item, oldSize + target_size));
            gui.tickmsg("all items transfered: " + target_size);
        }
        return Results.SUCCESS();
    }
}
