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

public class TestAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea cauldrons = NArea.findSpec(Specialisation.SpecName.boiler.toString());

        ArrayList<Container> containers = new ArrayList<>();
        for (Gob cm : Finder.findGobs(cauldrons, new NAlias("gfx/terobjs/cauldron"))) {
            Container cand = new Container();
            cand.gob = cm;
            cand.cap = "Cauldron";

            cand.initattr(Container.Space.class);
            cand.initattr(Container.FuelLvl.class);
            cand.initattr(Container.WaterLvl.class);
            cand.getattr(Container.WaterLvl.class).setMaxlvl(30);
            cand.getattr(Container.FuelLvl.class).setMaxlvl(4);
            cand.getattr(Container.FuelLvl.class).setFueltype("branch");

            containers.add(cand);
        }

        for(Container current_container: containers ) {
            new UseWorkStationNC(current_container.gob).run(gui);
            new OpenTargetContainer(current_container).run(gui);
        }
            new WaterToContainers(containers).run(gui);

//        ArrayList<Gob> lighted = new ArrayList<>();
//        for (Container cont : containers) {
//            lighted.add(cont.gob);
//        }
//
//        Results res = null;
//        while(res == null || res.IsSuccess()) {
//            NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 4));
//            Context icontext = new Context();
//            for(NArea area : NArea.findAllIn(new NAlias("Dough", "Unbaked"))) {
//                for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
//                    Container cand = new Container();
//                    cand.gob = sm;
//                    cand.cap = Context.contcaps.get(cand.gob.ngob.name);
//                    cand.initattr(Container.Space.class);
//                    cand.initattr(Container.TargetItems.class);
//                    cand.getattr(Container.TargetItems.class).addTarget("Dough");
//                    cand.getattr(Container.TargetItems.class).addTarget("Unbaked");
//                    icontext.icontainers.add(cand);
//                }
//            }
//            new FreeContainers(containers).run(gui);
//            res = new FillContainersFromAreas(containers, new NAlias("Dough", "Unbaked"), icontext).run(gui);
//            new FuelToContainers(containers).run(gui);
//            new LightGob(lighted, 4).run(gui);
//        }
        return Results.SUCCESS();
    }
}
