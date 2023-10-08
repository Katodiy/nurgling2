package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class GetItems implements NTask
{
    NAlias name = null;
    NInventory inventory;

    GItem target = null;

    public GetItems(NInventory inventory)
    {
        this.inventory = inventory;
    }

    public GetItems(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }

    public GetItems(NInventory inventory, GItem target)
    {
        this.target = target;
        this.inventory = inventory;
    }

    @Override
    public boolean check()
    {
        if(target!=null)
            if(((NGItem)target).name()!=null)
                name = new NAlias(((NGItem)target).name());
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
                    if(name == null || NParser.checkName(item_name, name))
                    {
                        result.add(item);
                    }
                }
            }
        }
        return true;
    }

    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getResult(){
        return result;
    }
}
