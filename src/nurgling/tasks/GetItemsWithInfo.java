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

public class GetItemsWithInfo implements NTask
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
        for (Widget widget = inventory.child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if (item.item.info != null) {
                    for (ItemInfo inf : item.item.info)
                        if (inf.getClass() == c) {
                            result.add(item);
                        }
                }
                else
                {
                    return false;
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
