package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.*;

/*
* You need a pile of ore. There must be more ore in the pile than free space in the inventory (NO window close command)
* */

public class TESTorestockpiletransfernoclose extends Test
{
    public static final NAlias ores = new NAlias(new ArrayList<>(Arrays.asList("rit", "Ore", "ore")));

    public TESTorestockpiletransfernoclose()
    {
        this.container = "Stockpile";
    }

    String container;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        double alpha = 0;
        while (true)
        {

            new SetDirTrol(new Coord2d(Math.cos(alpha),Math.sin(alpha))).run(gui);
            alpha+=Math.PI/9;
            if(Math.abs(Math.PI*2 - alpha)<0.05)
                alpha = 0;
        }
//        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
//        ArrayList<WItem> items = gui.getInventory().getItems(ores);
//        new TransferItems(gui.getStockpile(), items, items.size()).run(gui);
//        new TakeItemsFromPile(gui.getStockpile()).run(gui);
//        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
