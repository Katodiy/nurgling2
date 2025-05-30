package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import haven.res.ui.stackinv.ItemStack;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.tools.StackSupporter;

public class GetNotFullStack extends NTask
{
    NAlias name;
    NInventory inventory;

    final int maxSize;

    public GetNotFullStack(NInventory inventory, NAlias name)
    {
        this.name = name;
        this.inventory = inventory;
        maxSize = StackSupporter.getMaxStackSize(name.getDefault());
    }


    @Override
    public boolean check()
    {
        result = null;
        return !checkContainer(inventory.child);
    }

    private boolean checkContainer(Widget first) {
        for (Widget widget = first; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if (!NGItem.validateItem(item)) {
                    return true;
                } else {
                    if (NParser.checkName(((NGItem)item.item).name(), name)) {
                        if (item.item.contents != null && ((ItemStack) item.item.contents).wmap.size()!=maxSize) {
                            result = (ItemStack) item.item.contents;
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ItemStack result = null;

    public ItemStack getResult(){
        return result;
    }
}