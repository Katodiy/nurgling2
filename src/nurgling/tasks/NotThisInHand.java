package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.tools.NParser;

public class NotThisInHand implements NTask
{
    WItem item = null;

    public NotThisInHand(WItem item)
    {
        this.item = item;
    }

    @Override
    public boolean check() {
        WItem res;
        return (res = NUtils.getGameUI().vhand) != null &&
                res != item;
    }
}
