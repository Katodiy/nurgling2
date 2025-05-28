package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.routes.RoutePoint;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

public class FreeContainersInArea implements Action {
    RoutePoint closestRoutePoint = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        this.closestRoutePoint = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(gui);

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
                    int target_size = 0;
                    while (Finder.findGob(pile.id) != null)
                        if ( NUtils.getGameUI().getInventory().getNumberFreeCoord((size != null) ?size:new Coord(1,1)) > 0) {
                            NISBox spbox = gui.getStockpile();
                            if (spbox != null) {
                                do {
                                    if (Finder.findGob(pile.id) == null&&target_size!=0) {
                                        break;
                                    }
                                    target_size = NUtils.getGameUI().getInventory().getNumberFreeCoord((size != null) ?size:new Coord(1,1));
                                    if (target_size == 0) {
                                        new TransferItems(context, targets).run(gui);
                                        new RoutePointNavigator(this.closestRoutePoint).run(NUtils.getGameUI());
                                        targets.clear();
                                        if (Finder.findGob(pile.id) != null) {
                                            new PathFinder(pile).run(gui);
                                            new OpenTargetContainer("Stockpile", pile).run(gui);
                                        } else break;
                                    } else {
                                        TakeItemsFromPile tifp = new TakeItemsFromPile(pile, spbox, target_size);
                                        tifp.run(gui);
                                        for (NGItem item : tifp.newItems())
                                            targets.add((item).name());
                                    }
                                }
                                while (target_size!=0);
                            }
                        }
                    else
                        {
                            new TransferItems(context, targets).run(gui);
                            new RoutePointNavigator(this.closestRoutePoint).run(NUtils.getGameUI());
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
