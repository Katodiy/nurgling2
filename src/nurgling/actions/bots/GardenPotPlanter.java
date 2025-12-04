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
import java.util.HashMap;
import java.util.Map;

/**
 * Bot that handles planting and harvesting in garden pots.
 * Works with areas that have "Planting Garden Pots" specialization
 * and subspecializations for specific plants (e.g., "Blueberries").
 */
public class GardenPotPlanter implements Action {

    // Plant type mappings - subspecialization name to item alias
    private static final Map<String, NAlias> PLANT_ITEMS = new HashMap<>();
    static {
        PLANT_ITEMS.put("Blueberries", new NAlias("Blueberry"));
        PLANT_ITEMS.put("Lingonberries", new NAlias("Lingonberry"));
        PLANT_ITEMS.put("Strawberries", new NAlias("Strawberry"));
        // Add more plant types here as needed
    }

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

        // Step 1: Harvest ready plants (STUB - to be implemented when we have planted pots)
        // Results harvestResult = harvestReadyPlants(gui, context, area, plantType);

        // Step 2: Fill pots with soil and water using existing GardenPotFiller
        Results fillResult = new GardenPotFiller().run(gui);
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
     * Plant items in pots that are ready (marker = 3).
     */
    private Results plantInPots(NGameUI gui, NContext context, NArea area, String plantType)
            throws InterruptedException {

        // Get the item alias for this plant type
        NAlias plantItem = new NAlias(plantType);
        if (plantItem == null) {
            gui.msg("Unknown plant type: " + plantType);
            return Results.SUCCESS();
        }

        // Navigate to pots area
        context.getAreaById(area.id);

        // Find pots ready for planting
        ArrayList<Gob> allPots = Finder.findGobs(area, GardenPotUtils.GARDEN_POT);
        ArrayList<Gob> readyPots = GardenPotUtils.filterPotsReadyForPlanting(allPots);

        if (readyPots.isEmpty()) {
            gui.msg("No pots ready for planting in area " + area.name);
            return Results.SUCCESS();
        }

        gui.msg("Found " + readyPots.size() + " pots ready for planting");

        // Check if we have any items to plant
        ArrayList<WItem> existingItems = gui.getInventory().getItems(plantItem);
        if (existingItems.isEmpty()) {
            // Try to get items from seed area
            NArea seedArea = NContext.findSpec(
                Specialisation.SpecName.gardenPotSeeds.toString(),
                plantType
            );
            if (seedArea == null) {
                seedArea = NContext.findSpecGlobal(
                    Specialisation.SpecName.gardenPotSeeds.toString(),
                    plantType
                );
            }

            if (seedArea == null) {
                gui.msg("No Garden Pot Seeds area found for " + plantType);
                return Results.ERROR("No Garden Pot Seeds area with subtype " + plantType);
            }

            // Take items to inventory
            int itemsNeeded = readyPots.size();
            int freeSpace = gui.getInventory().getFreeSpace();
            int itemsToTake = Math.min(itemsNeeded, freeSpace);

            if (itemsToTake > 0) {
                new TakeItems2(context, plantItem.getDefault(), itemsToTake,
                    Specialisation.SpecName.gardenPotSeeds, plantType).run(gui);
            }

            // Navigate back to pot area
            context.getAreaById(area.id);
        }

        // Plant in each ready pot
        int plantsPlaced = 0;
        for (Gob pot : readyPots) {
            // Re-check pot state (it might have changed)
            Gob currentPot = Finder.findGob(pot.id);
            if (currentPot == null || !GardenPotUtils.isReadyForPlanting(currentPot)) {
                continue;
            }

            Results result = plantInPot(gui, currentPot, plantItem);
            if (!result.IsSuccess()) {
                // Out of items, need more
                gui.msg("Out of " + plantType + " to plant");
                break;
            }
            plantsPlaced++;
        }

        gui.msg("Planted " + plantsPlaced + " " + plantType);
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
}
