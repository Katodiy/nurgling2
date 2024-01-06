package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.*;

/*
* You need a pile of ore. There must be more ore in the pile than free space in the inventory
* */

public class TESTorestockpiletransfer extends Test
{
    public static final NAlias ores = new NAlias(new ArrayList<>(Arrays.asList("rit", "Ore", "ore")));

    public TESTorestockpiletransfer()
    {
        this.container = "Stockpile";
    }

    String container;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        Gob pile = TestUtils.findGob("stockpile");
        new OpenTargetContainer(container, pile).run(gui);
        ArrayList<WItem> items = gui.getInventory().getItems(ores);
        new TransferItemsOLD(gui.getStockpile(), items, items.size()).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

        new OpenTargetContainer(container, pile).run(gui);
        new TakeItemsFromPile(pile, gui.getStockpile()).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
