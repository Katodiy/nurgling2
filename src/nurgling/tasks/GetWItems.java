package nurgling.tasks;

import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class GetWItems extends NTask
{
    NAlias name = null;
    NInventory inventory;


    public GetWItems(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
    }

    @Override
    public boolean check()
    {
        result.clear();
        for (Widget widget = inventory.child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if (!NGItem.validateItem(item)) {
                    return false;
                }
                if (name == null || NParser.checkName(((NGItem)item.item).name(), name))
                {
                    result.add(item);
                }
            }
        }
        return true;
    }



    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getItems(){
        return result;
    }
}
