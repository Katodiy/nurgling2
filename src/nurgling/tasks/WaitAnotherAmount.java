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

public class WaitAnotherAmount extends NTask
{
    private final int target_size;
    NAlias name = null;
    Widget inventory;

    public WaitAnotherAmount(NInventory inventory, NAlias name, int size)
    {
        this.name = name;
        this.inventory = inventory;
        this.target_size = size;
    }



    @Override
    public boolean check() {

        int count = 0;

        for (Widget widget = inventory.child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                String item_name;
                if ((item_name = ((NGItem) item.item).name()) == null) {
                    return false;
                } else {
                    if (name == null || NParser.checkName(item_name, name)) {
                        GItem.Amount amount;
                        if ((amount = ((NGItem) item.item).getInfo(GItem.Amount.class)) != null) {
                            count += amount.itemnum();
                        } else {
                            return false;
                        }
                    }
                }
            }
        }

        return count != target_size;
    }

}
