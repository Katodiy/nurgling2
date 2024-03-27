package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class WaitItemInInventory implements NTask
{
    String name;
    GItem item = null;
    NAlias naitem = null;

    public WaitItemInInventory(NAlias item) throws InterruptedException {
        this.naitem = item;
        this.item = NUtils.getGameUI().getInventory().getItem(naitem).item;
    }

    @Override
    public boolean check() {
        if (item != null) {
            if (((NGItem) item).name() == null)
                return false;
            else
                name = ((NGItem) item).name();

            WItem wi;
            try {
                wi = NUtils.getGameUI().getInventory().getItem(naitem);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return (wi != null) &&
                    wi.item.info != null &&
                    ((NGItem) wi.item).name() != null &&
                    NParser.checkName(((NGItem) wi.item).name(), name);
        } else {
            WItem wi;
            try {
                wi = NUtils.getGameUI().getInventory().getItem(naitem);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return (wi != null) &&
                    wi.item.info != null &&
                    ((NGItem) wi.item).name() != null;
        }
    }

}
