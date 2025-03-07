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
        for (Widget widget = inventory.child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if(item.item.contents!=null) {
                    for (Widget swidget = item.item.contents.child; swidget != null; swidget = swidget.next) {
                        if (swidget instanceof WItem) {
                            WItem sitem = (WItem) swidget;
                            if (sitem.item.info != null) {
                                for (ItemInfo inf : sitem.item.info)
                                    if (inf.getClass() == c) {
                                        result.add(sitem);
                                    }
                            }
                        }
                    }
                }
                else if (item.item.info != null) {
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
