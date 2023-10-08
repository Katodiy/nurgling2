package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.*;

/*
* You need one branch in your inventory
* */

public class TESTtakehanddporop extends Test
{

    public TESTtakehanddporop()
    {
        this.name = "Branch";
    }

    String name;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        WItem item = gui.getInventory().getItem(name);
        Coord pos = item.c.div(Inventory.sqsz);
        NUtils.takeItemToHand(item);
        gui.getInventory().dropOn(pos,name);
    }
}
