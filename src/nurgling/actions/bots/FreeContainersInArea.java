package nurgling.actions.bots;

import haven.*;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NISBox;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainersInArea implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        SelectArea insa;
        NUtils.getGameUI().msg("Please, select area with piles or containers");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);
        Pair<Coord2d,Coord2d> area = insa.getRCArea();
        ArrayList<Container> containers = new ArrayList<>();
        if(area!=null) {
            for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                Container cand = new Container();
                cand.gob = sm;
                cand.cap = Context.contcaps.get(cand.gob.ngob.name);
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }
            if (!containers.isEmpty())
                new FreeContainers(containers).run(gui);
        }

        ArrayList<Gob> gobs;
        HashSet<String> targets = new HashSet<>();
        Context context = new Context();
        while(!(gobs = Finder.findGobs(area, new NAlias("stockpile"))).isEmpty())
        {
            for (Gob pile : gobs) {
                if(PathFinder.isAvailable(pile)) {
                    Coord size = StockpileUtils.itemMaxSize.get(pile.ngob.name);
                    new PathFinder(pile).run(gui);
                    new OpenTargetContainer("Stockpile",pile).run(gui);
                    while (Finder.findGob(pile.id) != null)
                        if ((size != null ? NUtils.getGameUI().getInventory().getNumberFreeCoord(size) : NUtils.getGameUI().getInventory().getFreeSpace()) > 0) {
                            NISBox spbox = gui.getStockpile();
                            if (spbox != null) {
                                int target_size = 0;
                                if (size == null) {
                                    int fs = NUtils.getGameUI().getInventory().getFreeSpace();
                                    target_size = Math.min(fs, spbox.calcCount());


                                } else {
                                    int fs = NUtils.getGameUI().getInventory().getNumberFreeCoord(size);
                                    target_size = Math.min(fs, spbox.calcCount());
                                }
                                if(target_size == 0) {
                                    new TransferItems(context, targets).run(gui);
                                    targets.clear();
                                    if(Finder.findGob(pile.id) != null) {
                                        new PathFinder(pile).run(gui);
                                        new OpenTargetContainer("Stockpile", pile).run(gui);
                                    }
                                    else break;
                                }
                                TakeItemsFromPile tifp = new TakeItemsFromPile(pile, spbox, target_size);
                                tifp.run(gui);
                                for(WItem item : tifp.newItems())
                                    targets.add(((NGItem)item.item).name());
                            }
                        }
                    else
                        {
                            new TransferItems(context, targets).run(gui);
                            if(Finder.findGob(pile.id) != null) {
                                new PathFinder(pile).run(gui);
                                new OpenTargetContainer("Stockpile", pile).run(gui);
                            }
                        }
                }
            }
        }
        new TransferItems(context, targets).run(gui);


        return Results.SUCCESS();
    }


}
