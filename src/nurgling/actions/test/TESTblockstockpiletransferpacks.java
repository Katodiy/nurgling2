package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

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
        Gob pile = TestUtils.findGob("stockpile");
        new OpenTargetContainer(container, pile).run(gui);
        new TransferItemsOLD(gui.getStockpile(), gui.getInventory().getItems(block), 5).run(gui);
        new TransferItemsOLD(gui.getStockpile(), gui.getInventory().getItems(block), 5).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        new TakeItemsFromPile(pile, gui.getStockpile(), new Coord(1, 2), 10).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
