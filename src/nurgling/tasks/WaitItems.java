package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class WaitItems extends NTask
{
    private final int target_size;
    NAlias name = null;
    Widget inventory;

    int count = 0;

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

    public WaitItems(NISBox inv, int size)
    {
        this.inventory = inv;
        this.target_size = size;
    }

    public WaitItems(NInventory inv, int size)
    {
        this.inventory = inv;
        this.target_size = size;
    }

    @Override
    public boolean check()
    {
        count++;
        if (target != null)
            if (((NGItem) target).name() != null)
                name = new NAlias(((NGItem) target).name());
            else
                return false;

        if(inventory instanceof NInventory)
        {
            result.clear();

            if (checkSize(inventory.child)) return false;
            if(count == 1000) {
                NUtils.getGameUI().error("WAIT ITEMS ERROR result.size():" + String.valueOf(result.size()) + " req target size:" + String.valueOf(target_size) + " WITEMS: " + ((name != null && name.keys.size() > 0) ? name.keys.get(0) : "null"));
                return true;
            }
            return result.size() == target_size || ((NInventory) inventory).calcFreeSpace()==0; /*Плохое решение, надо было добить заполнение последнего стака но уже лень*/
        }
        else if(inventory instanceof NISBox)
        {
            return ((NISBox) inventory).calcFreeSpace() == target_size;
        }
        return false;
    }

    private boolean checkSize(Widget first) {
        for (Widget widget = first; widget != null; widget = widget.next)
        {
            if (widget instanceof WItem)
            {
                WItem item = (WItem) widget;
                if (!NGItem.validateItem(item)) {
                    return true;
                } else {
                    if (name == null || NParser.checkName(((NGItem)item.item).name(), name)) {
                        if(item.item.contents!=null)
                        {
                            if(checkSize(item.item.contents.child))
                                return true;
                        }
                        else {
                            result.add(item);
                        }
                    }
                }
            }
        }
        return false;
    }

    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getResult(){
        return result;
    }
}
