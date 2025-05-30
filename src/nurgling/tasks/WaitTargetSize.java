package nurgling.tasks;

import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.Collections;

public class WaitTargetSize extends NTask
{
    private final int target_size;
    Widget inventory;

    int count = 0;
    public WaitTargetSize(NInventory inventory, int size)
    {
        this.inventory = inventory;
        this.target_size = size;
    }




    @Override
    public boolean check()
    {
        count = 0;
        if (checkContainer(inventory.child)) return false;
        return count == target_size;
    }

    private boolean checkContainer(Widget first) {

        for (Widget widget = first; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if ((((NGItem) item.item).name()) == null) {
                    return true;
                } else {

                    if (item.item.contents != null) {
                        if(checkContainer(item.item.contents.child))
                            return true;
                    } else {
                        count++;
                    }
                }
            }
        }

        return false;
    }


}
