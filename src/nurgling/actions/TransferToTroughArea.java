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

        // Continue until all items are transferred or all troughs are full
        while (!gui.getInventory().getItems(items).isEmpty()) {
            // Find all NON-full troughs
            ArrayList<Gob> availableTroughs = new ArrayList<>();
            for (Gob trough : allTroughs) {
                if (!isTroughLikelyFull(trough)) {
                    availableTroughs.add(trough);
                }
            }

            if (availableTroughs.isEmpty()) {
                gui.msg("All troughs are full - stopping transfer");
                break;
            }

            gui.msg("Found " + availableTroughs.size() + " available (non-full) troughs");

            // Pick the first available trough and fill it completely using TransferToTrough pattern
            Gob currentTrough = availableTroughs.get(0);
            gui.msg("Filling trough " + currentTrough.id + " completely");

            Results result = fillTroughCompletely(gui, currentTrough);
            if (!result.IsSuccess()) {
                gui.msg("Failed to fill trough " + currentTrough.id + ": " + result.msg);
                // Remove this problematic trough from our list to avoid infinite loops
                allTroughs.remove(currentTrough);
                if (allTroughs.isEmpty()) {
                    return Results.ERROR("No more troughs available");
                }
            }
        }

        // Check final result
        ArrayList<WItem> remainingItems = gui.getInventory().getItems(items);
        if (remainingItems.isEmpty()) {
            gui.msg("All items successfully transferred to troughs!");
            return Results.SUCCESS();
        } else {
            return Results.ERROR("Could not transfer all items - " + remainingItems.size() + " items remaining. All accessible troughs may be full.");
        }
    }

    /**
     * Fill a single trough completely using the exact TransferToTrough pattern.
     * This method is fast because it stays focused on one trough until it's full.
     */
    private Results fillTroughCompletely(NGameUI gui, Gob trough) throws InterruptedException {
        // Navigate to the trough first
        new PathFinder(trough).run(gui);

        ArrayList<WItem> witems;

        // Use the exact same loop as TransferToTrough - fast and efficient
        while (!(witems = gui.getInventory().getItems(items)).isEmpty()) {
            // Check if trough is full (same check as TransferToTrough)
            if (trough.ngob.getModelAttribute() == 7) {
                gui.msg("Trough " + trough.id + " is now full (model attribute = 7)");
                break;
            }

            // Take item to hand and drop into trough (exact TransferToTrough pattern)
            NUtils.takeItemToHand(witems.get(0));
            NUtils.dropsame(trough);

            // Wait for trough to process the item (exact TransferToTrough pattern)
            NUtils.getUI().core.addTask(new FilledTrough(trough, items));
        }

        return Results.SUCCESS();
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