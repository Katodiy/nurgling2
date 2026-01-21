package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseAreaManager;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

/**
 * Test bot that demonstrates the cheese navigation bug WITHOUT the fix.
 *
 * Flow:
 * 1. Navigate to first cheese area (buffer containers)
 * 2. Find containers, store references in list
 * 3. Navigate to a DIFFERENT cheese area (simulates FreeInventory2)
 * 4. Navigate BACK to original area
 * 5. Try to PathFinder to containers using OLD stale references
 *
 * Expected result: PathFinder fails with "can't find path" on step 5
 */
public class CheeseNavTestWithoutFix implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        gui.msg("=== CheeseNavTest WITHOUT FIX ===");
        gui.msg("This test demonstrates the stale gob reference bug");

        // Get two different cheese areas to navigate between
        NArea sourceArea = findFirstCheeseAreaWithContainers(gui, CheeseBranch.Place.cellar);
        if (sourceArea == null) {
            sourceArea = findFirstCheeseAreaWithContainers(gui, CheeseBranch.Place.inside);
        }

        NArea destinationArea = findFirstCheeseAreaWithContainers(gui, CheeseBranch.Place.outside);
        if (destinationArea == null) {
            destinationArea = findFirstCheeseAreaWithContainers(gui, CheeseBranch.Place.inside);
        }

        if (sourceArea == null) {
            gui.error("No source cheese area with containers found");
            return Results.FAIL();
        }

        if (destinationArea == null || destinationArea.id == sourceArea.id) {
            gui.error("No different destination area found. Need two separate areas to test.");
            return Results.FAIL();
        }

        gui.msg("Source area: " + sourceArea.name + " (id=" + sourceArea.id + ")");
        gui.msg("Destination area: " + destinationArea.name + " (id=" + destinationArea.id + ")");

        // Step 1: Navigate to source area
        gui.msg("Step 1: Navigating to source area...");
        NContext context = new NContext(gui);
        context.getAreaById(sourceArea.id);

        // Step 2: Find containers and store references
        gui.msg("Step 2: Finding containers in source area...");
        ArrayList<Gob> originalContainers = Finder.findGobs(sourceArea,
            new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

        gui.msg("Found " + originalContainers.size() + " containers");
        for (Gob container : originalContainers) {
            gui.msg("  - " + container.ngob.name + " (id=" + container.id + ", pos=" + container.rc + ")");
        }

        if (originalContainers.isEmpty()) {
            gui.error("No containers found in source area");
            return Results.FAIL();
        }

        // Step 3: Navigate to destination area (simulates FreeInventory2)
        gui.msg("Step 3: Navigating to destination area (simulating FreeInventory2)...");
        context = new NContext(gui);
        context.getAreaById(destinationArea.id);

        // Small wait to simulate doing work
        Thread.sleep(1000);
        gui.msg("Arrived at destination area");

        // Step 4: Navigate BACK to source area
        gui.msg("Step 4: Navigating BACK to source area...");
        context = new NContext(gui);
        context.getAreaById(sourceArea.id);

        // Step 5: Try to access containers using OLD references (WITHOUT FIX)
        gui.msg("Step 5: Testing PathFinder with OLD container references (WITHOUT FIX)...");
        gui.msg("Using " + originalContainers.size() + " container references from before navigation");

        int successCount = 0;
        int failCount = 0;

        for (Gob container : originalContainers) {
            gui.msg("Trying PathFinder to: " + container.ngob.name + " (id=" + container.id + ")");
            try {
                Results pathResult = new PathFinder(container).run(gui);
                if (pathResult.IsSuccess()) {
                    gui.msg("  SUCCESS - PathFinder reached container");
                    successCount++;
                } else {
                    gui.msg("  FAIL - PathFinder returned failure");
                    failCount++;
                }
            } catch (Exception e) {
                gui.msg("  FAIL - Exception: " + e.getMessage());
                failCount++;
            }
        }

        // Report results
        gui.msg("=== TEST RESULTS (WITHOUT FIX) ===");
        gui.msg("Total containers: " + originalContainers.size());
        gui.msg("PathFinder SUCCESS: " + successCount);
        gui.msg("PathFinder FAIL: " + failCount);

        if (failCount > 0) {
            gui.msg("BUG CONFIRMED: Stale gob references caused PathFinder failures");
        } else {
            gui.msg("No failures detected (area may be close enough that gobs stayed loaded)");
        }

        return Results.SUCCESS();
    }

    private NArea findFirstCheeseAreaWithContainers(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        ArrayList<NArea> areas = CheeseAreaManager.getAllCheeseAreas(place);
        for (NArea area : areas) {
            // Navigate to check if it has containers
            NContext context = new NContext(gui);
            context.getAreaById(area.id);

            ArrayList<Gob> containers = Finder.findGobs(area,
                new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

            if (!containers.isEmpty()) {
                return area;
            }
        }
        return null;
    }
}
