package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class AreaToTroughBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Setup phase
        NContext context = new NContext(gui);

        // Area selection
        String collectionAreaId = context.createArea(
            "Please select area to collect stockpiles from",
            Resource.loadsimg("baubles/selectItem")
        );

        // Find trough and get its area
        TroughInfo troughInfo = findTrough(context, collectionAreaId);
        if (troughInfo == null || troughInfo.trough == null) {
            return Results.ERROR("No food trough found");
        }

        String troughAreaId = troughInfo.areaId;
        Gob trough = troughInfo.trough;
        Gob cistern = troughInfo.cistern;

        // Statistics
        int totalTransferred = 0;
        int cycles = 0;

        // Main collection loop
        while (true) {
            cycles++;

            // STEP 1: Navigate to collection area (using NContext)
            navigateToArea(context, collectionAreaId, gui);

            // STEP 2: Collect items from stockpiles
            int itemsCollected = collectFromStockpiles(context, collectionAreaId, gui);

            if (itemsCollected == 0) {
                // Area is empty - we're done
                break;
            }

            totalTransferred += itemsCollected;

            // STEP 3: Navigate to trough area (using NContext)
            if (troughAreaId != null) {
                navigateToArea(context, troughAreaId, gui);
            }

            // STEP 4: Transfer all items to trough
            int previousInvCount = gui.getInventory().getItems().size();
            transferAllItemsToTrough(gui, trough, cistern);
            int currentInvCount = gui.getInventory().getItems().size();

            // Check if items didn't transfer (trough full or incompatible)
            if (currentInvCount > 0 && currentInvCount == previousInvCount) {
                gui.msg("Trough appears full or items incompatible. Stopping.");
                break;
            }

            // Check if more items remain in area
            if (!hasMoreStockpiles(context, collectionAreaId)) {
                break;
            }
        }

        // Return to safe location
        new RunToSafe().run(gui);

        // Report results
        gui.msg("Area to Trough: Transferred " + totalTransferred +
                " items in " + cycles + " cycles");

        return Results.SUCCESS();
    }

    private void navigateToArea(NContext context, String areaId, NGameUI gui)
            throws InterruptedException {
        // Let NContext handle navigation via getGobs - it has built-in navigation
        // Just trigger it by requesting gobs from the area
        context.getGobs(areaId, new NAlias("*"));
    }

    private int collectFromStockpiles(NContext context, String areaId, NGameUI gui)
            throws InterruptedException {

        int itemsCollectedThisCycle = 0;

        // Find all stockpiles in area
        ArrayList<Gob> stockpiles = context.getGobs(areaId, new NAlias("stockpile"));

        if (stockpiles.isEmpty()) {
            return 0;
        }

        // Sort by distance (nearest first)
        stockpiles.sort(NUtils.d_comp);

        // Process each stockpile
        for (Gob pile : stockpiles) {
            // Check if inventory is full
            if (gui.getInventory().getFreeSpace() == 0) {
                break;
            }

            // Navigate to this stockpile
            new PathFinder(pile).run(gui);

            // Open stockpile
            new OpenTargetContainer("Stockpile", pile).run(gui);

            // Check how many items are in the stockpile
            int pileCount = gui.getStockpile().calcCount();
            if (pileCount == 0) {
                // Empty stockpile, skip it
                new CloseTargetWindow(gui.getWindow("Stockpile")).run(gui);
                continue;
            }

            // Calculate how many to take
            int invSpace = gui.getInventory().getFreeSpace();
            int toTake = Math.min(pileCount, invSpace);

            // Take items from stockpile
            TakeItemsFromPile takeAction = new TakeItemsFromPile(
                pile,
                gui.getStockpile(),
                toTake
            );
            takeAction.run(gui);

            // Track how many we actually got
            int actuallyTaken = takeAction.getResult();
            itemsCollectedThisCycle += actuallyTaken;

            // Close stockpile
            new CloseTargetWindow(gui.getWindow("Stockpile")).run(gui);

            // If inventory is now full, stop collecting
            if (gui.getInventory().getFreeSpace() == 0) {
                break;
            }
        }

        return itemsCollectedThisCycle;
    }

    private void transferAllItemsToTrough(NGameUI gui, Gob trough, Gob cistern)
            throws InterruptedException {

        // Keep transferring until inventory is empty
        while (!gui.getInventory().getItems().isEmpty()) {

            ArrayList<WItem> items = gui.getInventory().getItems();
            if (items.isEmpty()) break;

            // Get the first item's type
            String itemType = ((NGItem)items.get(0).item).name();
            NAlias itemAlias = new NAlias(itemType);

            // Transfer all items of this type
            TransferToTrough transfer = new TransferToTrough(
                trough,
                itemAlias,
                cistern
            );
            transfer.run(gui);

            // Loop will continue with next item type if inventory not empty
        }
    }

    private TroughInfo findTrough(NContext context, String collectionAreaId)
            throws InterruptedException {

        Gob trough = null;
        String troughAreaId = null;

        // Strategy 1: Search in collection area first
        ArrayList<Gob> troughs = context.getGobs(collectionAreaId, new NAlias("gfx/terobjs/trough"));
        if (!troughs.isEmpty()) {
            trough = troughs.get(0);
            // Trough is in collection area - reuse the same area ID
            troughAreaId = collectionAreaId;
            Gob cistern = findCisternNear(trough);
            return new TroughInfo(troughAreaId, trough, cistern);
        }

        // Strategy 2: Search visible area (current view)
        trough = Finder.findGob(new NAlias("gfx/terobjs/trough"));
        if (trough != null) {
            // Trough is visible but not in collection area
            Gob cistern = findCisternNear(trough);
            return new TroughInfo(null, trough, cistern);
        }

        // Strategy 3: Prompt user for trough area
        troughAreaId = context.createArea(
            "No trough found. Please select area containing food trough",
            Resource.loadsimg("baubles/selectItem")
        );

        // Find trough in user-selected area
        ArrayList<Gob> troughsInArea = context.getGobs(troughAreaId, new NAlias("gfx/terobjs/trough"));
        if (troughsInArea.isEmpty()) {
            return null;
        }

        trough = troughsInArea.get(0);

        Gob cistern = findCisternNear(trough);
        return new TroughInfo(troughAreaId, trough, cistern);
    }

    private Gob findCisternNear(Gob trough) throws InterruptedException {
        if (trough == null) return null;

        Coord2d troughPos = trough.rc;

        // Search within 100 tiles of trough
        ArrayList<Gob> cisterns = Finder.findGobs(
            new haven.Pair<>(
                troughPos.sub(100, 100),
                troughPos.add(100, 100)
            ),
            new NAlias("gfx/terobjs/cistern")
        );

        if (cisterns.isEmpty()) {
            return null;
        }

        // Return closest cistern
        cisterns.sort((a, b) -> Double.compare(
            a.rc.dist(troughPos),
            b.rc.dist(troughPos)
        ));

        return cisterns.get(0);
    }

    private boolean hasMoreStockpiles(NContext context, String areaId) {
        ArrayList<Gob> stockpiles;
        try {
            stockpiles = context.getGobs(areaId, new NAlias("stockpile"));
        } catch (InterruptedException e) {
            return false;
        }

        // Check if any stockpiles are non-empty
        for (Gob pile : stockpiles) {
            // Check if stockpile has items (not model attribute 0)
            if (pile.ngob.getModelAttribute() != 0) {
                return true;
            }
        }

        return false;
    }

    // Helper class to return both trough gob and area info
    private static class TroughInfo {
        String areaId;    // For NContext navigation
        Gob trough;       // The actual trough gob
        Gob cistern;      // Optional cistern nearby

        TroughInfo(String areaId, Gob trough, Gob cistern) {
            this.areaId = areaId;
            this.trough = trough;
            this.cistern = cistern;
        }
    }
}
