package nurgling.actions.bots;

import haven.Coord;
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

public class Cheese implements Action {
    Coord sizeOfSingleTray = new Coord(1, 2);
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        Specialisation.SpecName cheeseRacksSpecializationName = Specialisation.SpecName.cheeseRacks;

        NArea.Specialisation cheezeRackSpecialization = new NArea.Specialisation(cheeseRacksSpecializationName.toString(), "Outside");

        NArea outsideCheeseRackArea = NContext.findSpecGlobal(cheezeRackSpecialization);

        if (outsideCheeseRackArea == null) {
            return Results.ERROR("Outside Cheese Racks specialization area required, but not found!");
        }

        context.getSpecArea(cheeseRacksSpecializationName, "Outside");

        ArrayList<Gob> cheeseRacks = Finder.findGobs(NContext.findSpec(cheezeRackSpecialization), new NAlias("gfx/terobjs/cheeserack"));

        int totalNumberOfTraysCanFit = 0;

        for (Gob cheeseRack : cheeseRacks) {
            Container cheeseRackContainer = new Container(cheeseRack, "Rack");
            new PathFinder(cheeseRack).run(gui);
            new OpenTargetContainer(cheeseRackContainer).run(gui);

            totalNumberOfTraysCanFit += gui.getInventory(cheeseRackContainer.cap).getNumberFreeCoord(sizeOfSingleTray);

            new CloseTargetContainer(cheeseRackContainer).run(gui);
        }

        System.out.println(totalNumberOfTraysCanFit);

        return null;
    }
}
