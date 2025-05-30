package nurgling.actions;

import nurgling.NUtils;
import nurgling.tasks.NTask;

public class WaitStockpile extends NTask {

    boolean exist = true;
    public WaitStockpile(boolean exist) {
        this.exist = exist;
    }

    @Override
    public boolean check() {
        return (NUtils.getGameUI().getStockpile()!=null)==exist;
    }
}
