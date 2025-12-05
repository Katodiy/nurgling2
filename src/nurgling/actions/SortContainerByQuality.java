package nurgling.actions;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import haven.res.ui.tt.stackn.Stack;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.*;

/**
 * Sorts items in an open container by quality.
 * Groups items by type, sorts each group by quality (descending),
 * and stacks items optimally.
 *
 * Prerequisites:
 * - Player inventory must be empty
 * - A container window must be open
 */
public class SortContainerByQuality implements Action {

    // Windows to ignore when looking for container
    private static final Set<String> IGNORED_WINDOWS = new HashSet<>(Arrays.asList(
        "Character Sheet", "Equipment", "Inventory", "Belt", "Study",
        "Quest Log", "Kith & Kin", "Options", "Map", "Chat"
    ));

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Step 1: Validate player inventory is empty
        NInventory playerInv = gui.getInventory();
        ArrayList<WItem> playerItems = playerInv.getItems();
        if (!playerItems.isEmpty()) {
            return Results.ERROR("Player inventory must be empty to sort container");
        }

        // Step 2: Find open container inventory
        ContainerInfo containerInfo = findOpenContainer(gui);
        if (containerInfo == null) {
            return Results.ERROR("No open container found");
        }

        NInventory containerInv = containerInfo.inventory;
        String containerName = containerInfo.windowName;

        // Step 3: Get all items and group by type
        Map<String, List<ItemEntry>> itemsByType = collectAndGroupItems(containerInv);

        if (itemsByType.isEmpty()) {
            return Results.SUCCESS(); // Nothing to sort
        }

        // Step 4: Process each item type
        List<Map.Entry<String, List<ItemEntry>>> typeList = new ArrayList<>(itemsByType.entrySet());

        for (int i = 0; i < typeList.size(); i++) {
            Map.Entry<String, List<ItemEntry>> entry = typeList.get(i);
            String itemType = entry.getKey();
            List<ItemEntry> items = entry.getValue();

            if (items.size() <= 1) {
                continue; // Nothing to sort for single items
            }

            // Check if we need to clear player inventory before processing this type
            int requiredSlots = estimateRequiredSlots(items, itemType);
            int availableSlots = playerInv.getFreeSpace();

            if (availableSlots < requiredSlots) {
                // Not enough room - transfer all items back to container first
                Results transferResult = transferAllPlayerItemsToContainer(gui, playerInv, containerInv);
                if (!transferResult.isSuccess) {
                    return transferResult;
                }
            }

            // Sort by quality descending (highest first)
            items.sort((a, b) -> Double.compare(b.quality, a.quality));

            // Process this item type (extract and sort, but don't transfer back yet)
            Results result = processItemType(gui, containerInv, playerInv, itemType, items);
            if (!result.isSuccess) {
                return result;
            }
        }

        // Transfer any remaining items back to container at the end
        Results finalTransfer = transferAllPlayerItemsToContainer(gui, playerInv, containerInv);
        if (!finalTransfer.isSuccess) {
            return finalTransfer;
        }

        return Results.SUCCESS();
    }

    /**
     * Estimate how many inventory slots will be needed for a list of items (considering stacking)
     */
    private int estimateRequiredSlots(List<ItemEntry> items, String itemType) {
        int totalItems = 0;
        for (ItemEntry entry : items) {
            totalItems += entry.stackSize;
        }

        int maxStackSize = StackSupporter.getMaxStackSize(itemType);
        if (maxStackSize <= 1) {
            return totalItems;
        }

        // Calculate slots needed with stacking
        return (int) Math.ceil((double) totalItems / maxStackSize);
    }

    /**
     * Holds information about an item for sorting
     */
    private static class ItemEntry {
        WItem witem;           // The WItem in the container
        String name;           // Item name
        double quality;        // Quality (or average for stacks)
        boolean isStack;       // True if this is a stack container
        int stackSize;         // Number of items if stack, 1 if single

        ItemEntry(WItem witem, String name, double quality, boolean isStack, int stackSize) {
            this.witem = witem;
            this.name = name;
            this.quality = quality;
            this.isStack = isStack;
            this.stackSize = stackSize;
        }
    }

    /**
     * Container info holder
     */
    private static class ContainerInfo {
        NInventory inventory;
        String windowName;

        ContainerInfo(NInventory inventory, String windowName) {
            this.inventory = inventory;
            this.windowName = windowName;
        }
    }

    /**
     * Find an open container window and its inventory
     */
    private ContainerInfo findOpenContainer(NGameUI gui) {
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (w instanceof Window) {
                Window wnd = (Window) w;
                if (wnd.cap != null && !IGNORED_WINDOWS.contains(wnd.cap)) {
                    // Look for inventory in this window
                    for (Widget sp = wnd.lchild; sp != null; sp = sp.prev) {
                        if (sp instanceof NInventory) {
                            NInventory inv = (NInventory) sp;
                            // Make sure it's not the main inventory
                            if (inv != gui.maininv) {
                                return new ContainerInfo(inv, wnd.cap);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Collect all items from container and group by type
     */
    private Map<String, List<ItemEntry>> collectAndGroupItems(NInventory containerInv) throws InterruptedException {
        Map<String, List<ItemEntry>> result = new LinkedHashMap<>();

        ArrayList<WItem> allItems = containerInv.getItems();

        for (WItem witem : allItems) {
            if (!NGItem.validateItem(witem)) {
                continue;
            }

            NGItem ngitem = (NGItem) witem.item;
            String name = ngitem.name();
            if (name == null) {
                continue;
            }

            double quality;
            boolean isStack;
            int stackSize;

            // Check if this is a stack
            if (witem.item.contents != null && witem.item.contents instanceof ItemStack) {
                ItemStack stack = (ItemStack) witem.item.contents;
                stackSize = stack.wmap.size();
                isStack = true;

                // Get stack quality (average)
                Stack stackInfo = ngitem.getInfo(Stack.class);
                if (stackInfo != null && stackInfo.quality > 0) {
                    quality = stackInfo.quality;
                } else {
                    // Calculate manually if needed
                    quality = calculateStackQuality(stack);
                }
            } else {
                // Single item
                isStack = false;
                stackSize = 1;
                quality = ngitem.quality != null ? ngitem.quality : -1;
            }

            ItemEntry entry = new ItemEntry(witem, name, quality, isStack, stackSize);

            result.computeIfAbsent(name, k -> new ArrayList<>()).add(entry);
        }

        return result;
    }

    /**
     * Calculate average quality for a stack
     */
    private double calculateStackQuality(ItemStack stack) {
        double total = 0;
        int count = 0;

        for (GItem gitem : stack.order) {
            if (gitem instanceof NGItem) {
                NGItem ngitem = (NGItem) gitem;
                if (ngitem.quality != null) {
                    total += ngitem.quality;
                    count++;
                }
            }
        }

        return count > 0 ? total / count : -1;
    }

    /**
     * Process a single item type: extract in quality order, stack in player inv.
     * Does NOT transfer back - caller is responsible for that.
     */
    private Results processItemType(NGameUI gui, NInventory containerInv, NInventory playerInv,
                                    String itemType, List<ItemEntry> items) throws InterruptedException {

        boolean isStackable = StackSupporter.isStackable(playerInv, itemType);

        // Process items in quality order (already sorted descending)
        for (ItemEntry item : items) {
            // Re-fetch the item in case container state changed
            WItem currentItem = findItemInContainer(containerInv, item);
            if (currentItem == null) {
                continue; // Item no longer exists
            }

            // Check if player inventory has space
            if (playerInv.getFreeSpace() <= 0) {
                // Transfer all sorted items back to container to make room
                Results transferResult = transferAllPlayerItemsToContainer(gui, playerInv, containerInv);
                if (!transferResult.isSuccess) {
                    return transferResult;
                }
            }

            // Extract this item to player inventory
            Results extractResult = extractToPlayerInventory(gui, currentItem, playerInv, itemType, isStackable);
            if (!extractResult.isSuccess) {
                // If extraction failed, try to continue with next item
                continue;
            }
        }

        return Results.SUCCESS();
    }

    /**
     * Find an item in the container (it may have moved)
     */
    private WItem findItemInContainer(NInventory containerInv, ItemEntry entry) throws InterruptedException {
        ArrayList<WItem> items = containerInv.getItems();

        for (WItem witem : items) {
            if (witem == entry.witem) {
                return witem;
            }
        }

        // Item not found by reference, try to find by characteristics
        for (WItem witem : items) {
            if (!NGItem.validateItem(witem)) {
                continue;
            }

            NGItem ngitem = (NGItem) witem.item;
            String name = ngitem.name();

            if (name != null && name.equals(entry.name)) {
                double quality;
                if (witem.item.contents != null && witem.item.contents instanceof ItemStack) {
                    Stack stackInfo = ngitem.getInfo(Stack.class);
                    quality = stackInfo != null ? stackInfo.quality : -1;
                } else {
                    quality = ngitem.quality != null ? ngitem.quality : -1;
                }

                // Check if quality matches (with small tolerance for floating point)
                if (Math.abs(quality - entry.quality) < 0.01) {
                    return witem;
                }
            }
        }

        return null;
    }

    /**
     * Extract a single item/stack to player inventory, stacking if possible
     */
    private Results extractToPlayerInventory(NGameUI gui, WItem item, NInventory playerInv,
                                             String itemType, boolean isStackable) throws InterruptedException {

        if (item.item.contents != null && item.item.contents instanceof ItemStack) {
            // It's a stack - transfer the whole stack
            return transferStackToPlayer(gui, item, playerInv);
        } else {
            // Single item - try to stack with existing, or place new
            return transferSingleToPlayer(gui, item, playerInv, itemType, isStackable);
        }
    }

    /**
     * Transfer a stack to player inventory
     */
    private Results transferStackToPlayer(NGameUI gui, WItem stackItem, NInventory playerInv) throws InterruptedException {
        // Get the containing GItem for the stack
        if (stackItem.item.contents != null && stackItem.item.contents instanceof ItemStack) {
            ItemStack stack = (ItemStack) stackItem.item.contents;
            int stackSize = stack.wmap.size();

            // Transfer the whole stack using the outer item
            stackItem.item.wdgmsg("transfer", Coord.z);
            int id = stackItem.item.wdgid();
            NUtils.addTask(new ISRemoved(id));

            return Results.SUCCESS();
        }

        return Results.FAIL();
    }

    /**
     * Transfer a single item to player inventory, stacking if possible
     */
    private Results transferSingleToPlayer(NGameUI gui, WItem item, NInventory playerInv,
                                           String itemType, boolean isStackable) throws InterruptedException {

        if (isStackable) {
            // Try to find existing stack or single item to merge with
            ItemStack targetStack = playerInv.findNotFullStack(itemType);
            WItem targetSingle = playerInv.findNotStack(itemType);

            if (targetStack != null) {
                // Stack onto existing stack
                int targetStackSize = targetStack.wmap.size();
                NUtils.takeItemToHand(item);
                NUtils.itemact(((NGItem) ((GItem.ContentsWindow) targetStack.parent).cont).wi);
                NUtils.addTask(new WaitFreeHand());
                NUtils.addTask(new StackSizeChanged(targetStack, targetStackSize));
                return Results.SUCCESS();
            } else if (targetSingle != null) {
                // Stack with single item to create new stack
                NUtils.takeItemToHand(item);
                NUtils.itemact(targetSingle);
                NUtils.addTask(new WaitFreeHand());
                return Results.SUCCESS();
            }
        }

        // No stacking target or not stackable - transfer to free space
        if (playerInv.getFreeSpace() > 0) {
            item.item.wdgmsg("transfer", Coord.z);
            int id = item.item.wdgid();
            NUtils.addTask(new ISRemoved(id));
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }

    /**
     * Transfer ALL items from player inventory back to container (all types).
     * Re-fetches items each iteration to avoid stale references.
     */
    private Results transferAllPlayerItemsToContainer(NGameUI gui, NInventory playerInv, NInventory containerInv)
            throws InterruptedException {

        // Keep transferring until player inventory is empty
        WItem item;
        while ((item = getFirstTopLevelItem(playerInv)) != null) {
            // Check if container has space
            if (containerInv.getFreeSpace() <= 0) {
                return Results.ERROR("Container is full");
            }

            // Transfer to container - works for both stacks and single items
            item.item.wdgmsg("transfer", Coord.z);
            int id = item.item.wdgid();
            NUtils.addTask(new ISRemoved(id));
        }

        return Results.SUCCESS();
    }

    /**
     * Get the first top-level WItem from inventory (any type).
     * Returns null if inventory is empty.
     */
    private WItem getFirstTopLevelItem(NInventory inv) {
        // Iterate direct children of inventory
        for (Widget w = inv.child; w != null; w = w.next) {
            if (w instanceof WItem) {
                WItem witem = (WItem) w;
                if (NGItem.validateItem(witem)) {
                    return witem;
                }
            }
        }

        return null;
    }
}
