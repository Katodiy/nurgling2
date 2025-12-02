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

public class GardenPotFiller implements Action {

    private static final NAlias GARDEN_POT = new NAlias("gfx/terobjs/gardenpot");
    private static final NAlias SOIL = new NAlias("Soil", "Mulch");
    private static final NAlias WATER = new NAlias("Water");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        // Register soil/mulch as input items so TakeItems2 can find them
        context.addInItem("Soil", null);
        context.addInItem("Mulch", null);

        // Phase 1: Fill soil
        fillSoilPhase(gui, context);

        // Phase 2: Fill water
        Results waterResult = fillWaterPhase(gui, context);
        if (!waterResult.IsSuccess()) {
            return waterResult;
        }

        gui.msg("Garden pot filling complete!");
        return Results.SUCCESS();
    }

    private Results fillSoilPhase(NGameUI gui, NContext context) throws InterruptedException {
        // Navigate to pots area
        NArea potArea = context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
        if (potArea == null) {
            return Results.ERROR("No Planting Garden Pots area found. Please configure the specialization.");
        }

        // Get ALL pots - we try to fill each one, not filter by marker
        ArrayList<Gob> allPots = Finder.findGobs(potArea, GARDEN_POT);
        if (allPots.isEmpty()) {
            gui.msg("No garden pots found in area");
            return Results.FAIL();
        }

        gui.msg("Found " + allPots.size() + " garden pots to fill with soil");

        // Get soil from Take area
        int soilInInventory = gui.getInventory().getItems(SOIL).size();
        if (soilInInventory == 0) {
            Results getSoilResult = getSoilFromArea(gui, context);
            if (!getSoilResult.IsSuccess()) {
                return getSoilResult;
            }
        }

        // Navigate back to pots area after getting soil
        context.getSpecArea(Specialisation.SpecName.plantingGardenPots);

        // Fill each pot with soil until it stops accepting
        for (Gob pot : allPots) {
            fillPotWithSoil(gui, pot);

            // Check if we need more soil
            if (gui.getInventory().getItems(SOIL).isEmpty()) {
                Results getSoilResult = getSoilFromArea(gui, context);
                if (!getSoilResult.IsSuccess()) {
                    gui.msg("Out of soil");
                    return Results.SUCCESS(); // Not an error, we filled what we could
                }
                // Navigate back to pots area
                context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
            }
        }

        return Results.SUCCESS();
    }

    private Results fillPotWithSoil(NGameUI gui, Gob pot) throws InterruptedException {
        PathFinder pf = new PathFinder(pot);
        pf.isHardMode = true;
        pf.run(gui);

        while (true) {
            // Check if we have soil
            ArrayList<WItem> soilItems = gui.getInventory().getItems(SOIL);
            if (soilItems.isEmpty()) {
                return Results.SUCCESS(); // Need to get more soil
            }

            // Take soil to hand
            NUtils.takeItemToHand(soilItems.get(0));

            // Apply soil to pot using dropsame
            NUtils.dropsame(pot);

            // Wait for hand to be free (max 100 frames)
            NUtils.getUI().core.addTask(new WaitHandFreeWithTimeout());

            // Check if soil was consumed or pot is full
            if (gui.vhand != null) {
                // Soil still in hand = pot is full
                NUtils.dropToInv();
                NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
                return Results.SUCCESS(); // This pot is done
            }
            // Soil was consumed, continue adding more
        }
    }

    private Results getSoilFromArea(NGameUI gui, NContext context) throws InterruptedException {
        int freeSpace = gui.getInventory().getFreeSpace();
        if (freeSpace == 0) {
            return Results.ERROR("Inventory is full");
        }

        // Try to take Soil first, then Mulch
        new TakeItems2(context, "Soil", freeSpace).run(gui);
        if (gui.getInventory().getItems(SOIL).isEmpty()) {
            new TakeItems2(context, "Mulch", freeSpace).run(gui);
        }

        if (gui.getInventory().getItems(SOIL).isEmpty()) {
            return Results.ERROR("No soil/mulch available in Take areas");
        }

        return Results.SUCCESS();
    }

    private Results fillWaterPhase(NGameUI gui, NContext context) throws InterruptedException {
        // Navigate to pots area to get the list of pots
        NArea potArea = context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
        if (potArea == null) {
            return Results.ERROR("No Planting Garden Pots area found");
        }

        // Get ALL pots
        ArrayList<Gob> allPots = Finder.findGobs(potArea, GARDEN_POT);
        if (allPots.isEmpty()) {
            gui.msg("No garden pots found");
            return Results.SUCCESS();
        }

        gui.msg("Filling " + allPots.size() + " garden pots with water");

        // Use FillFluid with the new garden pot constructor (no mask)
        FillFluid fillFluid = new FillFluid(allPots, context, Specialisation.SpecName.plantingGardenPots, WATER);
        return fillFluid.run(gui);
    }

    // Task to wait for hand to be free with 100 frame timeout
    private static class WaitHandFreeWithTimeout extends NTask {
        private int counter = 0;

        @Override
        public boolean check() {
            counter++;
            if (counter >= 100) {
                return true;
            }
            return NUtils.getGameUI().vhand == null;
        }
    }
}
