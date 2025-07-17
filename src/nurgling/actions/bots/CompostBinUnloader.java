package nurgling.actions.bots;

import haven.Gob;
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

public class CompostBinUnloader implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean oldStackingValue = ((NInventory) NUtils.getGameUI().maininv).bundle.a;

        NContext context = new NContext(gui);

        Specialisation.SpecName compostBinSpecName = Specialisation.SpecName.compostBin;

        NArea.Specialisation compostBinSpecialization = new NArea.Specialisation(compostBinSpecName.toString());

        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(compostBinSpecialization);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        NArea compostOutputArea = NContext.findOut("Mulch", 1);

        if(compostOutputArea == null) {
            return Results.ERROR("PUT Area for Compost required, but not found!");
        }

        if (new Validator(req, opt).run(gui).IsSuccess()) {
            NUtils.stackSwitch(true);

            context.getSpecArea(compostBinSpecName);

            ArrayList<Gob> compostBins = Finder.findGobs(NContext.findSpec(compostBinSpecialization), new NAlias("gfx/terobjs/compostbin"));

            ArrayList<Container> containers = new ArrayList<>();
            for (Gob compostBin : compostBins) {
                Container cand = new Container(compostBin, "Compost Bin");
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }

            // Marker 7 is half way full
            new FillFluid(containers, NContext.findSpec(Specialisation.SpecName.swill.toString()).getRCArea(), new NAlias("swill"), 7).run(gui);

            new FreeContainers(containers, new NAlias("Mulch")).run(gui);

            NUtils.stackSwitch(oldStackingValue);
            return Results.SUCCESS();
        }

        NUtils.stackSwitch(oldStackingValue);
        return Results.FAIL();
    }
}
