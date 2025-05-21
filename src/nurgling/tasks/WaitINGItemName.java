package nurgling.tasks;

import haven.WItem;
import nurgling.NGItem;

public class WaitINGItemName extends NTask{

    WItem item;
    NGItem ngitem;

    public WaitINGItemName(WItem item) {
        this.item = item;
        this.ngitem = (NGItem)item.item;
    }

    @Override
    public boolean check() {
        return (ngitem.name() != null && !ngitem.name().isEmpty());
    }
}
