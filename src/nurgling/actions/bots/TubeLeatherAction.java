package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.FillContainersFromPiles;
import nurgling.actions.FreeContainers;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class TubeLeatherAction implements Action {

    NAlias raw = new NAlias("Fresh");
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();

        for (Gob dframe : Finder.findGobs(NArea.findSpec(Specialisation.SpecName.dframe.toString()),
                new NAlias("gfx/terobjs/dframe"))) {
            Container cand = new Container();
            cand.gob = dframe;
            cand.cap = "Frame";

            cand.initattr(Container.Space.class);
            cand.initattr(Container.Tetris.class);
            Container.Tetris tetris = cand.getattr(Container.Tetris.class);
            ArrayList<Coord> coords = new ArrayList<>();

            coords.add(new Coord(2, 2));
            coords.add(new Coord(2, 1));
            coords.add(new Coord(1, 1));

            tetris.getRes().put(Container.Tetris.TARGET_COORD, coords);

            containers.add(cand);
        }


        new FreeContainers(containers).run(gui);
        new FillContainersFromPiles(containers, NArea.findSpec(Specialisation.SpecName.rawhides.toString()).getRCArea(), raw).run(gui);

        return Results.SUCCESS();
    }
}
