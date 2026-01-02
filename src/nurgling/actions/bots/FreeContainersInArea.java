package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainersInArea implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String workAreaId = context.createArea("Please, select area with piles or containers", Resource.loadsimg("baubles/inputArea"));
        Pair<Coord2d,Coord2d> area = context.getRCArea(workAreaId);
        ArrayList<Container> containers = new ArrayList<>();
        if(area!=null) {
            for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())))) {
                Container cand = new Container(sm, NContext.contcaps.get(sm.ngob.name), context.getAreaById(workAreaId));
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }
            if (!containers.isEmpty())
                new FreeContainers(containers).run(gui);
        }

        ArrayList<Gob> gobs;
        HashSet<String> targets = new HashSet<>();
        while(!(gobs = Finder.findGobs(area, new NAlias("stockpile"))).isEmpty())
        {
            for (Gob pile : gobs) {
                if(PathFinder.isAvailable(pile))
                {
                    Coord size = StockpileUtils.itemMaxSize.get(pile.ngob.name);
                    new PathFinder(pile).run(gui);
                    new OpenTargetContainer("Stockpile", pile).run(gui);
                    int target_size = 0;
                    while (Finder.findGob(pile.id) != null)
                    {
                        if (NUtils.getGameUI().getInventory().getNumberFreeCoord((size != null) ? size : new Coord(1, 1)) > 0)
                        {
                            NISBox spbox = gui.getStockpile();
                            if (spbox != null)
                            {
                                do
                                {
                                    if (Finder.findGob(pile.id) == null && target_size != 0)
                                    {
                                        break;
                                    }
                                    target_size = NUtils.getGameUI().getInventory().getNumberFreeCoord((size != null) ? size : new Coord(1, 1));
                                    if (target_size == 0)
                                    {
                                        new FreeInventory2(context).run(gui);
                                        context.navigateToAreaIfNeeded(workAreaId);
                                        targets.clear();
                                        if (Finder.findGob(pile.id) != null)
                                        {
                                            new PathFinder(pile).run(gui);
                                            new OpenTargetContainer("Stockpile", pile).run(gui);
                                        } else break;
                                    } else
                                    {
                                        TakeItemsFromPile tifp = new TakeItemsFromPile(pile, spbox, target_size);
                                        tifp.run(gui);
                                        for (NGItem item : tifp.newItems())
                                            targets.add((item).name());
                                    }
                                }
                                while (target_size != 0);
                            }
                        } else
                        {
                            new FreeInventory2(context).run(gui);
                            context.navigateToAreaIfNeeded(workAreaId);
                            if (Finder.findGob(pile.id) != null)
                            {
                                new PathFinder(pile).run(gui);
                                new OpenTargetContainer("Stockpile", pile).run(gui);
                            }
                        }
                        context.navigateToAreaIfNeeded(workAreaId);
                    }
                }
            }
        }
        new FreeInventory2(context).run(gui);


        return Results.SUCCESS();
    }


}
