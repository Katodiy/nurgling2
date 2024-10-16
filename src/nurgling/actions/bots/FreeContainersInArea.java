package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.FreeContainers;
import nurgling.actions.Results;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class FreeContainersInArea implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        SelectArea insa;
        NUtils.getGameUI().msg("Please, select input area");
        (insa = new SelectArea()).run(gui);
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
            if (containers.isEmpty())
                return Results.ERROR("No containers in area");
        }

        new FreeContainers(containers).run(gui);

        return Results.SUCCESS();
    }


}
