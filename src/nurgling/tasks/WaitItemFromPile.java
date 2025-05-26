package nurgling.tasks;

import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NUI;
import nurgling.NUtils;

import java.util.ArrayList;

public class WaitItemFromPile extends NTask
{
    int target_size = 1;
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
                            }
                        }
                    }
                    else
                    {
                        result.add(item);
                    }
                }
            }
        }
        return result.size() >=target_size;
    }

    private ArrayList<NGItem> result = new ArrayList<>();

    public ArrayList<NGItem> getResult(){
        return result;
    }
}
