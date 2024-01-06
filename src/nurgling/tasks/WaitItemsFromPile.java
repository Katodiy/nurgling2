package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NISBox;
import nurgling.NInventory;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class WaitItemsFromPile implements NTask
{
    private final int target_size;
    private final int start_size;
    NAlias name = null;
    Widget inventory;
    long pile;
    GItem target = null;

    public WaitItemsFromPile(long pile, NInventory inv, int startsize, int size)
    {
        this.inventory = inv;
        this.target_size = size;
        this.pile = pile;
        this.start_size = startsize;
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
                            result.add(item);
                        }
                    }
                }
            }
            return result.size() == start_size + target_size;
        }
        return false;
    }

    private ArrayList<WItem> result = new ArrayList<>();

    public ArrayList<WItem> getResult(){
        return result;
    }

    public int took(){
        return result.size()-start_size;
    }
}
