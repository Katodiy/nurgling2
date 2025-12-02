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
    private static final NAlias MULCH = new NAlias("Mulch");
    private static final NAlias WATER = new NAlias("Water");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        // Register mulch as input item so TakeItems2 can find it
        context.addInItem("Mulch", null);

        // Phase 1: Fill mulch
        fillMulchPhase(gui, context);

        // Phase 2: Fill water
        Results waterResult = fillWaterPhase(gui, context);
        if (!waterResult.IsSuccess()) {
            return waterResult;
        }

        gui.msg("Garden pot filling complete!");
        return Results.SUCCESS();
    }

    private Results fillMulchPhase(NGameUI gui, NContext context) throws InterruptedException {
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

        gui.msg("Found " + allPots.size() + " garden pots to fill with mulch");

        // Get mulch from Take area
        int mulchInInventory = gui.getInventory().getItems(MULCH).size();
        if (mulchInInventory == 0) {
            Results getMulchResult = getMulchFromArea(gui, context);
            if (!getMulchResult.IsSuccess()) {
                return getMulchResult;
            }
        }

        // Navigate back to pots area after getting mulch
        context.getSpecArea(Specialisation.SpecName.plantingGardenPots);

        // Fill each pot with mulch until it stops accepting
        for (Gob pot : allPots) {
            fillPotWithMulch(gui, pot);

            // Check if we need more mulch
            if (gui.getInventory().getItems(MULCH).isEmpty()) {
                Results getMulchResult = getMulchFromArea(gui, context);
                if (!getMulchResult.IsSuccess()) {
                    gui.msg("Out of mulch");
                    return Results.SUCCESS(); // Not an error, we filled what we could
                }
                // Navigate back to pots area
                context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
            }
        }

        return Results.SUCCESS();
    }

    private Results fillPotWithMulch(NGameUI gui, Gob pot) throws InterruptedException {
        PathFinder pf = new PathFinder(pot);
        pf.isHardMode = true;
        pf.run(gui);

        while (true) {
            // Check if we have mulch
            ArrayList<WItem> mulchItems = gui.getInventory().getItems(MULCH);
            if (mulchItems.isEmpty()) {
                return Results.SUCCESS(); // Need to get more mulch
            }

            // Take mulch to hand
            NUtils.takeItemToHand(mulchItems.get(0));

            // Apply mulch to pot using dropsame
            NUtils.dropsame(pot);

            // Wait for hand to be free (max 100 frames)
            NUtils.getUI().core.addTask(new WaitHandFreeWithTimeout());

            // Check if mulch was consumed or pot is full
            if (gui.vhand != null) {
                // Mulch still in hand = pot is full
                NUtils.dropToInv();
                NUtils.getUI().core.addTask(new HandIsFree(gui.getInventory()));
                return Results.SUCCESS(); // This pot is done
            }
            // Mulch was consumed, continue adding more
        }
    }

    private Results getMulchFromArea(NGameUI gui, NContext context) throws InterruptedException {
        int freeSpace = gui.getInventory().getFreeSpace();
        if (freeSpace == 0) {
            return Results.ERROR("Inventory is full");
        }

        new TakeItems2(context, "Mulch", freeSpace).run(gui);

        if (gui.getInventory().getItems(MULCH).isEmpty()) {
            return Results.ERROR("No mulch available in Take areas");
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
