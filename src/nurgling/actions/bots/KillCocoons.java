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
        
        // Check if TAKE area for cocoons exists
        NArea takeArea = NContext.findInGlobal(COCOON_NAME);
        if (takeArea == null) {
            return Results.ERROR("TAKE Silkworm Cocoons area is required, but not found");
        }
        
        // Continue until no more cocoons in TAKE area
        while (true) {
            // Take cocoons from TAKE area (batch size limited by inventory space)
            int inventorySpace = gui.getInventory().getFreeSpace();
            if (inventorySpace < 1) {
                // Free inventory if too full
                new FreeInventory2(context).run(gui);
                inventorySpace = gui.getInventory().getFreeSpace();
            }
            
            context.addInItem(COCOON_NAME, null);
            
            // Take cocoons - limit batch size to prevent inventory overflow
            new TakeItems2(context, COCOON_NAME, inventorySpace-1).run(gui);
            
            // Check if we got any cocoons
            ArrayList<WItem> cocoons = gui.getInventory().getItems(COCOON_ALIAS);
            if (cocoons.isEmpty()) {
                // No more cocoons available, we're done
                break;
            }

            killAllCocoons(gui, cocoons);

            new FreeInventory2(context).run(gui);
        }
        
        return Results.SUCCESS();
    }
    
    private void killAllCocoons(NGameUI gui, ArrayList<WItem> cocoons) throws InterruptedException {

        for (WItem cocoon : cocoons) {
            new SelectFlowerAction("Kill", cocoon).run(gui);
        }
    }
}