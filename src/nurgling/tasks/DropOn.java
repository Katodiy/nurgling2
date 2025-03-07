package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

public class DropOn extends NTask
{
    public DropOn(NInventory inventory, Coord coord, NAlias name)
    {
        this.coord = coord;
        this.inventory = inventory;
        this.name = name;
        if(name.keys.contains("Traveller's Sack")) {
            name.keys.add("Traveler's Sack");
        } else if (name.keys.contains("Traveler's Sack")) {
            name.keys.add("Traveller's Sack");
        }
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
