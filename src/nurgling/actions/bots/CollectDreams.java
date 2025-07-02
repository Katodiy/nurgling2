package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

public class CollectDreams implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Context context = new Context();

        for (Gob dreamCatcher : Finder.findGobs(NContext.findSpec(Specialisation.SpecName.dreamcatcher.toString()),
                new NAlias("gfx/terobjs/dreca"))) {
            new PathFinder(dreamCatcher).run(gui);
            Results harvestResult;
            int harvestAttempt = 0;
            do {
                harvestAttempt++;
                harvestResult = new SelectFlowerAction("Harvest", dreamCatcher).run(gui);
                if(harvestAttempt == 2) {
                    break;
                }
            } while (harvestResult.isSuccess);

            if(NUtils.getGameUI().getInventory().getFreeSpace()<3) {
                new FreeInventory(context).run(gui);
            }
        }

        new FreeInventory(context).run(gui);

        return Results.SUCCESS();
    }
}
