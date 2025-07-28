package nurgling.actions.bots;

import haven.Gob;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

public class CurdingTubUnloader implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NContext context = new NContext(gui);

        Specialisation.SpecName curdingTubSpecName = Specialisation.SpecName.curdingTub;

        NArea.Specialisation curdingTubSpecialization = new NArea.Specialisation(curdingTubSpecName.toString());

//        ArrayList<NArea.Specialisation> req = new ArrayList<>();
//        req.add(curdingTubSpecialization);
//        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        NArea goatsCurdOutputArea = NContext.findOutGlobal("Goat's Curd", 1, gui);
        NArea cowsCurdOutputArea = NContext.findOutGlobal("Cow's Curd", 1, gui);
        NArea sheepsCurdOutputArea = NContext.findOutGlobal("Sheep's Curd", 1, gui);
        NArea curdingTubArea = NContext.findSpecGlobal(curdingTubSpecialization);

        if(curdingTubArea == null) {
            return Results.ERROR("Curding Tub specialization area required, but not found!");
        }

        if(goatsCurdOutputArea == null || cowsCurdOutputArea == null || sheepsCurdOutputArea == null) {
            return Results.ERROR("PUT Area for Curds required, but not found!");
        }

//        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            context.getSpecArea(curdingTubSpecName);

            ArrayList<Gob> curdingTubs = Finder.findGobs(NContext.findSpec(curdingTubSpecialization), new NAlias("gfx/terobjs/curdingtub"));

            ArrayList<Container> containers = new ArrayList<>();
            for (Gob curdingTub : curdingTubs) {
                Container cand = new Container(curdingTub, "Curding Tub");
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }

            new FreeContainers(containers, new NAlias("Curd")).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
//        }

//        NUtils.stackSwitch(oldStackingValue);
//        return Results.FAIL();
    }
}
