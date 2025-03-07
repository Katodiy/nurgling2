package nurgling.tasks;

import haven.WItem;
import nurgling.NUtils;

public class WaitFreeHand extends NTask
{

    public WaitFreeHand()
    {
    }

    @Override
    public boolean check() {
        WItem res;
        return (res = NUtils.getGameUI().vhand) == null;
    }
}
