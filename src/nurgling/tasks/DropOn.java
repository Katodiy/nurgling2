package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class DropOn implements NTask
{
    public DropOn(NInventory inventory, Coord coord, NAlias name)
    {
        this.coord = coord;
        this.inventory = inventory;
        this.name = name;
    }

    public DropOn(NInventory inventory, Coord coord, String name)
    {
        this(inventory, coord, new NAlias(name));
    }

    Coord coord;
    NInventory inventory;

    NAlias name;

    @Override
    public boolean check()
    {
        return !inventory.isSlotFree(coord) && inventory.isItemInSlot(coord, name);
    }
}
