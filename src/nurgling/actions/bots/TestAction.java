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

import java.util.ArrayList;

public class TestAction implements Action {
    String cap = "Cauldron";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
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
            new SelectFlowerAction("Open", current_container.gob, true).run(gui);
            //TODO: change SelectFlowerAction to OpenContainer
            NUtils.getUI().core.addTask(new FindNInventory(cap));
            new CloseTargetContainer(current_container).run(gui);
            //gui.msg(Boolean.toString((current_container.gob.ngob.getModelAttribute() & 4) == 0));
        }
        new WaterToContainers(containers).run(gui);
        if(!new FuelToContainers(containers).run(gui).IsSuccess())
            return Results.ERROR("NO FUEL");
        //LightGob with getAttribute
        return Results.SUCCESS();
    }
}
