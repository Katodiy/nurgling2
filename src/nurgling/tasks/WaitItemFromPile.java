package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NUtils;

import java.util.ArrayList;

public class WaitItemFromPile extends NTask
{
    int target_size = 1;
    int totalItemCount = 0;

    public WaitItemFromPile()
    {
    }

    public WaitItemFromPile(int target_size)
    {
        this.target_size = target_size;
    }

    @Override
    public boolean check()
    {
        result.clear();
        totalItemCount = 0;

        for (Widget widget : NUtils.getUI().getMonitorInfo())
        {
            if (widget instanceof NGItem)
            {
                NGItem item = (NGItem) widget;
                if (item.name() == null)
                {
                    return false;
                }
                else {
                    if (item.contents != null) {
                        for (Widget cwidget = item.contents.child; cwidget != null; cwidget = cwidget.next) {
                            if (cwidget instanceof WItem) {
                                WItem wi = (WItem)cwidget;
                                result.add((NGItem) wi.item);
                                totalItemCount += getItemCount((NGItem) wi.item);
                            }
                        }
                    }
                    else
                    {
                        result.add(item);
                        totalItemCount += getItemCount(item);
                    }
                }
            }
        }
        return totalItemCount >= target_size;
    }

    private int getItemCount(NGItem item)
    {
        GItem.Amount amount = item.getInfo(GItem.Amount.class);
        if (amount != null && amount.itemnum() > 0) {
            return amount.itemnum();
        }
        return 1;
    }

    private ArrayList<NGItem> result = new ArrayList<>();

    public ArrayList<NGItem> getItems(){
        return result;
    }

    public int getTotalItemCount(){
        return totalItemCount;
    }
}
