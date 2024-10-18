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

import java.util.*;

public class SmelterAction implements Action {



    static NAlias ores = new NAlias ( new ArrayList<> (
            Arrays.asList ( "Cassiterite", "Lead Glance", "Wine Glance", "Chalcopyrite", "Malachite", "Peacock Ore", "Cinnabar", "Heavy Earth", "Iron Ochre",
                    "Bloodstone", "Black Ore", "Galena", "Silvershine", "Horn Silver", "Direvein", "Schrifterz", "Leaf Ore", "Meteorite", "Dross") ) );


    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation ofuelc = new NArea.Specialisation(Specialisation.SpecName.fuel.toString(), "coal");
        NArea.Specialisation ofuelb = new NArea.Specialisation(Specialisation.SpecName.fuel.toString(), "branch");
        NArea.Specialisation rsmelter = new NArea.Specialisation(Specialisation.SpecName.smelter.toString());
        NArea.Specialisation rore = new NArea.Specialisation(Specialisation.SpecName.ore.toString());
        NArea.Specialisation omercury = new NArea.Specialisation(Specialisation.SpecName.barrel.toString(),"Quicksilver");

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rsmelter);
        req.add(rore);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        opt.add(ofuelb);
        opt.add(ofuelc);
        opt.add(omercury);

        if(new Validator(req, opt).run(gui).IsSuccess()) {

            NArea smelters = NArea.findSpec(Specialisation.SpecName.smelter.toString());
            Finder.findGobs(smelters, new NAlias("gfx/terobjs/smelter"));

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob sm : Finder.findGobs(smelters, new NAlias("gfx/terobjs/smelter"))) {
                Container cand = new Container();
                cand.gob = sm;
                cand.cap = ((sm.ngob.getModelAttribute() & 128) == 128) ? "Smith's Smelter" : "Ore Smelter";

                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(12);
                cand.getattr(Container.FuelLvl.class).setCredolvl(9);
                cand.getattr(Container.FuelLvl.class).setFueltype("coal");

                cand.initattr(Container.TargetItems.class);
                cand.getattr(Container.TargetItems.class).addTarget("Slag");
                cand.getattr(Container.TargetItems.class).addTarget("Quicksilver");
                containers.add(cand);
            }

            for (Gob sm : Finder.findGobs(smelters, new NAlias("gfx/terobjs/primsmelter"))) {
                Container cand = new Container();
                cand.gob = sm;
                cand.cap = "Furnace";

                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(30);
                cand.getattr(Container.FuelLvl.class).setCredolvl(24);
                cand.getattr(Container.FuelLvl.class).setFueltype("branch");

                cand.initattr(Container.TargetItems.class);
                cand.getattr(Container.TargetItems.class).addTarget("Slag");
                cand.getattr(Container.TargetItems.class).addTarget("Quicksilver");
                containers.add(cand);
            }

            ArrayList<Gob> lighted = new ArrayList<>();
            for (Container cont : containers) {
                lighted.add(cont.gob);

            }

            Results res = null;
            while (res == null || res.IsSuccess()) {
                NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 2));
                new FreeContainers(containers).run(gui);
                new CollectQuickSilver(containers).run(gui);
                new DropTargets(containers, new NAlias("Slag")).run(gui);
                res = new FillContainersFromPiles(containers, NArea.findSpec(Specialisation.SpecName.ore.toString()), ores).run(gui);
                if (!new FuelToContainers(containers).run(gui).IsSuccess())
                    return Results.ERROR("NO FUEL");
                if (!new LightGob(lighted, 2).run(gui).IsSuccess())
                    return Results.ERROR("I can't start a fire");
            }
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
