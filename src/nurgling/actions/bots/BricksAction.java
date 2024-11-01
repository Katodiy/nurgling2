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

public class BricksAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NAlias clays = VSpec.getNamesInCategory("Clay");
        NArea.Specialisation rkilns = new NArea.Specialisation(Specialisation.SpecName.kiln.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rkilns);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        if(new Validator(req, opt).run(gui).IsSuccess()) {

            NArea npile_area = NArea.findIn(clays);
            Pair<Coord2d,Coord2d> pile_area = npile_area!=null?npile_area.getRCArea():null;
            if(pile_area==null)
            {
                return Results.ERROR("Clay not found");
            }

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob kiln : Finder.findGobs(NArea.findSpec(rkilns.name),
                    new NAlias("gfx/terobjs/kiln"))) {
                Container cand = new Container();
                cand.gob = kiln;
                cand.cap = "Kiln";
                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(2);
                cand.getattr(Container.FuelLvl.class).setFueltype("branch");
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
                res = new FillContainersFromPiles(containers, pile_area, clays).run(gui);

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
