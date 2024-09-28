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

public class FillContainersFromPiles implements Action
{
    ArrayList<Container> conts;
    NAlias transferedItems;
    NArea area;
    Coord targetCoord = new Coord(1,1);

    public FillContainersFromPiles(ArrayList<Container> conts, NArea area, NAlias transferedItems) {
        this.conts = conts;
        this.area = area;
        this.transferedItems = transferedItems;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        for (Container cont : conts) {
            Container.Space space = cont.getattr(Container.Space.class);
            while ((Integer)space.getRes().get(Container.Space.FREESPACE)!=0)
            {
                if (gui.getInventory().getItems(transferedItems).isEmpty()) {
                    int target_size = 0;
                    if (targetCoord.equals(1, 1)) {
                        for (Container tcont : conts) {
                            Container.Space tspace = tcont.getattr(Container.Space.class);
                            target_size += (Integer) tspace.getRes().get(Container.Space.FREESPACE);
                        }
                    }

                    while ( target_size!= 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(targetCoord)!=0) {
                        ArrayList<Gob> piles = Finder.findGobs(area, new NAlias("stockpile"));
                        if (piles.isEmpty()) {
                            if(gui.getInventory().getItems(transferedItems).isEmpty())
                                return Results.ERROR("no items");
                            else
                                break;
                        }
                        piles.sort(NUtils.d_comp);

                        Gob pile = piles.get(0);
                        new PathFinder(pile).run(gui);
                        new OpenTargetContainer("Stockpile", pile).run(gui);
                        TakeItemsFromPile tifp;
                        (tifp = new TakeItemsFromPile(pile, gui.getStockpile(), Math.min(target_size, gui.getInventory().getFreeSpace()))).run(gui);
                        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                        target_size = target_size - tifp.getResult();
                    }
                }
                new TransferToContainer(new Context(), cont, transferedItems).run(gui);
            }
            new CloseTargetContainer(cont).run(gui);
        }
        return Results.SUCCESS();
    }
}
