package nurgling.tasks;

import haven.res.ui.stackinv.ItemStack;
import nurgling.tasks.NTask;

public class StackSizeChanged extends NTask {
    ItemStack is;
    int oldSize;
    public StackSizeChanged(ItemStack is, int originalSize) {
        super();
        this.is = is;
        oldSize = originalSize;
    }

    @Override
    public boolean check() {
        return oldSize!=is.wmap.size();
    }
}
