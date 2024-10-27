package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.FindNInventory;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import space.dynomake.libretranslate.Language;
import space.dynomake.libretranslate.Translator;

import java.util.ArrayList;

public class TestAction implements Action {
    String cap = "Cauldron";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
//        Translator.setUrlApi("http://localhost:5000/translate");
//        System.out.println(Translator.translate(Language.ENGLISH, Language.CHINESE,"fuck your mommy!"));

        NArea cauldrons = NArea.findSpec(Specialisation.SpecName.boiler.toString());

        ArrayList<Container> containers = new ArrayList<>();
        for (Gob cm : Finder.findGobs(cauldrons, new NAlias("gfx/terobjs/cauldron"))) {
            Container cand = new Container();
            cand.gob = cm;
            cand.cap = cap;

            cand.initattr(Container.Space.class);
            cand.initattr(Container.FuelLvl.class);
            cand.initattr(Container.WaterLvl.class);
            cand.getattr(Container.WaterLvl.class).setMaxlvl(30);
            cand.getattr(Container.FuelLvl.class).setAbsMaxlvl(50);
            cand.getattr(Container.FuelLvl.class).setMaxlvl(20);
            cand.getattr(Container.FuelLvl.class).setFuelmod(5);
            cand.getattr(Container.FuelLvl.class).setFueltype("branch");

            containers.add(cand);
        }

        for(Container current_container: containers ) {
            new OpenTargetContainer(current_container).run(gui);
            new CloseTargetContainer(current_container).run(gui);
        }


        ArrayList<Gob> lighted = new ArrayList<>();
        for (Container cont : containers) {
            lighted.add(cont.gob);
        }

        Results res = null;
        while(res == null || res.IsSuccess()) {
            NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 2));
            Context icontext = new Context();
            for(NArea area : NArea.findAllIn(new NAlias("Ashes"))) {
                for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                    Container cand = new Container();
                    cand.gob = sm;
                    cand.cap = Context.contcaps.get(cand.gob.ngob.name);
                    cand.initattr(Container.Space.class);
                    cand.initattr(Container.TargetItems.class);
                    cand.getattr(Container.TargetItems.class).addTarget("Ashes");
                    icontext.icontainers.add(cand);
                }
            }
            new FreeContainers(containers).run(gui);
            res = new FillContainersFromAreas(containers, new NAlias("Ashes"), icontext).run(gui);

            ArrayList<Container> forFuel = new ArrayList<>();
            for(Container container: containers) {
                Container.Space space = container.getattr(Container.Space.class);
                if(!space.isEmpty())
                    forFuel.add(container);
            }

            new WaterToContainers(containers).run(gui);
            if(!new FuelToContainers(containers).run(gui).IsSuccess())
                return Results.ERROR("NO FUEL");

            ArrayList<Gob> flighted = new ArrayList<>();
            for (Container cont : forFuel) {
                flighted.add(cont.gob);
            }
            new LightGob(flighted, 2).run(gui);
        }
        return Results.SUCCESS();
    }
}
