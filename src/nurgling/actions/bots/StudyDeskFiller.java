package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.CloseTargetContainer;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.actions.TakeWItemsFromContainer;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import org.json.JSONObject;

import java.awt.Color;
import java.util.*;

/**
 * Bot that validates the current study desk layout against the planned layout from config
 */
public class StudyDeskFiller implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Step 1: Validate config exists
        Map<String, Object> charData = loadAndValidateConfig(gui);
        if (charData == null) {
            return Results.ERROR("No planner layout");
        }

        String studyDeskHash = (String) charData.get("gobHash");
        Map<String, Object> plannedLayout = (Map<String, Object>) charData.get("layout");

        gui.msg("Found layout config with " + plannedLayout.size() + " planned items", Color.GREEN);

        // Step 2: Navigate to study desk area
        NArea studyDeskArea = getStudyDeskArea(gui);
        if (studyDeskArea == null) {
            gui.msg("ERROR: No study desk area found! Please create one first.", Color.RED);
            return Results.ERROR("No study desk area found! Please create one first");
        }

        // Step 3: Find the specific study desk by hash
        gui.msg("Looking for study desk with hash: " + studyDeskHash, Color.YELLOW);
        Gob studyDesk = Finder.findGob(studyDeskHash);
        if (studyDesk == null) {
            gui.msg("ERROR: Could not find study desk with hash: " + studyDeskHash, Color.RED);
            return Results.ERROR("Could not find study desk with hash: " + studyDeskHash);
        }

        gui.msg("Found study desk! Navigating to it...", Color.GREEN);

        // Step 4: Navigate to the study desk
        new PathFinder(studyDesk).run(gui);

        // Step 5: Open the study desk
        gui.msg("Opening study desk...", Color.YELLOW);
        new OpenTargetContainer("Study Desk", studyDesk).run(gui);

        // Step 6: Get the study desk inventory
        NInventory studyDeskInv = gui.getInventory("Study Desk");
        if (studyDeskInv == null) {
            gui.msg("ERROR: Could not access study desk inventory!", Color.RED);
            return Results.ERROR("ERROR: Could not access study desk inventory!");
        }

        gui.msg("Study desk opened successfully! Analyzing contents...", Color.GREEN);

        // Step 7: Build map of current item positions
        Map<Coord, WItem> currentItems = buildCurrentItemsMap(studyDeskInv);
        gui.msg("Current desk has " + currentItems.size() + " items", Color.CYAN);

        // Step 8: Find missing items
        List<MissingItem> missingItems = findMissingItems(plannedLayout, currentItems);

        // Step 9: Report results
        reportMissingItems(gui, missingItems);

        // Step 10: Fetch and place missing items
        if (!missingItems.isEmpty()) {
            gui.msg("Starting to fetch and place missing items...", Color.CYAN);
            fetchAndPlaceAllItems(gui, missingItems, studyDesk, studyDeskInv);
        }

        return Results.SUCCESS();
    }

    /**
     * Load and validate the study desk layout config for the current character
     */
    private Map<String, Object> loadAndValidateConfig(NGameUI gui) {
        String charName = gui.chrid;
        Object existingData = NConfig.get(NConfig.Key.studyDeskLayout);

        if (existingData == null) {
            gui.msg("ERROR: No study desk layout configuration found!", Color.RED);
            return null;
        }

        Map<String, Object> allLayouts;
        if (existingData instanceof Map) {
            allLayouts = (Map<String, Object>) existingData;
        } else if (existingData instanceof String && !((String) existingData).isEmpty()) {
            JSONObject jsonObj = new JSONObject((String) existingData);
            allLayouts = jsonObj.toMap();
        } else {
            gui.msg("ERROR: Invalid study desk layout configuration!", Color.RED);
            return null;
        }

        if (!allLayouts.containsKey(charName)) {
            gui.msg("ERROR: No study desk layout found for character: " + charName, Color.RED);
            return null;
        }

        Object charObj = allLayouts.get(charName);
        if (!(charObj instanceof Map)) {
            gui.msg("ERROR: Invalid character layout data!", Color.RED);
            return null;
        }

        Map<String, Object> charData = (Map<String, Object>) charObj;

        if (!charData.containsKey("gobHash")) {
            gui.msg("ERROR: No study desk hash found in layout config!", Color.RED);
            return null;
        }

        if (!charData.containsKey("layout")) {
            gui.msg("ERROR: No layout data found in config!", Color.RED);
            return null;
        }

        Object layoutObj = charData.get("layout");
        if (!(layoutObj instanceof Map)) {
            gui.msg("ERROR: Invalid layout data format!", Color.RED);
            return null;
        }

        return charData;
    }

    /**
     * Get the study desk area using NContext
     */
    private NArea getStudyDeskArea(NGameUI gui) throws InterruptedException {
        gui.msg("Navigating to study desks area...", Color.YELLOW);
        NContext context = new NContext(gui);
        return context.getSpecArea(Specialisation.SpecName.studyDesks);
    }

    /**
     * Build a map of current item positions in the study desk
     */
    private Map<Coord, WItem> buildCurrentItemsMap(NInventory inventory) throws InterruptedException {
        Map<Coord, WItem> currentItems = new HashMap<>();
        ArrayList<WItem> items = inventory.getItems();

        for (WItem witem : items) {
            if (witem != null && witem.c != null) {
                // Convert pixel coordinates to grid coordinates
                Coord gridPos = witem.c.div(inventory.sqsz);
                currentItems.put(gridPos, witem);
            }
        }

        return currentItems;
    }

    /**
     * Find missing items by comparing planned layout vs current items
     */
    private List<MissingItem> findMissingItems(Map<String, Object> plannedLayout, Map<Coord, WItem> currentItems) {
        List<MissingItem> missingItems = new ArrayList<>();

        for (Map.Entry<String, Object> plannedEntry : plannedLayout.entrySet()) {
            String posKey = plannedEntry.getKey();
            String[] coords = posKey.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            Coord plannedPos = new Coord(x, y);

            if (!(plannedEntry.getValue() instanceof Map)) {
                continue;
            }

            Map<String, Object> itemData = (Map<String, Object>) plannedEntry.getValue();
            String itemName = (String) itemData.get("name");

            // Extract item size from the layout data
            Coord itemSize = new Coord(1, 1); // Default size
            if (itemData.containsKey("size")) {
                Map<String, Object> sizeData = (Map<String, Object>) itemData.get("size");
                itemSize = new Coord(
                    ((Number) sizeData.get("x")).intValue(),
                    ((Number) sizeData.get("y")).intValue()
                );
            }

            // Check if this position is empty in the current desk
            if (!currentItems.containsKey(plannedPos)) {
                missingItems.add(new MissingItem(itemName, plannedPos, itemSize));
            }
        }

        return missingItems;
    }

    /**
     * Fetch and place all missing items into the study desk
     */
    private void fetchAndPlaceAllItems(NGameUI gui, List<MissingItem> missingItems, Gob studyDesk, NInventory studyDeskInv) throws InterruptedException {
        // Group missing items by item name
        Map<String, List<MissingItem>> itemGroups = new HashMap<>();
        for (MissingItem missing : missingItems) {
            itemGroups.computeIfAbsent(missing.itemName, k -> new ArrayList<>()).add(missing);
        }

        gui.msg("Processing " + itemGroups.size() + " unique item types...", Color.CYAN);

        // Process each item type
        for (Map.Entry<String, List<MissingItem>> entry : itemGroups.entrySet()) {
            String itemName = entry.getKey();
            List<MissingItem> itemsNeeded = entry.getValue();

            gui.msg("Fetching " + itemsNeeded.size() + " x " + itemName, Color.YELLOW);

            try {
                fetchAndPlaceItemType(gui, itemName, itemsNeeded, studyDesk, studyDeskInv);
            } catch (Exception e) {
                // Skip this item if fetching fails, but continue with others
                gui.msg("Failed to fetch " + itemName + ", skipping: " + e.getMessage(), Color.ORANGE);
            }
        }

        gui.msg("Item placement complete!", Color.GREEN);
    }

    /**
     * Fetch and place all items of a specific type
     */
    private void fetchAndPlaceItemType(NGameUI gui, String itemName, List<MissingItem> itemsNeeded, Gob studyDesk, NInventory studyDeskInv) throws InterruptedException {
        // Get item size from the first item's planned data
        Coord itemSize = itemsNeeded.get(0).itemSize;

        // Create NContext to find storage
        NContext context = new NContext(gui);
        context.addInItem(itemName, null);

        ArrayList<NContext.ObjectStorage> storages = context.getInStorages(itemName);
        if (storages == null || storages.isEmpty()) {
            gui.msg("No storage found for " + itemName + ", skipping", Color.ORANGE);
            return;
        }

        // Process items in batches based on inventory capacity
        List<MissingItem> remainingItems = new ArrayList<>(itemsNeeded);

        while (!remainingItems.isEmpty()) {
            // Check how many items can fit in player inventory
            int canFit = gui.getInventory().getNumberFreeCoord(itemSize);

            if (canFit == 0) {
                gui.msg("Inventory full, cannot continue with " + itemName, Color.RED);
                break;
            }

            // Take min(canFit, remaining) items
            int toFetch = Math.min(canFit, remainingItems.size());
            gui.msg("Fetching " + toFetch + " x " + itemName + " (capacity: " + canFit + ")", Color.CYAN);

            // Fetch items from storage
            int fetched = fetchItemsFromStorage(gui, storages, itemName, toFetch);

            if (fetched == 0) {
                gui.msg("Could not fetch any " + itemName + " from storage, skipping", Color.ORANGE);
                break;
            }

            gui.msg("Fetched " + fetched + " items, now placing them...", Color.GREEN);

            // Navigate back to study desk and place items
            getStudyDeskArea(gui);
            new PathFinder(studyDesk).run(gui);
            new OpenTargetContainer("Study Desk", studyDesk).run(gui);

            // Refresh study desk inventory reference
            studyDeskInv = gui.getInventory("Study Desk");
            if (studyDeskInv == null) {
                gui.msg("ERROR: Lost study desk inventory reference!", Color.RED);
                return;
            }

            // Place the fetched items
            int placed = placeItemsInDesk(gui, itemName, remainingItems.subList(0, fetched), studyDeskInv);

            // Remove placed items from remaining list
            for (int i = 0; i < placed; i++) {
                remainingItems.remove(0);
            }

            gui.msg("Placed " + placed + " items, " + remainingItems.size() + " remaining", Color.GREEN);
        }
    }

    /**
     * Fetch items from storage containers
     */
    private int fetchItemsFromStorage(NGameUI gui, ArrayList<NContext.ObjectStorage> storages, String itemName, int count) throws InterruptedException {
        int totalFetched = 0;
        NAlias itemAlias = new NAlias(itemName);

        for (NContext.ObjectStorage storage : storages) {
            if (totalFetched >= count) {
                break;
            }

            try {
                // Handle different storage types
                if (storage instanceof Container) {
                    Container container = (Container) storage;
                    int fetched = fetchFromContainer(gui, container, itemAlias, count - totalFetched);
                    totalFetched += fetched;
                }
                else if (storage instanceof NContext.Pile) {
                    NContext.Pile pile = (NContext.Pile) storage;
                    int fetched = fetchFromPile(gui, pile, itemAlias, count - totalFetched);
                    totalFetched += fetched;
                }
                // Skip Barter for now as it's more complex
            } catch (Exception e) {
                // Skip this storage and try next one
                gui.msg("Failed to fetch from storage, trying next...", Color.YELLOW);
            }
        }

        return totalFetched;
    }

    /**
     * Fetch items from a Container storage
     */
    private int fetchFromContainer(NGameUI gui, Container container, NAlias itemAlias, int count) throws InterruptedException {
        // Navigate to container
        Gob containerGob = Finder.findGob(container.gobid);
        if (containerGob == null) {
            return 0;
        }

        new PathFinder(containerGob).run(gui);

        // Open container
        new OpenTargetContainer(container.cap, containerGob).run(gui);
        NInventory containerInv = gui.getInventory(container.cap);

        int fetched = 0;
        if (containerInv != null) {
            // Get available items
            ArrayList<WItem> availableItems = containerInv.getItems(itemAlias);
            int toTake = Math.min(availableItems.size(), count);

            // Take items to player inventory
            if (toTake > 0) {
                ArrayList<WItem> itemsToTake = new ArrayList<>(availableItems.subList(0, toTake));
                for (WItem item : itemsToTake) {
                    if (gui.getInventory().getNumberFreeCoord(item) > 0) {
                        item.item.wdgmsg("transfer", Coord.z);
                        NUtils.addTask(new ISRemoved(item.item.wdgid()));
                        fetched++;
                    } else {
                        break; // Inventory full
                    }
                }
            }
        }

        new CloseTargetContainer(container.cap).run(gui);
        return fetched;
    }

    /**
     * Fetch items from a Pile storage
     */
    private int fetchFromPile(NGameUI gui, NContext.Pile pile, NAlias itemAlias, int count) throws InterruptedException {
        if (pile.pile == null) {
            return 0;
        }

        // Navigate to pile
        new PathFinder(pile.pile).run(gui);

        int startCount = gui.getInventory().getItems(itemAlias).size();
        int toTake = Math.min(count, gui.getInventory().calcFreeSpace());

        // Take items from pile by right-clicking
        for (int i = 0; i < toTake; i++) {
            NUtils.rclickGob(pile.pile);
            // Wait for item to appear in inventory
            int expectedCount = startCount + i + 1;
            NUtils.addTask(new WaitItems(gui.getInventory(), itemAlias, expectedCount));
        }

        int endCount = gui.getInventory().getItems(itemAlias).size();
        return endCount - startCount;
    }

    /**
     * Place items from player inventory into study desk at exact positions
     */
    private int placeItemsInDesk(NGameUI gui, String itemName, List<MissingItem> itemsToPlace, NInventory studyDeskInv) throws InterruptedException {
        int placed = 0;
        NAlias itemAlias = new NAlias(itemName);

        // Get items from player inventory
        ArrayList<WItem> itemsInInventory = gui.getInventory().getItems(itemAlias);

        for (int i = 0; i < Math.min(itemsToPlace.size(), itemsInInventory.size()); i++) {
            MissingItem target = itemsToPlace.get(i);
            WItem item = itemsInInventory.get(i);

            try {
                // Take item to hand
                NUtils.takeItemToHand(item);

                // Wait a bit for item to be in hand
                NUtils.getUI().core.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return gui.vhand != null;
                    }
                });

                // Drop at precise position
                studyDeskInv.dropOn(target.position, itemName);

                // Wait for slot to be filled
                Coord finalPos = target.position;
                NUtils.getUI().core.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return !studyDeskInv.isSlotFree(finalPos);
                    }
                });

                placed++;
                gui.msg("Placed " + itemName + " at (" + target.position.x + ", " + target.position.y + ")", Color.GREEN);

            } catch (Exception e) {
                gui.msg("Failed to place item at position, skipping", Color.ORANGE);
            }
        }

        return placed;
    }

    /**
     * Report missing items to the user
     */
    private void reportMissingItems(NGameUI gui, List<MissingItem> missingItems) {
        if (missingItems.isEmpty()) {
            gui.msg("SUCCESS: All planned items are present in the study desk!", Color.GREEN);
            return;
        }

        gui.msg("=".repeat(50), Color.YELLOW);
        gui.msg("MISSING ITEMS REPORT:", Color.YELLOW);
        gui.msg("=".repeat(50), Color.YELLOW);

        // Sort by position for easier reading
        missingItems.sort(Comparator.comparing(a -> a.position.x * 100 + a.position.y));

        for (MissingItem missing : missingItems) {
            gui.msg(String.format("  Missing: %s at position (%d, %d)",
                    missing.itemName, missing.position.x, missing.position.y), Color.ORANGE);
        }

        gui.msg("=".repeat(50), Color.YELLOW);
        gui.msg("Total missing items: " + missingItems.size(), Color.YELLOW);
    }

    /**
     * Helper class to store information about missing items
     */
    private static class MissingItem {
        String itemName;
        Coord position;
        Coord itemSize;

        MissingItem(String itemName, Coord position, Coord itemSize) {
            this.itemName = itemName;
            this.position = position;
            this.itemSize = itemSize;
        }
    }
}
