package nurgling.actions.test;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.actions.bots.SelectArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Collections.swap;


/*
* You need a pile of ore. There must be more ore in the pile than free space in the inventory (NO window close command)
* */

public class TESTfreeStockpilesAndTransfer extends Test
{
    public static final NAlias ores = new NAlias(new ArrayList<>(Arrays.asList("rit", "Ore", "ore")));

    NAlias transferedItems = new NAlias("flax");
    public TESTfreeStockpilesAndTransfer()
    {
        this.container = "Stockpile";
    }

    String container;
    Coord target_size = new Coord(1,1);
    SelectArea insa;
    SelectArea outsa;
    @Override
    protected void runAction() throws InterruptedException {

        NUtils.getGameUI().msg("Please, select input area");
        (insa = new SelectArea()).run(NUtils.getGameUI());
        NUtils.getGameUI().msg("Please, select output area");
        (outsa = new SelectArea()).run(NUtils.getGameUI());
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        while (true) {
            int tsize = NUtils.getGameUI().getInventory().getNumberFreeCoord(target_size);
            while (NUtils.getGameUI().getInventory().getNumberFreeCoord(target_size) != 0) {
                ArrayList<Gob> piles = Finder.findGobs(insa.getRCArea(), new NAlias("stockpile"));
                if (piles.isEmpty()) {
                    if (gui.getInventory().getItems(transferedItems).isEmpty()) {
                        swap();
                        return;
                    } else
                        break;
                }
                piles.sort(NUtils.d_comp);

                Gob pile = piles.get(0);
                new PathFinder(pile).run(gui);
                new OpenTargetContainer("Stockpile", pile).run(gui);
                TakeItemsFromPile tifp;
                (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(tsize, gui.getInventory().getFreeSpace()))).run(gui);
                new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                tsize = tsize - tifp.getResult();
            }
            new TransferToPiles(outsa.getRCArea(), transferedItems).run(gui);
        }
    }

    private void swap() {
        SelectArea tmp = insa;
        insa = outsa;
        outsa = tmp;
    }
}
