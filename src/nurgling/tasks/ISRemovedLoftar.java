package nurgling.tasks;

import haven.res.ui.stackinv.ItemStack;
import nurgling.NUtils;

public class ISRemovedLoftar extends NTask {
    int id;

    int oldSize;
    ItemStack is;
    public ISRemovedLoftar(int id, ItemStack is) {
        super();
        this.id = id;
        this.is = is;
        oldSize = is.wmap.size();
    }

    @Override
    public boolean check() {
        return NUtils.getUI().getwidget(id)==null || oldSize!=is.wmap.size();
    }
}
