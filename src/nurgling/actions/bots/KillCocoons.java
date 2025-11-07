package nurgling.actions.bots;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Bot that takes Silkworm Cocoons from TAKE area, kills them all, and stores the results.
 * Continues until the TAKE area containers are empty.
 */
public class KillCocoons implements Action {
    private static final String COCOON_NAME = "Silkworm Cocoon";
    private static final NAlias COCOON_ALIAS = new NAlias(COCOON_NAME);
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        // Check if we got any cocoons
        ArrayList<WItem> cocoons = gui.getInventory().getItems(COCOON_ALIAS);
        if (cocoons.isEmpty()) {
            return Results.SUCCESS();
        }

        killAllCocoons(gui, cocoons);

        new FreeInventory2(context).run(gui);
        
        return Results.SUCCESS();
    }
    
    private void killAllCocoons(NGameUI gui, ArrayList<WItem> cocoons) throws InterruptedException {
        for (WItem cocoon : cocoons) {
            new SelectFlowerAction("Kill", cocoon).run(gui);
        }
    }
}