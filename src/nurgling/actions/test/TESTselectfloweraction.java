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
        ArrayList<WItem> items = gui.getInventory().getItems(name);
        for(WItem item : items)
        {
            new SelectFlowerAction("Split", (NWItem) item).run(gui);
        }
    }
}
