package nurgling.tasks;

import haven.res.ui.stackinv.ItemStack;
import nurgling.tasks.NTask;

public class StackSizeChanged extends NTask {
    ItemStack is;
    int oldSize;
    public StackSizeChanged(ItemStack is) {
        super();
        this.is = is;
        oldSize = is.wmap.size();
    }

    @Override
    public boolean check() {
        return oldSize!=is.wmap.size();
    }
}
