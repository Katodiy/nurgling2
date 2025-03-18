package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.VSpec;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class BlockAshAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation rBoneForAsh = new NArea.Specialisation(Specialisation.SpecName.blockforash.toString());
        NArea.Specialisation rkilns = new NArea.Specialisation(Specialisation.SpecName.kiln.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rkilns);
        req.add(rBoneForAsh);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        if(new Validator(req, opt).run(gui).IsSuccess()) {

            Pair<Coord2d,Coord2d> rca = NArea.findSpec(Specialisation.SpecName.blockforash.toString()).getRCArea();
            if(rca==null)
            {
                return Results.ERROR("Input area not found");
            }

            Gob testGob;
            boolean isBoard = false;
            if((testGob = Finder.findGob(rca,new NAlias("stockpile")))!=null)
            {
               isBoard = testGob.ngob.name.contains("board");
            }

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob kiln : Finder.findGobs(NArea.findSpec(rkilns.name),
                    new NAlias("gfx/terobjs/kiln"))) {
                Container cand = new Container();
                cand.gob = kiln;
                cand.cap = "Kiln";
                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(isBoard ? 3 : 8);
                cand.getattr(Container.FuelLvl.class).setFueltype("Branch");
                cand.initattr(Container.Tetris.class);
                Container.Tetris tetris = cand.getattr(Container.Tetris.class);
                ArrayList<Coord> coords = new ArrayList<>();
                if(isBoard)
                {
                    coords.add(new Coord(4, 1));
                }
                else {
                    coords.add(new Coord(1, 2));
                }

                tetris.getRes().put(Container.Tetris.TARGET_COORD, coords);
                containers.add(cand);
            }

            ArrayList<Gob> flighted = new ArrayList<>();
            for (Container cont : containers) {
                flighted.add(cont.gob);
            }

            Results res = null;
            while (res == null || res.IsSuccess()) {
                NUtils.getUI().core.addTask(new WaitForBurnout(flighted, 1));
                new FreeContainers(containers).run(gui);
                res = new FillContainersFromPiles(containers, rca, isBoard?new NAlias("Board"):new NAlias("Block")).run(gui);

                ArrayList<Container> forFuel = new ArrayList<>();
                for (Container container : containers) {
                    Container.Space space = container.getattr(Container.Space.class);
                    if (!space.isEmpty())
                        forFuel.add(container);
                }
                new FuelToContainers(forFuel).run(gui);

                flighted.clear();
                for (Container cont : forFuel) {
                    flighted.add(cont.gob);
                }
                if (!new LightGob(flighted, 1).run(gui).IsSuccess())
                    return Results.ERROR("I can't start a fire");
            }
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
