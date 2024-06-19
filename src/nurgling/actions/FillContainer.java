package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FillContainer implements Action
{
    ArrayList<Container> conts;
    Context context;
    NAlias transferedItems;
    Coord targetCoord = new Coord(1,1);
    NArea area;

    public FillContainer(ArrayList<Container> conts, Context context, NArea area, NAlias transferedItems) {
        this.conts = conts;
        this.context = context;
        this.area = area;
        this.transferedItems = transferedItems;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        for (Container cont : conts) {
//            while (cont.freeSpace != 0) {
//                if (gui.getInventory().getItems(transferedItems).isEmpty()) {
//                    int target_size = 0;
//                    if (targetCoord.equals(1, 1)) {
//                        for (Context.Container tcont : conts) {
//                            target_size += tcont.freeSpace;
//                        }
//                    }
//                    for (String name : transferedItems.keys) {
//                        ArrayList<Context.Input> in = context.getInputs(name);
//                        if (in != null && !in.isEmpty()) {
//                            //TODO take from inputs
//                        }
//                    }
//
//                    if (gui.getInventory().getItems(transferedItems).isEmpty() && area != null) {
//                        while (target_size != 0 && gui.getInventory().getNumberFreeCoord(targetCoord) > 0) {
//                            ArrayList<Gob> piles = Finder.findGobs(area, new NAlias("stockpile"));
//                            if (piles.isEmpty())
//                                break;
//                            piles.sort(NUtils.d_comp);
//                            Gob pile = piles.get(0);
//                            new PathFinder(pile).run(gui);
//                            new OpenTargetContainer("Stockpile", pile).run(gui);
//                            TakeItemsFromPile tifp;
//                            (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(target_size, gui.getInventory().getFreeSpace()))).run(gui);
//                            new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
//                            target_size = target_size - tifp.getResult();
//                        }
//                    }
//                }
//
//                new PathFinder(cont.gob).run(gui);
//                new OpenTargetContainer(cont.cap, cont.gob).run(gui);
//                new TransferToContainer(context, cont, transferedItems ).run(gui);
//            }
        }
        return Results.SUCCESS();
    }
}
