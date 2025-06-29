package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

public class FFAction implements Action {




    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation rfuelc = new NArea.Specialisation(Specialisation.SpecName.fuel.toString(), "coal");
        NArea.Specialisation rfforge = new NArea.Specialisation(Specialisation.SpecName.fforge.toString());
        NArea.Specialisation ranvil = new NArea.Specialisation(Specialisation.SpecName.anvil.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(rfforge);
        req.add(rfuelc);
        req.add(ranvil);

        if(new Validator(req, new ArrayList<>()).run(gui).IsSuccess()) {

            NArea fforges = NContext.findSpec(Specialisation.SpecName.fforge.toString());

            ArrayList<Container> containers = new ArrayList<>();

            for (Gob sm : Finder.findGobs(fforges, new NAlias("gfx/terobjs/fineryforge"))) {
                Container cand = new Container(sm,"Finery Forge" );

                cand.initattr(Container.Space.class);
                cand.initattr(Container.FuelLvl.class);
                cand.getattr(Container.FuelLvl.class).setMaxlvl(2);
                cand.getattr(Container.FuelLvl.class).setFueltype("coal");
                cand.initattr(Container.TargetItems.class);
                cand.getattr(Container.TargetItems.class).addTarget("Dross");
                containers.add(cand);
            }


            ArrayList<Long> lighted = new ArrayList<>();
            for (Container cont : containers) {
                lighted.add(cont.gobid);

            }
            if(containers.isEmpty())
                return Results.ERROR("NO Forges");

            Results res = null;
            NContext context = new NContext(gui);
            context.workstation = new NContext.Workstation("gfx/terobjs/anvil", null);
            context.workstation.selected = Finder.findGob(NContext.findSpec(Specialisation.SpecName.anvil.toString()), new NAlias("anvil")).id;
            while (res == null || res.IsSuccess()) {
                NUtils.getUI().core.addTask(new WaitForBurnout(lighted, 8));
                synchronized (NUtils.getGameUI()) {
                    new FreeContainers(containers).run(gui);

                    new DropTargets(containers, new NAlias("Dross")).run(gui);
                    new Forging(containers, context).run(gui);
                    new FreeInventory2(context).run(gui);
                    res = new FillContainers(containers, "Bar of Cast Iron", new Context()).run(gui);
                    ArrayList<Container> forFuel = new ArrayList<>();

                    for (Container container : containers) {
                        Container.Space space = container.getattr(Container.Space.class);
                        if (!space.isEmpty())
                            forFuel.add(container);
                    }

                    if (!new FuelToContainers(forFuel).run(gui).IsSuccess())
                        return Results.ERROR("NO FUEL");

                    ArrayList<Long> flighted = new ArrayList<>();
                    for (Container cont : forFuel) {
                        flighted.add(cont.gobid);
                    }

                    if (!new LightGob(flighted, 8).run(gui).IsSuccess())
                        return Results.ERROR("I can't start a fire");
                }
            }
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }
}
