package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NGItem;
import nurgling.NISBox;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.actions.TakeItemsFromContainer;
import nurgling.actions.TakeItemsFromPile;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.TransferToTrough;
import nurgling.areas.NContext;
import nurgling.tasks.FilledTrough;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.StockpileUtils;

import java.util.*;

/**
 * Collects swill-compatible items from containers in a selected area
 * and delivers them to a selected trough (via object click selection).
 * Based on CollectSwillInArea but uses SelectGob for trough instead of area system.
 */
public class CollectSwillToTrough implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        // Step 1: Select trough by clicking on it
        SelectGob troughSel;
        gui.msg("Please select trough");
        (troughSel = new SelectGob(Resource.loadsimg("baubles/outputArea"))).run(gui);
        Gob trough = troughSel.result;
        if (trough == null) {
            return Results.ERROR("Trough not selected");
        }

        // Step 2: Select area for swill collection
        SelectArea insa;
        gui.msg("Please select area for swill collection");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);
        Pair<Coord2d, Coord2d> area = insa.getRCArea();

        if (area == null) {
            return Results.ERROR("No area selected");
        }

        // Build swill alias once
        NAlias swillAlias = createSwillAlias();

        // Process containers first (like FreeContainers)
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())))) {
            Container cand = new Container(sm, NContext.contcaps.get(sm.ngob.name), null);
            containers.add(cand);
        }

        if (!containers.isEmpty()) {
            for (Container container : containers) {
                try {
                    new PathFinder(Finder.findGob(container.gobid)).run(gui);
                    new OpenTargetContainer(container.cap, Finder.findGob(container.gobid)).run(gui);

                    // Take swill items directly using alias
                    HashSet<String> swillNames = getSwillItemNamesFromAlias(swillAlias);
                    if (!swillNames.isEmpty()) {
                        new TakeItemsFromContainer(container, swillNames, swillAlias).run(gui);
                    }

                    // If inventory full, deliver and return
                    if (gui.getInventory().getFreeSpace() == 0) {
                        if (!deliverSwillToTrough(gui, trough, swillAlias)) {
                            return Results.SUCCESS(); // Stop collection gracefully - trough full
                        }
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    gui.msg("Error processing container: " + e.getMessage());
                }
            }
        }

        // Process stockpiles (like FreeContainers pattern)
        ArrayList<Gob> gobs;
        while (!(gobs = Finder.findGobs(area, new NAlias("stockpile"))).isEmpty()) {
            for (Gob pile : gobs) {
                if (PathFinder.isAvailable(pile)) {
                    Coord size = StockpileUtils.itemMaxSize.get(pile.ngob.name);
                    new PathFinder(pile).run(gui);
                    new OpenTargetContainer("Stockpile", pile).run(gui);

                    while (Finder.findGob(pile.id) != null) {
                        if (gui.getInventory().getNumberFreeCoord((size != null) ? size : new Coord(1, 1)) > 0) {
                            NISBox spbox = gui.getStockpile();
                            if (spbox != null && spbox.calcCount() > 0) {
                                int target_size = gui.getInventory().getNumberFreeCoord((size != null) ? size : new Coord(1, 1));
                                if (target_size == 0) {
                                    // Inventory full - deliver and return
                                    if (!deliverSwillToTrough(gui, trough, swillAlias)) {
                                        return Results.SUCCESS(); // Stop collection - trough full
                                    }
                                    if (Finder.findGob(pile.id) != null) {
                                        new PathFinder(pile).run(gui);
                                        new OpenTargetContainer("Stockpile", pile).run(gui);
                                    } else break;
                                } else {
                                    // Take items from pile
                                    target_size = Math.min(spbox.calcCount(), target_size);
                                    if (target_size > 0) {
                                        TakeItemsFromPile tifp = new TakeItemsFromPile(pile, spbox, target_size);
                                        tifp.run(gui);
                                    }
                                }
                            } else {
                                break; // Empty pile
                            }
                        } else {
                            // Inventory full - deliver and return
                            if (!deliverSwillToTrough(gui, trough, swillAlias)) {
                                return Results.SUCCESS(); // Stop collection - trough full
                            }
                            if (Finder.findGob(pile.id) != null) {
                                new PathFinder(pile).run(gui);
                                new OpenTargetContainer("Stockpile", pile).run(gui);
                            }
                        }
                    }
                }
            }
            // Break out if no more piles in area
            if (Finder.findGobs(area, new NAlias("stockpile")).isEmpty()) {
                break;
            }
        }

        // Final delivery of any remaining items
        deliverSwillToTrough(gui, trough, swillAlias);
        return Results.SUCCESS();
    }

    /**
     * Create comprehensive swill alias for filtering.
     */
    private NAlias createSwillAlias() {
        HashSet<String> allSwillItems = new HashSet<>();

        // Add all swill items from registry
        allSwillItems.addAll(SwillItemRegistry.HIGH_VALUE_SWILL);
        allSwillItems.addAll(SwillItemRegistry.STANDARD_SWILL);
        allSwillItems.addAll(SwillItemRegistry.LOW_VALUE_SWILL);
        allSwillItems.addAll(SwillItemRegistry.SEED_SWILL);

        return new NAlias(allSwillItems.toArray(new String[0]));
    }

    /**
     * Get swill item names from alias for TakeItemsFromContainer.
     */
    private HashSet<String> getSwillItemNamesFromAlias(NAlias swillAlias) {
        HashSet<String> names = new HashSet<>();
        for (String name : swillAlias.keys) {
            names.add(name);
        }
        return names;
    }

    /**
     * Deliver swill items to the selected trough.
     * @return true if delivery succeeded, false if trough is full
     */
    private boolean deliverSwillToTrough(NGameUI gui, Gob trough, NAlias swillAlias) throws InterruptedException {
        // Check if we have swill items to deliver
        ArrayList<WItem> swillItems = new ArrayList<>();
        for (WItem item : gui.getInventory().getItems()) {
            String itemName = ((NGItem) item.item).name();
            if (itemName != null && isSwillItem(itemName)) {
                swillItems.add(item);
            }
        }

        if (swillItems.isEmpty()) {
            return true; // Nothing to deliver, continue collection
        }

        // Use TransferToTrough for the selected trough
        new TransferToTrough(trough, swillAlias).run(gui);

        // Check if trough is now full
        if (trough.ngob.getModelAttribute() == 7) {
            // Check if we still have items after attempted delivery
            ArrayList<WItem> remainingItems = gui.getInventory().getItems(swillAlias);
            if (!remainingItems.isEmpty()) {
                gui.msg("Trough is full, stopping collection");
                return false; // Stop collection, trough is full
            }
        }

        return true; // Success, continue collection
    }

    /**
     * Check if an item name matches swill criteria.
     */
    private boolean isSwillItem(String itemName) {
        return SwillItemRegistry.isSwillItem(itemName);
    }
}


