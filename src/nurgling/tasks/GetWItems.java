package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.NInventory.QualityType;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

    public ArrayList<WItem> getResult(){
        return result;
    }
}
