package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class WaitItems implements NTask
{
    private final int target_size;
    NAlias name;
    NInventory inventory;

    GItem target = null;
    public WaitItems(NInventory inventory, NAlias name, int size)
    {
        this.name = name;
        this.inventory = inventory;
        this.target_size = size;
    }

    public WaitItems(NInventory inventory, GItem target, int size)
    {
        this.target = target;
        this.inventory = inventory;
        this.target_size = size;
    }

    @Override
    public boolean check()
    {

        if (target != null)
            if (((NGItem) target).name() != null)
                name = new NAlias(((NGItem) target).name());
            else
                return false;
        result.clear();
        for (Widget widget = inventory.child; widget != null; widget = widget.next)
        {
            if (widget instanceof WItem)
            {
                WItem item = (WItem) widget;
                String item_name;
                if ((item_name = ((NGItem) item.item).name()) == null)
                {
                    return false;
                }
                else
                {
                    if (NParser.checkName(item_name, name))
                    {
                        result.add(item);
                    }
                }
            }
        }
        return result.size() == target_size;
    }

    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getResult(){
        return result;
    }
}
