package nurgling.tasks;

import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;

public class WaitTargetSize implements NTask
{
    private final int target_size;
    Widget inventory;

    public WaitTargetSize(NInventory inventory, int size)
    {
        this.inventory = inventory;
        this.target_size = size;
    }



    @Override
    public boolean check() {

        int count = 0;

        for (Widget widget = inventory.child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if ( ((NGItem) item.item).name() == null) {
                    return false;
                } else {
                    count++;
                }
            }
        }

        return count == target_size;
    }

}
