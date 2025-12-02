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

    // Marker states
    private static final long MARKER_EMPTY = 0;      // Needs both mulch and water
    private static final long MARKER_WATER_ONLY = 1; // Needs mulch
    private static final long MARKER_MULCH_ONLY = 2; // Needs water
    private static final long MARKER_COMPLETE = 3;   // Done

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

    // Get pots that need mulch (marker 0 or 1)
    private ArrayList<Gob> getPotsNeedingMulch(ArrayList<Gob> allPots) {
        ArrayList<Gob> result = new ArrayList<>();
        for (Gob pot : allPots) {
            long marker = pot.ngob.getModelAttribute();
            if (marker == MARKER_EMPTY || marker == MARKER_WATER_ONLY) {
                result.add(pot);
            }
        }
        return result;
    }

    // Get pots that need water (marker 0 or 2)
    private ArrayList<Gob> getPotsNeedingWater(ArrayList<Gob> allPots) {
        ArrayList<Gob> result = new ArrayList<>();
        for (Gob pot : allPots) {
            long marker = pot.ngob.getModelAttribute();
            if (marker == MARKER_EMPTY || marker == MARKER_MULCH_ONLY) {
                result.add(pot);
            }
        }
        return result;
    }

    private Results fillMulchPhase(NGameUI gui, NContext context) throws InterruptedException {
        // Navigate to pots area
        NArea potArea = context.getSpecArea(Specialisation.SpecName.plantingGardenPots);
        if (potArea == null) {
            return Results.ERROR("No Planting Garden Pots area found. Please configure the specialization.");
        }

        // Get all pots and filter to those needing mulch (marker 0 or 1)
        ArrayList<Gob> allPots = Finder.findGobs(potArea, GARDEN_POT);
        ArrayList<Gob> potsNeedingMulch = getPotsNeedingMulch(allPots);

        if (potsNeedingMulch.isEmpty()) {
            gui.msg("No garden pots need mulch");
            return Results.SUCCESS();
        }

        gui.msg("Found " + potsNeedingMulch.size() + " garden pots needing mulch");

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

        // Fill each pot with mulch until marker changes
        for (Gob pot : potsNeedingMulch) {
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

            long markerBefore = pot.ngob.getModelAttribute();

            // Take mulch to hand
            NUtils.takeItemToHand(mulchItems.get(0));

            // Apply mulch to pot using dropsame
            NUtils.dropsame(pot);

            // Wait for marker change, hand free, or timeout
            WaitMulchApplied waitTask = new WaitMulchApplied(pot, markerBefore);
            NUtils.getUI().core.addTask(waitTask);

            return Results.SUCCESS();
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

        // Get all pots and filter to those needing water (marker 0 or 2)
        ArrayList<Gob> allPots = Finder.findGobs(potArea, GARDEN_POT);
        ArrayList<Gob> potsNeedingWater = getPotsNeedingWater(allPots);

        if (potsNeedingWater.isEmpty()) {
            gui.msg("No garden pots need water");
            return Results.SUCCESS();
        }

        gui.msg("Found " + potsNeedingWater.size() + " garden pots needing water");

        // Use FillFluid with the new garden pot constructor (no mask)
        FillFluid fillFluid = new FillFluid(potsNeedingWater, context, Specialisation.SpecName.plantingGardenPots, WATER);
        return fillFluid.run(gui);
    }

    // Task to wait for marker change, hand free, or timeout
    private static class WaitMulchApplied extends NTask {
        private final Gob pot;
        private final long originalMarker;
        private int counter = 0;
        boolean markerChanged = false;

        WaitMulchApplied(Gob pot, long originalMarker) {
            this.pot = pot;
            this.originalMarker = originalMarker;
        }

        @Override
        public boolean check() {
            counter++;

            // Check if marker changed
            long currentMarker = pot.ngob.getModelAttribute();
            if (currentMarker != originalMarker) {
                markerChanged = true;
                return true;
            }

            return false;
        }
    }
}
