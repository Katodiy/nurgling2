package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.Objects;

public class GetItem extends NTask
{
    NAlias name;
    NInventory inventory;

    float q = -1;

    public GetItem(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }

    public GetItem(NInventory inventory, NAlias name, float q)
    {
        this.name = name;
        this.inventory = inventory;
        this.q = q;
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
                        if(q !=-1) {
                            if (((NGItem) item.item).quality != q)
                                continue;
                        }
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
