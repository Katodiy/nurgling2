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
            new CloseTargetContainer(current_container).run(gui);
            gui.msg(Boolean.toString((current_container.gob.ngob.getModelAttribute() & 4) == 0));
        }
        new WaterToContainers(containers).run(gui);
        //FuelToCOntainers
        //LightGob with getAttribute
        return Results.SUCCESS();
    }
}
