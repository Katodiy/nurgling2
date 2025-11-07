package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import haven.Pair;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.FilledTrough;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Transfers items to multiple troughs within a trough specialization area.
 * Automatically finds and fills available troughs, moving to the next when one becomes full.
 * Based on the TransferToTrough pattern but handles multiple trough targets intelligently.
 */
public class TransferToTroughArea implements Action {

    private final NAlias items;
    private final NArea troughArea;
    private final NContext context;

    /**
     * Create a transfer action to fill troughs in the trough specialization area.
     *
     * @param gui The game UI context
     * @param items The items to transfer (NAlias for filtering)
     */
    public TransferToTroughArea(NGameUI gui, NAlias items) throws InterruptedException {
        this.items = items;
        this.context = new NContext(gui);
        this.troughArea = context.getSpecArea(Specialisation.SpecName.trough);

        if (this.troughArea == null) {
            throw new IllegalArgumentException("No trough specialization area found in NContext");
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (troughArea == null) {
            return Results.ERROR("No trough area found");
        }

        // Get area coordinates
        Pair<Coord2d, Coord2d> areaCoords = troughArea.getRCArea();
        if (areaCoords == null) {
            return Results.ERROR("Could not get trough area coordinates");
        }

        // Find all troughs in the area
        ArrayList<Gob> allTroughs = Finder.findGobs(areaCoords, new NAlias("Trough", "Cistern", "Stone Cistern"));
        if (allTroughs.isEmpty()) {
            return Results.ERROR("No troughs found in trough area");
        }

        gui.msg("Found " + allTroughs.size() + " troughs in area");

        ArrayList<WItem> remainingItems;
        int troughsAttempted = 0;
        int troughsFilled = 0;

        // Continue until all items are transferred or all troughs are full
        while (!(remainingItems = gui.getInventory().getItems(items)).isEmpty()) {
            boolean progressMade = false;

            gui.msg("Attempting to distribute " + remainingItems.size() + " items across troughs");

            // Try each trough until we find one that accepts items
            for (Gob trough : allTroughs) {
                if (remainingItems.isEmpty()) {
                    break; // No more items to transfer
                }

                try {
                    gui.msg("Trying trough at " + trough.rc + " (ID: " + trough.id + ")");

                    // Navigate to this trough
                    Results pathResult = new PathFinder(trough).run(gui);
                    if (!pathResult.IsSuccess()) {
                        gui.msg("Could not reach trough " + trough.id);
                        continue;
                    }

                    troughsAttempted++;

                    // Try to feed this trough
                    int itemsBeforeFeeding = gui.getInventory().getItems(items).size();
                    Results feedResult = feedSingleTrough(gui, trough);
                    int itemsAfterFeeding = gui.getInventory().getItems(items).size();

                    if (feedResult.IsSuccess() && itemsAfterFeeding < itemsBeforeFeeding) {
                        progressMade = true;
                        troughsFilled++;
                        gui.msg("Successfully fed items to trough " + trough.id);

                        // Update remaining items list for next iteration
                        remainingItems = gui.getInventory().getItems(items);

                        if (remainingItems.isEmpty()) {
                            gui.msg("All items successfully distributed to troughs");
                            return Results.SUCCESS();
                        }
                    } else {
                        gui.msg("Trough " + trough.id + " appears to be full or could not accept items");
                    }

                } catch (Exception e) {
                    gui.msg("Error feeding trough " + trough.id + ": " + e.getMessage());
                    continue;
                }
            }

            // Check if we made any progress this round
            if (!progressMade) {
                gui.msg("No troughs could accept more items - all troughs may be full");
                break;
            }
        }

        // Final status report
        remainingItems = gui.getInventory().getItems(items);
        if (remainingItems.isEmpty()) {
            gui.msg("Transfer completed successfully! Fed " + troughsFilled + " troughs");
            return Results.SUCCESS();
        } else {
            return Results.ERROR("Could not transfer all items - " + remainingItems.size() + " items remaining. " +
                    "Attempted " + troughsAttempted + " troughs, successfully fed " + troughsFilled + " troughs. " +
                    "All accessible troughs may be full.");
        }
    }

    /**
     * Feed a single trough using the same pattern as TransferToTrough.
     * Returns SUCCESS if items were successfully fed, FAIL if trough is full or unreachable.
     */
    private Results feedSingleTrough(NGameUI gui, Gob trough) throws InterruptedException {
        ArrayList<WItem> itemsToFeed = gui.getInventory().getItems(items);
        if (itemsToFeed.isEmpty()) {
            return Results.SUCCESS(); // No items to feed
        }

        int itemsFed = 0;
        int maxFeedAttempts = 20; // Prevent infinite loops on problematic troughs
        int feedAttempts = 0;

        try {
            // Feed items one by one using TransferToTrough pattern
            while (feedAttempts < maxFeedAttempts) {
                // Refresh item list
                itemsToFeed = gui.getInventory().getItems(items);
                if (itemsToFeed.isEmpty()) {
                    break; // No more items
                }

                feedAttempts++;
                WItem item = itemsToFeed.get(0);

                // Handle cistern mechanism (from TransferToTrough)
                if (trough.ngob.getModelAttribute() == 7) {
                    // Trough is full - stop feeding this trough
                    gui.msg("Trough " + trough.id + " is full (model attribute = 7)");
                    break;
                }

                // Take item to hand and drop into trough (exact TransferToTrough pattern)
                NUtils.takeItemToHand(item);
                NUtils.dropsame(trough);

                // Wait for trough to process the item
                NUtils.getUI().core.addTask(new FilledTrough(trough, items));

                itemsFed++;

                // Small delay to prevent overwhelming the trough
                Thread.sleep(100);
            }

            if (itemsFed > 0) {
                gui.msg("Fed " + itemsFed + " items to trough " + trough.id);
                return Results.SUCCESS();
            } else {
                gui.msg("Could not feed any items to trough " + trough.id + " (may be full)");
                return Results.FAIL();
            }

        } catch (Exception e) {
            gui.msg("Error feeding trough " + trough.id + ": " + e.getMessage());
            return Results.FAIL();
        }
    }

    /**
     * Check if a trough is likely full by examining its state.
     * This is a heuristic check - the FilledTrough task provides the definitive check.
     */
    private boolean isTroughLikelyFull(Gob trough) {
        try {
            // Trough model attribute 7 typically indicates full state
            return trough.ngob.getModelAttribute() == 7;
        } catch (Exception e) {
            return false; // Assume not full if we can't check
        }
    }
}