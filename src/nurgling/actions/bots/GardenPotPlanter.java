package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Action that plants items in garden pots that are ready (marker = 3, no plant).
 * Fetches items from Garden Pot Seeds areas as needed.
 */
public class GardenPotPlanter implements Action {

    private final NArea targetArea;
    private final NContext externalContext;
    private final String plantType;

    public GardenPotPlanter(NArea area, NContext context, String plantType) {
        this.targetArea = area;
        this.externalContext = context;
        this.plantType = plantType;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (plantType == null || plantType.isEmpty()) {
            gui.msg("No plant type specified, skipping planting");
            return Results.SUCCESS();
        }

        // Get the item alias for this plant type
        NAlias plantItem = new NAlias(plantType);

        int totalPlantsPlaced = 0;

        // Outer loop: keep fetching and planting until all pots are done or no more items
        while (true) {
            // Navigate to pots area
            externalContext.getAreaById(targetArea.id);

            // Find pots ready for planting (re-check each iteration)
            ArrayList<Gob> allPots = Finder.findGobs(targetArea, GardenPotUtils.GARDEN_POT);
            ArrayList<Gob> readyPots = GardenPotUtils.filterPotsReadyForPlanting(allPots);

            if (readyPots.isEmpty()) {
                break;
            }

            // Check if we have items to plant (in inventory or hand)
            ArrayList<WItem> existingItems = gui.getInventory().getItems(plantItem);
            boolean itemInHand = isItemInHand(gui, plantItem);

            if (existingItems.isEmpty() && !itemInHand) {
                // Fetch more items from seed area
                int itemsNeeded = readyPots.size();
                int freeSpace = gui.getInventory().getFreeSpace();
                int itemsToTake = Math.min(itemsNeeded, freeSpace);

                if (itemsToTake > 0) {
                    new TakeItems2(externalContext, plantItem.getDefault(), itemsToTake,
                        Specialisation.SpecName.gardenPotSeeds, plantType).run(gui);
                }

                // Check if we got any items
                existingItems = gui.getInventory().getItems(plantItem);
                if (existingItems.isEmpty()) {
                    gui.msg("No more " + plantType + " available in seed areas");
                    break;
                }

                // Navigate back to pot area
                externalContext.getAreaById(targetArea.id);
            }

            // Plant in ready pots until we run out of items
            for (Gob pot : readyPots) {
                // Re-check pot state (it might have changed)
                Gob currentPot = Finder.findGob(pot.id);
                if (currentPot == null || !GardenPotUtils.isReadyForPlanting(currentPot)) {
                    continue;
                }

                Results result = plantInPot(gui, currentPot, plantItem);
                if (!result.IsSuccess()) {
                    // Out of items, break inner loop to fetch more
                    break;
                }
                totalPlantsPlaced++;
            }
        }

        if (totalPlantsPlaced > 0) {
            gui.msg("Planted " + totalPlantsPlaced + " " + plantType);
        } else {
            gui.msg("No pots ready for planting");
        }
        return Results.SUCCESS();
    }

    /**
     * Plant a single item in a pot.
     */
    private Results plantInPot(NGameUI gui, Gob pot, NAlias plantItem) throws InterruptedException {
        // Check if we have items (in inventory or already in hand)
        ArrayList<WItem> items = gui.getInventory().getItems(plantItem);
        boolean itemInHand = isItemInHand(gui, plantItem);

        if (items.isEmpty() && !itemInHand) {
            return Results.FAIL();  // No items left
        }

        // Navigate to pot
        PathFinder pf = new PathFinder(pot);
        pf.isHardMode = true;
        pf.run(gui);

        // Take item to hand only if not already there
        if (!itemInHand) {
            NUtils.takeItemToHand(items.get(0));
            // Wait for item to be in hand
            NUtils.getUI().core.addTask(new WaitHand());
        }

        // Apply item to pot (right-click with item in hand)
        NUtils.dropsame(pot);

        // Wait for plant to appear (Equed overlay on pot)
        NUtils.getUI().core.addTask(new WaitPlantAppear(pot));

        return Results.SUCCESS();
    }

    /**
     * Check if the specified item type is currently in hand.
     */
    private boolean isItemInHand(NGameUI gui, NAlias itemAlias) {
        if (gui.vhand == null || gui.vhand.item == null) {
            return false;
        }
        String handItemName = ((NGItem) gui.vhand.item).name();
        return handItemName != null && NParser.checkName(handItemName, itemAlias);
    }

    // Task to wait until hand has an item
    private static class WaitHand extends NTask {
        private int counter = 0;

        @Override
        public boolean check() {
            counter++;
            if (counter >= 100) return true;  // Timeout
            return NUtils.getGameUI().vhand != null;
        }
    }

    // Task to wait until plant appears on pot (Equed overlay)
    private static class WaitPlantAppear extends NTask {
        private final Gob pot;
        private int counter = 0;

        WaitPlantAppear(Gob pot) {
            this.pot = pot;
        }

        @Override
        public boolean check() {
            counter++;
            if (counter >= 100) return true;  // Timeout
            Gob currentPot = Finder.findGob(pot.id);
            if (currentPot == null) return true;
            return GardenPotUtils.hasPlant(currentPot);
        }
    }
}
