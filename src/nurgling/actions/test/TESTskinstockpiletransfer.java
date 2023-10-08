package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.*;

/*
* You need a pile of hides. There must be more hides in the pile than free space in the inventory
* */



public class TESTskinstockpiletransfer extends Test
{
    public static final NAlias hides = new NAlias(new ArrayList<>(Arrays.asList("Fur", "skin", "hide", "Scale" ,"scale")),
            new ArrayList<>(Arrays.asList("blood", "raw", "Fresh", "Jacket", "cape")));

    public TESTskinstockpiletransfer()
    {
        this.container = "Stockpile";
    }

    String container;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        ArrayList<WItem> items = gui.getInventory().getItems(hides);
        new TransferItems(gui.getStockpile(), items, items.size()).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

        new OpenTargetContainer(container, TestUtils.findGob("stockpile")).run(gui);
        new TakeItemsFromPile(gui.getStockpile(), new Coord(2, 2)).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
