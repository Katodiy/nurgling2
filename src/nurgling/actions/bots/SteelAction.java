package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

public class SteelAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation ofuelb = new NArea.Specialisation(Specialisation.SpecName.fuel.toString(), "branch");
        NArea.Specialisation rsmelter = new NArea.Specialisation(Specialisation.SpecName.crucibles.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rsmelter);
        req.add(ofuelb);

        if (new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {

            NArea smelters = NArea.findSpec(Specialisation.SpecName.crucibles.toString());
            Finder.findGobs(smelters, new NAlias("gfx/terobjs/steelcrucible"));

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob sm : Finder.findGobs(smelters, new NAlias("gfx/terobjs/steelcrucible"))) {
                Container cand = new Container();
                cand.gob = sm;
                cand.cap = "Steelbox";

                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(15);
                cand.getattr(Container.FuelLvl.class).setAbsMaxlvl(18);
                cand.getattr(Container.FuelLvl.class).setFueltype("branch");

                containers.add(cand);
            }

            ArrayList<Gob> lighted = new ArrayList<>();
            for (Container cont : containers) {
                lighted.add(cont.gob);
            }

            if (containers.isEmpty())
                return Results.ERROR("NO CRUCIBLES");

            for (Container container : containers) {
                PathFinder pf = new PathFinder(container.gob);
                pf.isHardMode = true;
                pf.run(gui);
                new OpenTargetContainer(container).run(gui);
                new CloseTargetContainer(container).run(gui);
            }


            if (!new FuelToContainers(containers).run(gui).IsSuccess())
                return Results.ERROR("NO FUEL");

            ArrayList<Gob> flighted = new ArrayList<>();
            for (Container cont : containers) {
                flighted.add(cont.gob);
            }

            if (!new LightGob(flighted, 4).run(gui).IsSuccess())
                return Results.ERROR("I can't start a fire");
        }
        return Results.SUCCESS();
    }
}
