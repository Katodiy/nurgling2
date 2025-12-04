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

    private final NArea targetArea;
    private final NContext externalContext;

    public GardenPotFiller() {
        this.targetArea = null;
        this.externalContext = null;
    }

    public GardenPotFiller(NArea area, NContext context) {
        this.targetArea = area;
        this.externalContext = context;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = externalContext != null ? externalContext : new NContext(gui);

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
        NArea potArea;
        if (targetArea != null) {
            potArea = context.getAreaById(targetArea.id);
        } else {
            potArea = context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
        }
        if (potArea == null) {
            return Results.ERROR("No Planting Garden Pots area found. Please configure the specialization.");
        }

        // Get all pots and filter to those needing mulch (marker 0 or 1)
        ArrayList<Gob> allPots = Finder.findGobs(potArea, GardenPotUtils.GARDEN_POT);
        ArrayList<Gob> potsNeedingMulch = GardenPotUtils.filterPotsNeedingMulch(allPots);

        if (potsNeedingMulch.isEmpty()) {
            gui.msg("No garden pots need mulch");
            return Results.SUCCESS();
        }

        gui.msg("Found " + potsNeedingMulch.size() + " garden pots needing mulch");

        // Get mulch from Take area
        int mulchInInventory = gui.getInventory().getItems(GardenPotUtils.SOIL).size();
        if (mulchInInventory == 0) {
            Results getMulchResult = getMulchFromArea(gui, context);
            if (!getMulchResult.IsSuccess()) {
                return getMulchResult;
            }
        }

        // Navigate back to pots area after getting mulch
        if (targetArea != null) {
            context.getAreaById(targetArea.id);
        } else {
            context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
        }

        // Fill each pot with mulch until marker shows full (2 or 3)
        for (Gob pot : potsNeedingMulch) {
            long potId = pot.id;

            // Keep filling same pot until it has mulch
            while (true) {
                // Re-find pot to get fresh data
                Gob currentPot = Finder.findGob(potId);
                if (currentPot == null) {
                    break; // Pot no longer exists
                }

                long marker = currentPot.ngob.getModelAttribute();
                if (marker == GardenPotUtils.MARKER_MULCH_ONLY || marker == GardenPotUtils.MARKER_COMPLETE) {
                    break; // Pot is full, move to next
                }

                fillPotWithMulch(gui, currentPot);

                // Check if we need more mulch
                if (gui.getInventory().getItems(GardenPotUtils.SOIL).isEmpty()) {
                    Results getMulchResult = getMulchFromArea(gui, context);
                    if (!getMulchResult.IsSuccess()) {
                        gui.msg("Out of mulch");
                        return Results.SUCCESS(); // Not an error, we filled what we could
                    }
                    // Navigate back to pots area
                    if (targetArea != null) {
                        context.getAreaById(targetArea.id);
                    } else {
                        context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
                    }
                }
            }
        }

        return Results.SUCCESS();
    }

    private Results fillPotWithMulch(NGameUI gui, Gob pot) throws InterruptedException {
        PathFinder pf = new PathFinder(pot);
        pf.isHardMode = true;
        pf.run(gui);

        // Check if we have mulch
        ArrayList<WItem> mulchItems = gui.getInventory().getItems(GardenPotUtils.SOIL);
        if (mulchItems.isEmpty()) {
            return Results.SUCCESS(); // Need to get more mulch
        }

        // Take mulch to hand
        NUtils.takeItemToHand(mulchItems.get(0));

        // Apply mulch to pot using dropsame
        NUtils.dropsame(pot);

        // Wait until pot has mulch
        NUtils.getUI().core.addTask(new WaitMulchApplied(pot));

        return Results.SUCCESS();
    }

    private Results getMulchFromArea(NGameUI gui, NContext context) throws InterruptedException {
        int freeSpace = gui.getInventory().getFreeSpace();
        if (freeSpace == 0) {
            return Results.ERROR("Inventory is full");
        }

        new TakeItems2(context, "Mulch", freeSpace).run(gui);

        if (gui.getInventory().getItems(GardenPotUtils.SOIL).isEmpty()) {
            return Results.ERROR("No mulch available in Take areas");
        }

        return Results.SUCCESS();
    }

    private Results fillWaterPhase(NGameUI gui, NContext context) throws InterruptedException {
        // Navigate to pots area to get the list of pots
        NArea potArea;
        if (targetArea != null) {
            potArea = context.getAreaById(targetArea.id);
        } else {
            potArea = context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
        }
        if (potArea == null) {
            return Results.ERROR("No Planting Garden Pots area found");
        }

        // Get all pots and filter to those needing water (marker 0 or 2)
        ArrayList<Gob> allPots = Finder.findGobs(potArea, GardenPotUtils.GARDEN_POT);
        ArrayList<Gob> potsNeedingWater = GardenPotUtils.filterPotsNeedingWater(allPots);

        if (potsNeedingWater.isEmpty()) {
            gui.msg("No garden pots need water");
            return Results.SUCCESS();
        }

        gui.msg("Found " + potsNeedingWater.size() + " garden pots needing water");

        // Use FillFluid with the new garden pot constructor (no mask)
        FillFluid fillFluid = new FillFluid(potsNeedingWater, context, Specialisation.SpecName.plantingGardenPots, GardenPotUtils.WATER);
        return fillFluid.run(gui);
    }

    // Task to wait until pot has mulch (marker 2 or 3), with timeout
    private static class WaitMulchApplied extends NTask {
        private final Gob pot;
        private int counter = 0;

        WaitMulchApplied(Gob pot) {
            this.pot = pot;
        }

        @Override
        public boolean check() {
            counter++;
            // Timeout after 100 frames
            if (counter >= 100) {
                return true;
            }
            long marker = pot.ngob.getModelAttribute();
            // Pot has mulch when marker is 2 (mulch only) or 3 (complete)
            return marker == GardenPotUtils.MARKER_MULCH_ONLY || marker == GardenPotUtils.MARKER_COMPLETE;
        }
    }
}
