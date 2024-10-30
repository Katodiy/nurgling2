package nurgling.tasks;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NUtils;

public class HandWithoutContent implements NTask
{

    @Override
    public boolean check() {
        WItem res;
        return (res = NUtils.getGameUI().vhand) != null && ((NGItem)NUtils.getGameUI().vhand.item).content().isEmpty();
    }
}
