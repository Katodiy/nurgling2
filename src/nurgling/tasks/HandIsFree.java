package nurgling.tasks;

import nurgling.NInventory;
import nurgling.NUtils;

public class HandIsFree extends NTask
{
    public HandIsFree(NInventory inventory)
    {
        this.inventory = inventory;
    }


    NInventory inventory;


    @Override
    public boolean check()
    {
        return NUtils.getGameUI().vhand == null || inventory.calcFreeSpace() == 0;
    }
}
