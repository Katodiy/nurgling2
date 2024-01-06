package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.tasks.*;

import java.util.*;
/// TODO FOR DELETE
public class TransferItemsOLD implements Action
{
    Widget target = null;
    ArrayList<WItem> items;
    int count = -1;

    public TransferItemsOLD(NInventory inv, ArrayList<WItem> items)
    {
        this.target = inv;
        this.items = items;
    }

    public TransferItemsOLD(NInventory inv, ArrayList<WItem> items, int count)
    {
        this(inv, items);
        this.count = count;
    }

    public TransferItemsOLD(NISBox inv, ArrayList<WItem> items)
    {
        this.target = inv;
        this.items = items;
    }

    public TransferItemsOLD(NISBox inv, ArrayList<WItem> items, int count)
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
                items_transferd = target_size;
                space_left = inv.getNumberFreeCoord(items.get(0));
            }
            if(target instanceof NISBox)
            {
                NISBox inv = (NISBox) target;
                int oldSize = inv.getFreeSpace();
                int target_size = (count == -1) ? items.size() : Math.min(count, items.size());
                target_size = Math.min(target_size, oldSize);
                for (int i = 0; i < target_size; i++)
                {
                    items.get(i).item.wdgmsg("transfer", Coord.z);
                }
                gui.ui.core.addTask(new WaitItems(inv, oldSize - target_size));

            }
        }
        return Results.SUCCESS();
    }

    public int spaceLeft()
    {
        return space_left;
    }

    public int itemsTransfered()
    {
        return items_transferd;
    }

    private int items_transferd = 0;
    private int space_left = -1;
}
