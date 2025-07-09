package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.NWItem;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.NFlowerMenuIsClosed;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.VSpec;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class BranchAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NUtils.getGameUI().msg("Please select area with piles of blocks");
        SelectArea insa;
        (insa = new SelectArea(Resource.loadsimg("baubles/prepBlockP"))).run(gui);


        NUtils.getGameUI().msg("Please select area for output branches");
        SelectArea onsa;
        (onsa = new SelectArea(Resource.loadsimg("baubles/branchStart"))).run(gui);

        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;
        NUtils.stackSwitch(true);

        while(NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1, 2))!=0 && Finder.findGob(insa.getRCArea(), new NAlias("stockpile"))!=null) {
            int count = NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1, 2));
            count = Math.min(count, NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1, 1)));
            while (count != 0) {
                if (NUtils.getGameUI().getInventory().getNumberFreeCoord(new Coord(1, 2)) > 0) {
                    Gob pile = Finder.findGob(insa.getRCArea(), new NAlias("stockpile"));
                    if (pile == null) {
                        break;
                    }
                    new PathFinder(pile).run(gui);
                    new OpenTargetContainer("Stockpile", pile).run(gui);
                    TakeItemsFromPile tifp;
                    (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), 1)).run(gui);
                    count -= tifp.getResult();
                } else
                    break;
            }

            NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
            ArrayList<WItem> items = NUtils.getGameUI().getInventory().getItems("Block");
            for (WItem item : items) {
                new SelectFlowerAction("Split", (NWItem) item).run(gui);
            }
            new TransferToPiles(onsa.getRCArea(), new NAlias("Branch")).run(gui);
        }

        NUtils.stackSwitch(oldStackingValue);

        return Results.SUCCESS();
    }
}
