package nurgling.tasks;

import haven.WItem;

public class WaitItemSpr extends NTask{

    WItem item;

    public WaitItemSpr(WItem item) {
        this.item = item;
    }

    @Override
    public boolean check() {
        return item.item.spr!=null;
    }
}
