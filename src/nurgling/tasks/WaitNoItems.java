package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NISBox;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class WaitNoItems extends NTask
{
    NAlias name = null;
    Widget inventory;

    GItem target = null;
    public WaitNoItems(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }


    @Override
    public boolean check()
    {

        if (target != null)
            if (((NGItem) target).name() != null)
                name = new NAlias(((NGItem) target).name());
            else
                return false;

        if(inventory instanceof NInventory)
        {
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
                        if (name == null || NParser.checkName(item_name, name))
                        {
                            result.add(item);
                        }
                    }
                }
            }
            return result.isEmpty();
        }
        return false;
    }

    private ArrayList<WItem> result = new ArrayList<>();

}
