package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.*;

/*
* You need a pile of block (min 11 blocks).
* */

public class TESTblockstockpiletransferpacks extends Test
{
    public static final NAlias block = new NAlias("Block");

    public TESTblockstockpiletransferpacks()
    {
        this.container = "Stockpile";
    }

    String container;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        new TransferItems(gui.getStockpile(), gui.getInventory().getItems(block), 5).run(gui);
        new TransferItems(gui.getStockpile(), gui.getInventory().getItems(block), 5).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        new TakeItemsFromPile(gui.getStockpile(), new Coord(1, 2), 10).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
