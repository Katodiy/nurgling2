package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NGItem;
import nurgling.NISBox;
import nurgling.NConfig;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.actions.TakeItemsFromContainer;
import nurgling.actions.TakeItemsFromPile;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.TransferToTrough;
import nurgling.NMapView;
import nurgling.routes.RoutePoint;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.StockpileUtils;
import nurgling.widgets.Specialisation;

import java.util.*;

/**
 * Collects swill-compatible items from containers in a selected area
 * and delivers them to appropriate feeding destinations.
 * Based on the FreeContainersInArea pattern with specialized swill handling.
 */
public class CollectSwillInArea implements Action {

    private RoutePoint closestRoutePoint = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Store closest route point like FreeContainers
        this.closestRoutePoint = ((NMapView) gui.map).routeGraphManager.getGraph().findNearestPointToPlayer(gui);

        // Simple area selection like FreeContainers
        SelectArea insa;
        gui.msg("Please, select area for swill collection");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);
        Pair<Coord2d, Coord2d> area = insa.getRCArea();

        if (area == null) {
            return Results.ERROR("No area selected");
        }

        // Build swill alias once
        NAlias swillAlias = createSwillAlias();
        NContext context = new NContext(gui);

        // Process containers first (like FreeContainers)
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())))) {
            Container cand = new Container(sm, NContext.contcaps.get(sm.ngob.name));
            containers.add(cand);
        }

        if (!containers.isEmpty()) {
            gui.msg("Processing " + containers.size() + " containers");
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
                        deliverSwillToTrough(gui, context, swillAlias);
                        returnToAreaIfNeeded(gui, area);
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
                                    deliverSwillToTrough(gui, context, swillAlias);
                                    returnToAreaIfNeeded(gui, area);
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

                                        // Count swill items collected
                                        int swillCount = 0;
                                        for (NGItem item : tifp.newItems()) {
                                            if (item.name() != null && isSwillItem(item.name())) {
                                                swillCount++;
                                            }
                                        }
                                        if (swillCount > 0) {
                                            gui.msg("Collected " + swillCount + " swill items from stockpile");
                                        }
                                    }
                                }
                            }
                        } else {
                            // Inventory full - deliver and return
                            deliverSwillToTrough(gui, context, swillAlias);
                            returnToAreaIfNeeded(gui, area);
                            if (Finder.findGob(pile.id) != null) {
                                new PathFinder(pile).run(gui);
                                new OpenTargetContainer("Stockpile", pile).run(gui);
                            }
                        }
                    }
                }
            }
        }

        // Final delivery of any remaining items
        deliverSwillToTrough(gui, context, swillAlias);

        gui.msg("Swill collection completed");
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
     * Deliver swill items to trough using proper navigation and TransferToTrough.
     */
    private void deliverSwillToTrough(NGameUI gui, NContext context, NAlias swillAlias) throws InterruptedException {
        // Check if we have swill items to deliver
        ArrayList<WItem> swillItems = new ArrayList<>();
        for (WItem item : gui.getInventory().getItems()) {
            String itemName = ((NGItem)item.item).name();
            if (itemName != null && isSwillItem(itemName)) {
                swillItems.add(item);
            }
        }

        if (swillItems.isEmpty()) {
            return; // Nothing to deliver
        }

        gui.msg("Delivering " + swillItems.size() + " swill items to trough");

        try {
            // Navigate to trough area
            NArea troughArea = context.getSpecArea(Specialisation.SpecName.trough);
            if (troughArea != null) {
                gui.msg("Navigated to trough area");

                // Find a trough in the area
                Pair<Coord2d, Coord2d> areaCoords = troughArea.getRCArea();
                if (areaCoords != null) {
                    ArrayList<Gob> troughs = Finder.findGobs(areaCoords, new NAlias("Trough", "Cistern", "Stone Cistern"));
                    if (!troughs.isEmpty()) {
                        Gob trough = troughs.get(0);
                        new PathFinder(trough).run(gui);

                        // Use TransferToTrough with the found trough
                        new TransferToTrough(trough, swillAlias).run(gui);
                        gui.msg("Swill delivery completed");
                    } else {
                        gui.msg("No trough found in trough area");
                    }
                } else {
                    gui.msg("Could not get trough area coordinates");
                }
            } else {
                gui.msg("Could not find trough area");
            }
        } catch (Exception e) {
            gui.msg("Error during delivery: " + e.getMessage());
        }
    }

    /**
     * Return to collection area if needed using RoutePointNavigator.
     */
    private void returnToAreaIfNeeded(NGameUI gui, Pair<Coord2d, Coord2d> area) throws InterruptedException {
        if (closestRoutePoint != null && (Boolean) NConfig.get(NConfig.Key.useGlobalPf)) {
            // Calculate if we're far from the area
            Coord2d areaCenter = new Coord2d((area.a.x + area.b.x) / 2, (area.a.y + area.b.y) / 2);
            double distance = areaCenter.dist(NUtils.player().rc);

            if (distance > 500) {
                gui.msg("Returning to collection area via route navigation");
                new RoutePointNavigator(closestRoutePoint).run(gui);
            }
        }
    }

    /**
     * Check if an item name matches swill criteria.
     */
    private boolean isSwillItem(String itemName) {
        return SwillItemRegistry.isSwillItem(itemName);
    }

}