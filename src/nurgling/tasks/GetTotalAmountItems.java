package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class GetTotalAmountItems extends NTask
{
    NAlias name = null;
    NInventory inventory;

    boolean eq = false;
    GItem target = null;

    public GetTotalAmountItems(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }

    public GetTotalAmountItems(NInventory inventory, GItem target)
    {
        this.target = target;
        this.inventory = inventory;
        this.eq = true;
    }

    @Override
    public boolean check()
    {
        count = 0;
        if(target!=null)
            if(((NGItem)target).name()!=null)
                name = new NAlias(((NGItem)target).name());
            else
                return false;
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
                    if (name == null || (eq && !name.keys.isEmpty() ? item_name.equals(name.getDefault()) : NParser.checkName(item_name, name)))
                    {
                        GItem.Amount amount;
                        if((amount = ((NGItem)item.item).getInfo(GItem.Amount.class))!=null) {
                            count += amount.itemnum();
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private int count = 0;

    public int getResult(){
        return count;
    }
}
