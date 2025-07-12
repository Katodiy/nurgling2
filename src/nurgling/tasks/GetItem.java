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

    Class<? extends ItemInfo> prop = null;

    public GetItem(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }

    public GetItem(NInventory inventory, NAlias name, Class<? extends ItemInfo> prop )
    {
        this.name = name;
        this.inventory = inventory;
        this.prop = prop;
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
        result = null;
        return !checkContainer(inventory.child);
    }

    private boolean checkContainer(Widget first) {
        for (Widget widget = first; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if (!NGItem.validateItem(item)) {
                    return true;
                } else {
                    if (name == null || NParser.checkName(((NGItem)item.item).name(), name)) {
                        if (item.item.contents != null) {
                            if(checkContainer(item.item.contents.child))
                                return true;
                            if (result != null)
                                return false;
                        }
                        else {
                            if(prop!=null && ((NGItem) item.item).getInfo(prop)!=null)
                                continue;
                            if (q != -1) {
                                if (((NGItem) item.item).quality != q)
                                    continue;
                            }
                            result = item;
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private WItem result = null;

    public WItem getResult(){
        return result;
    }
}