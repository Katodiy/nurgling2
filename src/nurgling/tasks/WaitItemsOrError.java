package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.*;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class WaitItemsOrError implements NTask
{
    private final int target_size;
    NAlias name = null;
    Widget inventory;
    String errmsg;
    ArrayList<WItem> items;
    GItem target = null;
    public WaitItemsOrError(NInventory inventory, NAlias name, int size, String errmsg, ArrayList<WItem> items)
    {
        this.name = name;
        this.inventory = inventory;
        this.target_size = size;
        this.errmsg = errmsg;
        this.items = items;
    }


    @Override
    public boolean check()
    {

        if (target != null)
            if (((NGItem) target).name() != null)
                name = new NAlias(((NGItem) target).name());
            else
                return false;

        if(inventory instanceof NInventory)
        {
            result.clear();

            for (Widget widget = inventory.child; widget != null; widget = widget.next)
            {
                if (widget instanceof WItem)
                {
                    WItem item = (WItem) widget;
                    String item_name;
                    if ((item_name = ((NGItem) item.item).name()) == null)
                    {
                        return false;
                    }
                    else
                    {
                        if (name == null || NParser.checkName(item_name, name))
                        {
                            if(!items.contains(item))
                                result.add(item);
                        }
                    }
                }
            }
            String lastMsg = NUtils.getUI().getLastError();
            return result.size() >= target_size || (lastMsg!=null && lastMsg.contains(errmsg));
        }
        else if(inventory instanceof NISBox)
        {
            return ((NISBox) inventory).calcFreeSpace() == target_size;
        }
        return false;
    }

    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getResult(){
        return result;
    }
}
