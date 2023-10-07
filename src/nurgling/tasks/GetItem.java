package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class GetItem implements NTask
{
    NAlias name;
    NInventory inventory;

    public GetItem(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }

    @Override
    public boolean check()
    {
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
                    if(NParser.checkName(item_name, name))
                    {
                        result = item;
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private WItem result = null;

    public WItem getResult(){
        return result;
    }
}
