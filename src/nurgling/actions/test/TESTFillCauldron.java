package nurgling.actions.test;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class TESTFillCauldron implements Action {
    String cap = "Cauldron";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea cauldrons = NContext.findSpec(Specialisation.SpecName.boiler.toString());

        ArrayList<Container> containers = new ArrayList<>();
        for (Gob cm : Finder.findGobs(cauldrons, new NAlias("gfx/terobjs/cauldron"))) {
            Container cand = new Container(cm, cap);

            cand.initattr(Container.Space.class);
            cand.initattr(Container.FuelLvl.class);

            cand.initattr(Container.WaterLvl.class);
            cand.getattr(Container.WaterLvl.class).setMaxlvl(30);

            cand.getattr(Container.FuelLvl.class).setAbsMaxlvl(50);//cauldron has 50 fuel lvl
            cand.getattr(Container.FuelLvl.class).setFueltype("branch");
            cand.getattr(Container.FuelLvl.class).setMaxlvl(20);//we need 20
            cand.getattr(Container.FuelLvl.class).setFuelmod(5);//1 item of type "branch" = 5 fuel

            containers.add(cand);
        }

        for(Container current_container: containers ) {
            new OpenTargetContainer(current_container).run(gui);
            new CloseTargetContainer(current_container).run(gui);
        }
        new WaterToContainer(containers).run(gui);
        if(!new FuelToContainers(containers).run(gui).IsSuccess())
            return Results.ERROR("NO FUEL");
        return Results.SUCCESS();
    }
}