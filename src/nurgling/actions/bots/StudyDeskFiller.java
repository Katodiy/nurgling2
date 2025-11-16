package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.UI;
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

        // Step 2: Navigate to study desk area
        NArea studyDeskArea = getStudyDeskArea(gui);
        if (studyDeskArea == null) {
            gui.msg("ERROR: No study desk area found! Please create one first.", Color.RED);
            return Results.ERROR("No study desk area found! Please create one first");
        }

        // Step 3: Find the specific study desk by hash
        Gob studyDesk = Finder.findGob(studyDeskHash);
        if (studyDesk == null) {
            gui.msg("ERROR: Could not find study desk with hash: " + studyDeskHash, Color.RED);
            return Results.ERROR("Could not find study desk with hash: " + studyDeskHash);
        }

        // Step 4: Navigate to the study desk
        new PathFinder(studyDesk).run(gui);

        // Step 5: Open the study desk
        new OpenTargetContainer("Study Desk", studyDesk).run(gui);

        // Step 6: Get the study desk inventory
        NInventory studyDeskInv = gui.getInventory("Study Desk");
        if (studyDeskInv == null) {
            gui.msg("ERROR: Could not access study desk inventory!", Color.RED);
            return Results.ERROR("ERROR: Could not access study desk inventory!");
        }

        // Step 7: Build map of current item positions
        Map<Coord, WItem> currentItems = buildCurrentItemsMap(studyDeskInv);

        // Step 8: Find missing items and conflicts
        List<ConflictItem> conflicts = new ArrayList<>();
        List<MissingItem> missingItems = findMissingItems(plannedLayout, currentItems, conflicts);

        // Step 9: Report results
        reportMissingItems(gui, missingItems);
        reportConflicts(gui, conflicts);

        // Step 10: Fetch and place missing items
        if (!missingItems.isEmpty()) {
            fetchAndPlaceAllItems(gui, missingItems, studyDesk, studyDeskInv);
        }

        // Step 11: Final status message
        if (missingItems.isEmpty() && conflicts.isEmpty()) {
            gui.msg("Study desk layout matches plan perfectly!", Color.GREEN);
        } else if (missingItems.isEmpty() && !conflicts.isEmpty()) {
            gui.msg("Study desk has conflicts that need manual resolution.", Color.ORANGE);
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
     * Check if placing an item at the given position would collide with existing items
     */
    private boolean isPositionOccupied(Coord plannedPos, Coord plannedSize, Map<Coord, WItem> currentItems) throws InterruptedException {
        // Check if any existing item would overlap with the planned item area
        for (Map.Entry<Coord, WItem> entry : currentItems.entrySet()) {
            Coord existingPos = entry.getKey();
            WItem existingItem = entry.getValue();

            // Get size of existing item
            Coord existingSize = getItemSize(existingItem);

            // Check for rectangle overlap using standard algorithm
            // Two rectangles overlap if ALL of these are true:
            // - planned left edge is left of existing right edge
            // - planned right edge is right of existing left edge
            // - planned top edge is above existing bottom edge
            // - planned bottom edge is below existing top edge
            boolean overlaps = (plannedPos.x < existingPos.x + existingSize.x) &&
                              (plannedPos.x + plannedSize.x > existingPos.x) &&
                              (plannedPos.y < existingPos.y + existingSize.y) &&
                              (plannedPos.y + plannedSize.y > existingPos.y);

            if (overlaps) {
                return true; // Collision detected
            }
        }

        return false; // No collision, position is free
    }

    /**
     * Check if the current item matches the planned item (same name and size)
     */
    private boolean isMatchingItem(WItem currentItem, String plannedName, Coord plannedSize) throws InterruptedException {
        if (currentItem == null || currentItem.item == null) {
            return false;
        }

        // Get current item name
        String currentName = null;
        if (currentItem.item instanceof NGItem) {
            currentName = ((NGItem) currentItem.item).name();
        }

        if (currentName == null || !currentName.equals(plannedName)) {
            return false;
        }

        // Get current item size
        Coord currentSize = getItemSize(currentItem);

        // Check if sizes match
        return currentSize.equals(plannedSize);
    }

    /**
     * Get the size of a WItem in grid coordinates
     */
    private Coord getItemSize(WItem item) {
        if (item != null && item.item != null && item.item.spr != null) {
            // Use same approach as planner widget: divide by UI.scale(32)
            Coord size = item.item.spr.sz().div(UI.scale(32));
            return size;
        }
        // Default to 1x1 if size cannot be determined
        return new Coord(1, 1);
    }

    /**
     * Find missing items by comparing planned layout vs current items
     */
    private List<MissingItem> findMissingItems(Map<String, Object> plannedLayout, Map<Coord, WItem> currentItems, List<ConflictItem> conflicts) throws InterruptedException {
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
            if (itemData.containsKey("sizeX") && itemData.containsKey("sizeY")) {
                // New format: sizeX and sizeY as separate keys
                itemSize = new Coord(
                    ((Number) itemData.get("sizeX")).intValue(),
                    ((Number) itemData.get("sizeY")).intValue()
                );
            } else if (itemData.containsKey("size")) {
                // Old format: size as nested object (backward compatibility)
                Map<String, Object> sizeData = (Map<String, Object>) itemData.get("size");
                itemSize = new Coord(
                    ((Number) sizeData.get("x")).intValue(),
                    ((Number) sizeData.get("y")).intValue()
                );
            }

            // First check: Is the exact item already at this position?
            WItem currentItem = currentItems.get(plannedPos);
            if (currentItem != null && isMatchingItem(currentItem, itemName, itemSize)) {
                // Item is already correctly placed, skip
                continue;
            }

            // Second check: Would placing this item cause a collision?
            if (isPositionOccupied(plannedPos, itemSize, currentItems)) {
                // Cannot place - area is occupied by something else
                // Find which item is blocking
                String blockingItemName = findBlockingItemName(plannedPos, itemSize, currentItems);
                Coord blockingPos = findBlockingItemPosition(plannedPos, itemSize, currentItems);
                conflicts.add(new ConflictItem(itemName, plannedPos, itemSize, blockingItemName, blockingPos));
                continue;
            }

            // Item is missing and can be placed
            missingItems.add(new MissingItem(itemName, plannedPos, itemSize));
        }

        return missingItems;
    }

    /**
     * Find the name of the item blocking the planned position
     */
    private String findBlockingItemName(Coord plannedPos, Coord plannedSize, Map<Coord, WItem> currentItems) throws InterruptedException {
        for (Map.Entry<Coord, WItem> entry : currentItems.entrySet()) {
            Coord existingPos = entry.getKey();
            WItem existingItem = entry.getValue();
            Coord existingSize = getItemSize(existingItem);

            boolean overlaps = (plannedPos.x < existingPos.x + existingSize.x) &&
                              (plannedPos.x + plannedSize.x > existingPos.x) &&
                              (plannedPos.y < existingPos.y + existingSize.y) &&
                              (plannedPos.y + plannedSize.y > existingPos.y);

            if (overlaps && existingItem.item instanceof NGItem) {
                String name = ((NGItem) existingItem.item).name();
                return name != null ? name : "Unknown Item";
            }
        }
        return "Unknown Item";
    }

    /**
     * Find the position of the item blocking the planned position
     */
    private Coord findBlockingItemPosition(Coord plannedPos, Coord plannedSize, Map<Coord, WItem> currentItems) throws InterruptedException {
        for (Map.Entry<Coord, WItem> entry : currentItems.entrySet()) {
            Coord existingPos = entry.getKey();
            WItem existingItem = entry.getValue();
            Coord existingSize = getItemSize(existingItem);

            boolean overlaps = (plannedPos.x < existingPos.x + existingSize.x) &&
                              (plannedPos.x + plannedSize.x > existingPos.x) &&
                              (plannedPos.y < existingPos.y + existingSize.y) &&
                              (plannedPos.y + plannedSize.y > existingPos.y);

            if (overlaps) {
                return existingPos;
            }
        }
        return null;
    }

    /**
     * Fetch and place all missing items into the study desk
     */
    private void fetchAndPlaceAllItems(NGameUI gui, List<MissingItem> missingItems, Gob studyDesk, NInventory studyDeskInv) throws InterruptedException {
        // Create a working list of items still needed
        List<MissingItem> remainingItems = new ArrayList<>(missingItems);

        // Set up NContext once for all items
        NContext context = new NContext(gui);
        Set<String> uniqueItems = new HashSet<>();
        for (MissingItem missing : remainingItems) {
            if (!uniqueItems.contains(missing.itemName)) {
                context.addInItem(missing.itemName, null);
                uniqueItems.add(missing.itemName);
            }
        }

        // Keep fetching and placing until all items are done
        while (!remainingItems.isEmpty()) {

            // Fill inventory with as many items as possible
            List<FetchedItem> fetchedItems = fetchBatchUntilFull(gui, context, remainingItems);

            if (fetchedItems.isEmpty()) {
                break;
            }

            // Navigate back to study desk and place everything
            getStudyDeskArea(gui);
            new PathFinder(studyDesk).run(gui);
            new OpenTargetContainer("Study Desk", studyDesk).run(gui);

            // Refresh study desk inventory reference
            studyDeskInv = gui.getInventory("Study Desk");
            if (studyDeskInv == null) {
                gui.msg("ERROR: Lost study desk inventory reference!", Color.RED);
                break;
            }

            // Place all fetched items
            for (FetchedItem fetchedItem : fetchedItems) {
                placeItemInDesk(gui, fetchedItem.item, fetchedItem.targetPosition, studyDeskInv);
                remainingItems.remove(fetchedItem.originalMissingItem);
            }
        }
    }

    /**
     * Fetch as many items as possible until inventory is full
     * Removes items from remainingItems that cannot be fetched (storage depleted)
     */
    private List<FetchedItem> fetchBatchUntilFull(NGameUI gui, NContext context, List<MissingItem> remainingItems) throws InterruptedException {
        List<FetchedItem> fetchedItems = new ArrayList<>();
        List<MissingItem> itemsToRemove = new ArrayList<>();

        // Group remaining items by type
        Map<String, List<MissingItem>> itemGroups = new HashMap<>();
        for (MissingItem missing : remainingItems) {
            itemGroups.computeIfAbsent(missing.itemName, k -> new ArrayList<>()).add(missing);
        }

        // Try to fetch each item type until inventory is full
        for (Map.Entry<String, List<MissingItem>> entry : itemGroups.entrySet()) {
            String itemName = entry.getKey();
            List<MissingItem> itemsNeeded = entry.getValue();
            Coord itemSize = itemsNeeded.get(0).itemSize;

            // Check how many of this item can fit
            int canFit = gui.getInventory().getNumberFreeCoord(itemSize);
            if (canFit == 0) {
                continue; // Inventory full, try next item type
            }

            // Get storage for this item
            ArrayList<NContext.ObjectStorage> storages = context.getInStorages(itemName);
            if (storages == null || storages.isEmpty()) {
                gui.msg("No storage found for " + itemName + ", removing from list", Color.ORANGE);
                // Mark all items of this type for removal since there's no storage
                itemsToRemove.addAll(itemsNeeded);
                continue;
            }

            // Fetch what we can
            int toFetch = Math.min(itemsNeeded.size(), canFit);
            NAlias itemAlias = new NAlias(itemName);

            int beforeCount = gui.getInventory().getItems(itemAlias).size();
            fetchItemsFromStorage(gui, storages, itemName, toFetch);
            int afterCount = gui.getInventory().getItems(itemAlias).size();
            int actuallyFetched = afterCount - beforeCount;

            // Record which items we fetched with their target positions
            if (actuallyFetched > 0) {
                ArrayList<WItem> fetchedWItems = gui.getInventory().getItems(itemAlias);
                // Take the last N items (most recently added)
                int startIdx = Math.max(0, fetchedWItems.size() - actuallyFetched);
                for (int i = 0; i < actuallyFetched && i < itemsNeeded.size(); i++) {
                    WItem item = fetchedWItems.get(startIdx + i);
                    MissingItem target = itemsNeeded.get(i);
                    fetchedItems.add(new FetchedItem(item, target.position, target));
                }
            }

            // If we got fewer items than requested, storage is depleted
            // Remove the unfetched items from remainingItems so we don't try again
            if (actuallyFetched < toFetch) {
                gui.msg("Storage depleted for " + itemName + " (got " + actuallyFetched + "/" + toFetch + ")", Color.ORANGE);
                // Mark unfetched items for removal
                for (int i = actuallyFetched; i < itemsNeeded.size(); i++) {
                    itemsToRemove.add(itemsNeeded.get(i));
                }
            }
        }

        // Remove items that couldn't be fetched
        remainingItems.removeAll(itemsToRemove);

        return fetchedItems;
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
     * Place a single item into study desk at exact position
     */
    private void placeItemInDesk(NGameUI gui, WItem item, Coord targetPosition, NInventory studyDeskInv) throws InterruptedException {
        // Take item to hand
        NUtils.takeItemToHand(item);

        // Wait for item to be in hand
        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.vhand != null;
            }
        });

        // Get item name for dropOn
        String itemName = ((NGItem) item.item).name();

        // Drop at precise position
        studyDeskInv.dropOn(targetPosition, itemName);

        // Wait for slot to be filled
        Coord finalPos = targetPosition;
        NUtils.getUI().core.addTask(new NTask() {
            @Override
            public boolean check() {
                return !studyDeskInv.isSlotFree(finalPos);
            }
        });
    }

    /**
     * Report missing items to the user
     */
    private void reportMissingItems(NGameUI gui, List<MissingItem> missingItems) {
        if (missingItems.isEmpty()) {
            return;
        }

        // Sort by position for easier reading
        missingItems.sort(Comparator.comparing(a -> a.position.x * 100 + a.position.y));
    }

    /**
     * Report conflicts to the user
     */
    private void reportConflicts(NGameUI gui, List<ConflictItem> conflicts) {
        if (conflicts.isEmpty()) {
            return;
        }

        gui.msg("WARNING: " + conflicts.size() + " planned items cannot be placed due to conflicts", Color.ORANGE);

        for (ConflictItem conflict : conflicts) {
            String sizeStr = conflict.plannedSize.x + "x" + conflict.plannedSize.y;
            gui.msg("  [" + sizeStr + "] " + conflict.plannedItemName + " at (" +
                   conflict.plannedPosition.x + "," + conflict.plannedPosition.y +
                   ") blocked by " + conflict.blockingItemName +
                   " at (" + conflict.blockingPosition.x + "," + conflict.blockingPosition.y + ")",
                   Color.ORANGE);
        }
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

    /**
     * Helper class to store information about conflicting items
     */
    private static class ConflictItem {
        String plannedItemName;
        Coord plannedPosition;
        Coord plannedSize;
        String blockingItemName;
        Coord blockingPosition;

        ConflictItem(String plannedItemName, Coord plannedPosition, Coord plannedSize,
                    String blockingItemName, Coord blockingPosition) {
            this.plannedItemName = plannedItemName;
            this.plannedPosition = plannedPosition;
            this.plannedSize = plannedSize;
            this.blockingItemName = blockingItemName;
            this.blockingPosition = blockingPosition;
        }
    }

    /**
     * Helper class to store fetched items with their target positions
     */
    private static class FetchedItem {
        WItem item;
        Coord targetPosition;
        MissingItem originalMissingItem;

        FetchedItem(WItem item, Coord targetPosition, MissingItem originalMissingItem) {
            this.item = item;
            this.targetPosition = targetPosition;
            this.originalMissingItem = originalMissingItem;
        }
    }
}
