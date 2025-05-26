package nurgling.tasks;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import nurgling.*;
import nurgling.NInventory.QualityType;
import nurgling.tools.*;

import java.util.*;

public class GetItems extends NTask
{
    NAlias name = null;
    NInventory inventory;
    QualityType quality = null;
    float th = 1;
    boolean eq = false;
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

    public GetItems(NInventory inventory, NAlias name, float th)
    {
        this.name = name;
        this.inventory = inventory;
        this.th = th;
    }

    public GetItems(NInventory inventory, NAlias name, float th, QualityType quality)
    {
        this.name = name;
        this.inventory = inventory;
        this.th = th;
        this.quality = quality;
    }


    public GetItems(NInventory inventory, GItem target)
    {
        this.target = target;
        this.inventory = inventory;
        this.eq = true;
    }

    public GetItems(NInventory inventory, NAlias name, QualityType quality) {
        this.inventory = inventory;
        this.name = name;
        this.quality = quality;
    }

    private Comparator<WItem> high = new Comparator<WItem>() {
        @Override
        public int compare(WItem lhs, WItem rhs) {
            return Double.compare(((NGItem) rhs.item).quality, ((NGItem) lhs.item).quality);

        }
    };
    private Comparator<WItem> low = new Comparator<WItem>() {
        @Override
        public int compare(WItem lhs, WItem rhs) {
            return Double.compare(((NGItem) lhs.item).quality, ((NGItem) rhs.item).quality);

        }
    };


    @Override
    public boolean check()
    {
        if(target!=null && name==null)
            if(((NGItem)target).name()!=null)
                name = new NAlias(((NGItem)target).name());
            else
                return false;
        result.clear();
        if (checkContainer(inventory.child)) return false;
        if (quality != null) {
            Collections.sort(result, quality == QualityType.High ? high : low);
        }
        return true;
    }

    private boolean checkContainer(Widget first) {
        for (Widget widget = first; widget != null; widget = widget.next)
        {
            if (widget instanceof WItem)
            {
                WItem item = (WItem) widget;
                if (!NGItem.validateItem(item)) {
                    return true;
                } else {
                    if (name == null || NParser.checkName(((NGItem)item.item).name(), name)) {
                        if (item.item.contents != null) {
                            if(checkContainer(item.item.contents.child))
                                return true;
                        }
                        else {
                            if (th == 1)
                                result.add(item);
                            else if ((((NGItem) item.item).quality) != null && ((quality == QualityType.High || quality == null) && ((NGItem) item.item).quality >= th) || (quality == QualityType.Low && ((NGItem) item.item).quality <= th))
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
