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
 * Bot that handles planting and harvesting in garden pots.
 * Works with areas that have "Planting Garden Pots" specialization
 * and subspecializations for specific plants (e.g., "Blueberry").
 */
public class GardenPotPlanter implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        // Find all areas with "Planting Garden Pots" specialization
        ArrayList<NArea> potAreas = NContext.findAllSpec(
            Specialisation.SpecName.plantingGardenPots.toString()
        );

        if (potAreas.isEmpty()) {
            return Results.ERROR("No Planting Garden Pots areas found. Please configure the specialization.");
        }

        gui.msg("Found " + potAreas.size() + " garden pot area(s)");

        // Process each area
        for (NArea area : potAreas) {
            String subtype = getAreaSubtype(area);
            gui.msg("Processing area: " + area.name + " (subtype: " + (subtype != null ? subtype : "none") + ")");

            Results result = processArea(gui, context, area, subtype);
            if (!result.IsSuccess()) {
                gui.msg("Warning: Failed to process area " + area.name);
                // Continue to next area
            }
        }

        gui.msg("Garden pot planting complete!");
        return Results.SUCCESS();
    }

    /**
     * Get the subspecialization (plant type) for an area.
     */
    private String getAreaSubtype(NArea area) {
        for (NArea.Specialisation spec : area.spec) {
            if (spec.name.equals(Specialisation.SpecName.plantingGardenPots.toString())) {
                return spec.subtype;
            }
        }
        return null;
    }

    /**
     * Process a single garden pot area.
     */
    private Results processArea(NGameUI gui, NContext context, NArea area, String plantType)
            throws InterruptedException {

        // Step 1: Harvest ready plants (pots with 2 Equed overlays)
        Results harvestResult = harvestReadyPlants(gui, context, area);
        if (!harvestResult.IsSuccess()) {
            gui.msg("Warning: Harvest phase had issues");
            // Continue anyway
        }

        // Step 2: Fill pots with soil and water using existing GardenPotFiller
        Results fillResult = new GardenPotFiller(area, context).run(gui);
        if (!fillResult.IsSuccess()) {
            gui.msg("Warning: Fill phase had issues");
            // Continue anyway, some pots may be ready
        }

        // Step 3: Plant in pots that are ready (marker = 3)
        if (plantType != null && !plantType.isEmpty()) {
            Results plantResult = plantInPots(gui, context, area, plantType);
            if (!plantResult.IsSuccess()) {
                return plantResult;
            }
        } else {
            gui.msg("No plant type specified for area, skipping planting");
        }

        return Results.SUCCESS();
    }

    /**
     * Harvest plants from pots that are ready (2 Equed overlays).
     */
    private Results harvestReadyPlants(NGameUI gui, NContext context, NArea area)
            throws InterruptedException {

        int totalHarvested = 0;

        // Outer loop: keep harvesting until all ready pots are done
        while (true) {
            // Navigate to pots area
            context.getAreaById(area.id);

            // Find pots ready for harvest (re-check each iteration)
            ArrayList<Gob> allPots = Finder.findGobs(area, GardenPotUtils.GARDEN_POT);
            ArrayList<Gob> readyPots = GardenPotUtils.filterPotsReadyToHarvest(allPots);

            if (readyPots.isEmpty()) {
                if (totalHarvested == 0) {
                    gui.msg("No pots ready for harvest in area " + area.name);
                }
                break;
            }

            gui.msg("Found " + readyPots.size() + " pots ready for harvest");

            // Harvest from each ready pot
            for (Gob pot : readyPots) {
                // Check inventory space - need at least 2 free slots
                if (gui.getInventory().getFreeSpace() < 2) {
                    gui.msg("Inventory nearly full, dropping off items...");
                    new FreeInventory2(context).run(gui);
                    // Return to pot area
                    context.getAreaById(area.id);
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

    /**
     * Plant items in pots that are ready (marker = 3).
     */
    private Results plantInPots(NGameUI gui, NContext context, NArea area, String plantType)
            throws InterruptedException {

        // Get the item alias for this plant type
        NAlias plantItem = new NAlias(plantType);

        int totalPlantsPlaced = 0;

        // Outer loop: keep fetching and planting until all pots are done or no more items
        while (true) {
            // Navigate to pots area
            context.getAreaById(area.id);

            // Find pots ready for planting (re-check each iteration)
            ArrayList<Gob> allPots = Finder.findGobs(area, GardenPotUtils.GARDEN_POT);
            ArrayList<Gob> readyPots = GardenPotUtils.filterPotsReadyForPlanting(allPots);

            if (readyPots.isEmpty()) {
                if (totalPlantsPlaced == 0) {
                    gui.msg("No pots ready for planting in area " + area.name);
                }
                break;
            }

            // Check if we have items to plant
            ArrayList<WItem> existingItems = gui.getInventory().getItems(plantItem);
            if (existingItems.isEmpty()) {
                // Fetch more items from seed area
                int itemsNeeded = readyPots.size();
                int freeSpace = gui.getInventory().getFreeSpace();
                int itemsToTake = Math.min(itemsNeeded, freeSpace);

                if (itemsToTake > 0) {
                    new TakeItems2(context, plantItem.getDefault(), itemsToTake,
                        Specialisation.SpecName.gardenPotSeeds, plantType).run(gui);
                }

                // Check if we got any items
                existingItems = gui.getInventory().getItems(plantItem);
                if (existingItems.isEmpty()) {
                    gui.msg("No more " + plantType + " available in seed areas");
                    break;
                }

                // Navigate back to pot area
                context.getAreaById(area.id);
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

        gui.msg("Planted " + totalPlantsPlaced + " " + plantType);
        return Results.SUCCESS();
    }

    /**
     * Plant a single item in a pot.
     */
    private Results plantInPot(NGameUI gui, Gob pot, NAlias plantItem) throws InterruptedException {
        // Check if we have items
        ArrayList<WItem> items = gui.getInventory().getItems(plantItem);
        if (items.isEmpty()) {
            return Results.FAIL();  // No items left
        }

        // Navigate to pot
        PathFinder pf = new PathFinder(pot);
        pf.isHardMode = true;
        pf.run(gui);

        // Take item to hand
        NUtils.takeItemToHand(items.get(0));

        // Wait for item to be in hand
        NUtils.getUI().core.addTask(new WaitHand());

        // Apply item to pot (right-click with item in hand)
        NUtils.dropsame(pot);

        // Wait for plant to appear (Equed overlay on pot)
        NUtils.getUI().core.addTask(new WaitPlantAppear(pot));

        return Results.SUCCESS();
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
            // Harvest complete when no more Equed overlays (or just 0-1 if replanting)
            return GardenPotUtils.countEquedOverlays(currentPot) < 2;
        }
    }
}
