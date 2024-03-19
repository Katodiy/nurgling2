package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class WaitAnotherSize implements NTask
{
    private final int target_size;
    Widget inventory;

    public WaitAnotherSize(NInventory inventory, int size)
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

        return count != target_size;
    }

}
