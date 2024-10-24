package nurgling.actions.bots;

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
import java.util.Arrays;

public class BackerAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation rfuelb = new NArea.Specialisation(Specialisation.SpecName.fuel.toString(), "branch");
        NArea.Specialisation rovens = new NArea.Specialisation(Specialisation.SpecName.ovens.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rovens);
        req.add(rfuelb);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess()) {


            NArea ovens = NArea.findSpec(Specialisation.SpecName.ovens.toString());

            ArrayList<Container> containers = new ArrayList<>();
            for (Gob sm : Finder.findGobs(ovens, new NAlias("gfx/terobjs/oven"))) {
                Container cand = new Container();
                cand.gob = sm;
                cand.cap = "Oven";

                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(4);
                cand.getattr(Container.FuelLvl.class).setFueltype("branch");

                containers.add(cand);
            }

            ArrayList<Gob> lighted = new ArrayList<>();
            for (Container cont : containers) {
                lighted.add(cont.gob);
            }

            Results res = null;
            while (res == null || res.IsSuccess()) {
                NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 4));
                Context icontext = new Context();
                for (NArea area : NArea.findAllIn(new NAlias("Dough", "Unbaked"))) {
                    for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                        Container cand = new Container();
                        cand.gob = sm;
                        cand.cap = Context.contcaps.get(cand.gob.ngob.name);
                        cand.initattr(Container.Space.class);
                        cand.initattr(Container.TargetItems.class);
                        cand.getattr(Container.TargetItems.class).addTarget("Dough");
                        cand.getattr(Container.TargetItems.class).addTarget("Unbaked");
                        icontext.icontainers.add(cand);
                    }
                }
                new FreeContainers(containers).run(gui);
                res = new FillContainersFromAreas(containers, new NAlias("Dough", "Unbaked"), icontext).run(gui);

                ArrayList<Container> forFuel = new ArrayList<>();
                for (Container container : containers) {
                    Container.Space space = container.getattr(Container.Space.class);
                    if (!space.isEmpty())
                        forFuel.add(container);
                }
                new FuelToContainers(forFuel).run(gui);

                ArrayList<Gob> flighted = new ArrayList<>();
                for (Container cont : forFuel) {
                    flighted.add(cont.gob);
                }
                new LightGob(flighted, 4).run(gui);
            }
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
