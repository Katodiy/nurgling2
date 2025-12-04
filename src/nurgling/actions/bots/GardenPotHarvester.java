package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;

/**
 * Action that harvests ready plants from garden pots.
 * Pots are ready to harvest when they have 2 Equed overlays.
 */
public class GardenPotHarvester implements Action {

    private final NArea targetArea;
    private final NContext externalContext;

    public GardenPotHarvester(NArea area, NContext context) {
        this.targetArea = area;
        this.externalContext = context;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        int totalHarvested = 0;

        // Outer loop: keep harvesting until all ready pots are done
        while (true) {
            // Navigate to pots area
            externalContext.getAreaById(targetArea.id);

            // Find pots ready for harvest (re-check each iteration)
            ArrayList<Gob> allPots = Finder.findGobs(targetArea, GardenPotUtils.GARDEN_POT);
            ArrayList<Gob> readyPots = GardenPotUtils.filterPotsReadyToHarvest(allPots);

            if (readyPots.isEmpty()) {
                break;
            }

            gui.msg("Found " + readyPots.size() + " pots ready for harvest");

            // Harvest from each ready pot
            for (Gob pot : readyPots) {
                // Check inventory space - need at least 2 free slots
                if (gui.getInventory().getFreeSpace() < 2) {
                    gui.msg("Inventory nearly full, dropping off items...");
                    new FreeInventory2(externalContext).run(gui);
                    // Return to pot area
                    externalContext.getAreaById(targetArea.id);
                }

                // Re-check pot state (it might have changed)
                Gob currentPot = Finder.findGob(pot.id);
                if (currentPot == null || !GardenPotUtils.isReadyToHarvest(currentPot)) {
                    continue;
                }

                Results result = harvestPot(gui, currentPot);
                if (result.IsSuccess()) {
                    totalHarvested++;
                }
            }
        }

        if (totalHarvested > 0) {
            gui.msg("Harvested " + totalHarvested + " plants");
        } else {
            gui.msg("No pots ready for harvest");
        }
        return Results.SUCCESS();
    }

    /**
     * Harvest a single pot using the "Pick" flower menu action.
     */
    private Results harvestPot(NGameUI gui, Gob pot) throws InterruptedException {
        // Navigate to pot
        PathFinder pf = new PathFinder(pot);
        pf.isHardMode = true;
        pf.run(gui);

        // Use Pick action from flower menu
        new SelectFlowerAction("Pick", pot).run(gui);

        // Wait for overlays to be removed (plant harvested)
        NUtils.getUI().core.addTask(new WaitOverlayRemoval(pot));

        return Results.SUCCESS();
    }

    // Task to wait until overlays are removed from pot (harvest complete)
    private static class WaitOverlayRemoval extends NTask {
        private final Gob pot;
        private int counter = 0;

        WaitOverlayRemoval(Gob pot) {
            this.pot = pot;
        }

        @Override
        public boolean check() {
            counter++;
            if (counter >= 100) return true;  // Timeout
            Gob currentPot = Finder.findGob(pot.id);
            if (currentPot == null) return true;
            // Harvest complete when overlay count drops below 2
            return GardenPotUtils.countEquedOverlays(currentPot) < 2;
        }
    }
}
