package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;

import java.util.*;

/*
* You need blocks in your inventory
* */

public class TESTselectfloweraction extends Test
{

    public TESTselectfloweraction()
    {
        this.name = "Block";
        this.num = 1;
    }

    String name;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        NUtils.getGameUI().msg("(1,1)" + String.valueOf(NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1,1))));
        NUtils.getGameUI().msg("(2,1)" + String.valueOf(NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(2,1))));
        NUtils.getGameUI().msg("(2,2)" + String.valueOf(NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(2,2))));
        NUtils.getGameUI().msg("(1,2)" + String.valueOf(NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1,2))));
//        ArrayList<WItem> items = gui.getInventory().getItems(name);
//        for(WItem item : items)
//        {
//            new SelectFlowerAction("Split", (NWItem) item).run(gui);
//        }
    }
}
