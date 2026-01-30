package nurgling.tasks;

import haven.*;
import haven.res.ui.tt.cn.CustomName;
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
    
    private int getActualItemCount(WItem item) {
        if (item.item.info != null) {
            for (ItemInfo inf : item.item.info) {
                if (inf instanceof CustomName) {
                    float count = ((CustomName) inf).count;
                    if (count > 0) {
                        return (int) (count * 100);
                    }
                }
            }
        }
        return 1;
    }
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
            
            int actualCount = 0;
            for (WItem item : result) {
                actualCount += getActualItemCount(item);
            }
            
            if(count == 1000) {
                NUtils.getGameUI().error("WAIT ITEMS ERROR result.size():" + String.valueOf(result.size()) + " actual count: " + actualCount + " req target size:" + String.valueOf(target_size) + " WITEMS: " + ((name != null && name.keys.size() > 0) ? name.keys.get(0) : "null"));
                return true;
            }
            return actualCount >= target_size || ((NInventory) inventory).calcFreeSpace()==0;
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

    public ArrayList<WItem> getItems(){
        return result;
    }
    
    public int getActualCount() {
        int actualCount = 0;
        for (WItem item : result) {
            actualCount += getActualItemCount(item);
        }
        return actualCount;
    }
}
