package nurgling.tasks;

import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NUtils;

import java.util.ArrayList;

public class WaitItemFromPile extends NTask
{
    ArrayList<WItem> inventoryItems;
    int target_size = 1;
    public WaitItemFromPile(ArrayList<WItem> inventoryItems)
    {
        this.inventoryItems = inventoryItems;
    }

    public WaitItemFromPile(ArrayList<WItem> inventoryItems, int target_size)
    {
        this.inventoryItems = inventoryItems;
        this.target_size = target_size;
    }

    @Override
    public boolean check()
    {
        result.clear();

        for (Widget widget = NUtils.getGameUI().getInventory().child; widget != null; widget = widget.next)
        {
            if (widget instanceof WItem)
            {
                WItem item = (WItem) widget;
                if ((((NGItem) item.item).name()) == null)
                {
                    return false;
                }
                else
                {
                    if(!inventoryItems.contains(item))
                        result.add(item);
                }
            }
        }
        return result.size() >=target_size;
    }

    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getResult(){
        return result;
    }
}
