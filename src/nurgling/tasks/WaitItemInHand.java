package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class WaitItemInHand implements NTask
{
    String name;
    GItem item = null;

    public WaitItemInHand(String name)
    {
        this.name = name;
    }

    public WaitItemInHand(WItem item)
    {
        this.item = item.item;
    }

    public WaitItemInHand(GItem item)
    {
        this.item = item;
    }

    public WaitItemInHand()
    {

    }

    @Override
    public boolean check() {
        if (item != null) {
            if (((NGItem) item).name() == null)
                return false;
            else
                name = ((NGItem) item).name();
            WItem res;
            return (res = NUtils.getGameUI().vhand) != null &&
                    res.item.info != null &&
                    ((NGItem) res.item).name() != null &&
                    NParser.checkName(((NGItem) res.item).name(), name);
        } else {
            WItem res;
            return (res = NUtils.getGameUI().vhand) != null &&
                    res.item.info != null &&
                    ((NGItem) res.item).name() != null;
        }
    }

}
