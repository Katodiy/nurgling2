package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class UnGardentPotAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea kilns = NArea.findSpec(Specialisation.SpecName.kiln.toString());

        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(kilns, new NAlias("gfx/terobjs/kiln"))) {
            Container cand = new Container();
            cand.gob = sm;
            cand.cap = "Kiln";

            cand.initattr(Container.Space.class);
            cand.initattr(Container.FuelLvl.class);
            cand.getattr(Container.FuelLvl.class).setMaxlvl(23);
            cand.getattr(Container.FuelLvl.class).setFueltype("branch");
            cand.initattr(Container.Tetris.class);
            Container.Tetris tetris = cand.getattr(Container.Tetris.class);
            ArrayList<Coord> coords = new ArrayList<>();
            coords.add(new Coord(2, 2));

            tetris.getRes().put(Container.Tetris.TARGET_COORD, coords);

            containers.add(cand);
        }

        ArrayList<Gob> lighted = new ArrayList<>();
        for (Container cont : containers) {
            lighted.add(cont.gob);
        }

        Context icontext = new Context();
        for(NArea area : NArea.findAllIn(new NAlias("Unfired Garden Pot"))) {
            for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                Container cand = new Container();
                cand.gob = sm;
                cand.cap = Context.contcaps.get(cand.gob.ngob.name);
                cand.initattr(Container.Space.class);
                cand.initattr(Container.TargetItems.class);
                cand.getattr(Container.TargetItems.class).addTarget("Unfired Garden Pot");
                icontext.icontainers.add(cand);
            }
        }

        Results res = null;
        while(res == null || res.IsSuccess()) {
            NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 1));
            new FreeKIlnGP(containers).run(gui);
            res = new FillContainersFromAreas(containers, new NAlias("Unfired Garden Pot"), icontext).run(gui);
            new FuelToContainers(containers).run(gui);
            new LightGob(lighted, 1).run(gui);
        }
        return Results.SUCCESS();
    }
}
