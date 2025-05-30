package nurgling.tasks;

import haven.GItem;
import haven.ItemInfo;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class GetItemsWithInfo extends NTask
{
    NInventory inventory = null;
    Class<? extends ItemInfo> c;

    public <C extends ItemInfo> GetItemsWithInfo(NInventory inventory, Class<C> c)
    {
        this.inventory = inventory;
        this.c = c;
    }

    @Override
    public boolean check() {
        result.clear();
        return !checkContainer(inventory.child);
    }

    private boolean checkContainer(Widget first) {
        for (Widget widget = first; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if(item.item.contents != null) {
                    if (checkContainer(item.item.contents.child)) {
                        return true;
                    }
                }
                else if (item.item.info != null) {
                    for (ItemInfo inf : item.item.info) {
                        if (inf.getClass() == c) {
                            result.add(item);
                        }
                    }
                }
                else {
                    return true;
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