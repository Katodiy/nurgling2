package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.areas.NArea;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;


public class BarleyFarmer implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println(BotRegistry.listBots());

        BotRegistry.createBot("beetroot").run(gui);

        return Results.SUCCESS();
    }
}
