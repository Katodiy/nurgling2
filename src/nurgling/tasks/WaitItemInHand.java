package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class WaitItemInHand implements NTask
{
    String name;
    GItem item = null;
    NInventory inventory;

    public WaitItemInHand(String name, NInventory inventory)
    {
        this.name = name;
        this.inventory = inventory;
    }

    public WaitItemInHand(WItem item, NInventory inventory)
    {
        this.item = item.item;
        this.inventory = inventory;
    }

    public WaitItemInHand(GItem item, NInventory inventory)
    {
        this.item = item;
        this.inventory = inventory;
    }

    @Override
    public boolean check()
    {
        if (item != null)
            if (((NGItem) item).name() == null)
                return false;
            else
                name = ((NGItem) item).name();
        WItem res;
        return (res = NUtils.getGameUI().vhand) != null &&
                res.item.info != null &&
                ((NGItem) res.item).name() != null &&
                NParser.checkName(((NGItem) res.item).name(), name);
    }
}
