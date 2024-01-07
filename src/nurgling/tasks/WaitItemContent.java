package nurgling.tasks;

import haven.WItem;
import nurgling.NGItem;

public class WaitItemContent implements NTask{

    WItem item;

    public WaitItemContent(WItem item) {
        this.item = item;
    }

    @Override
    public boolean check() {
        return item.item.spr!=null && ((NGItem)item.item).content()!=null;
    }
}
