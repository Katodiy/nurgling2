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
        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        ArrayList<WItem> items = gui.getInventory().getItems(ores);
        new TransferItems(gui.getStockpile(), items, items.size()).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        new TakeItemsFromPile(gui.getStockpile()).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
