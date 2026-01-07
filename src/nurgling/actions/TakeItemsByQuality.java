package nurgling.actions;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import haven.res.ui.tt.stackn.Stack;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.*;

/**
 * Takes items from an open container to player inventory by quality (highest first).
 * Handles both stacked and non-stacked items, stacking in player inventory when possible.
 * Stops when player inventory is full or container is empty.
 */
public class TakeItemsByQuality implements Action {

    private static final Set<String> IGNORED_WINDOWS = new HashSet<>(Arrays.asList(
        "Character Sheet", "Equipment", "Inventory", "Belt", "Study",
        "Quest Log", "Kith & Kin", "Options", "Map", "Chat"
    ));

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Find open container
        ContainerInfo containerInfo = findOpenContainer(gui);
        if (containerInfo == null) {
            return Results.ERROR("No open container found");
        }

        NInventory containerInv = containerInfo.inventory;
        NInventory playerInv = gui.getInventory();

        // Collect all items with quality, sorted by quality descending
        List<ItemEntry> allItems = collectAllItemsSortedByQuality(containerInv);

        if (allItems.isEmpty()) {
            return Results.SUCCESS();
        }

        // Transfer items in quality order until inventory full or container empty
        for (ItemEntry item : allItems) {
            // Check if player inventory has space
            if (playerInv.getFreeSpace() <= 0) {
                break; // Inventory full, stop
            }

            // Re-fetch the item in case container state changed
            WItem currentItem = findItemInContainer(containerInv, item);
            if (currentItem == null) {
                continue; // Item no longer exists
            }

            // Transfer this item to player inventory
            Results result = extractToPlayerInventory(gui, currentItem, playerInv, item.name);
            if (!result.isSuccess) {
                continue; // If failed, try next item
            }
        }

        return Results.SUCCESS();
    }

    private static class ItemEntry {
        WItem witem;
        String name;
        double quality;
        boolean isStack;
        int stackSize;

        ItemEntry(WItem witem, String name, double quality, boolean isStack, int stackSize) {
            this.witem = witem;
            this.name = name;
            this.quality = quality;
            this.isStack = isStack;
            this.stackSize = stackSize;
        }
    }

    private static class ContainerInfo {
        NInventory inventory;
        String windowName;

        ContainerInfo(NInventory inventory, String windowName) {
            this.inventory = inventory;
            this.windowName = windowName;
        }
    }

    private ContainerInfo findOpenContainer(NGameUI gui) {
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (w instanceof Window) {
                Window wnd = (Window) w;
                if (wnd.cap != null && !IGNORED_WINDOWS.contains(wnd.cap)) {
                    for (Widget sp = wnd.lchild; sp != null; sp = sp.prev) {
                        if (sp instanceof NInventory) {
                            NInventory inv = (NInventory) sp;
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

    private List<ItemEntry> collectAllItemsSortedByQuality(NInventory containerInv) throws InterruptedException {
        List<ItemEntry> result = new ArrayList<>();

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

            if (witem.item.contents != null && witem.item.contents instanceof ItemStack) {
                ItemStack stack = (ItemStack) witem.item.contents;
                stackSize = stack.wmap.size();
                isStack = true;

                Stack stackInfo = ngitem.getInfo(Stack.class);
                if (stackInfo != null && stackInfo.quality > 0) {
                    quality = stackInfo.quality;
                } else {
                    quality = calculateStackQuality(stack);
                }
            } else {
                isStack = false;
                stackSize = 1;
                quality = ngitem.quality != null ? ngitem.quality : -1;
            }

            // Skip items with unknown quality
            if (quality < 0) {
                continue;
            }

            result.add(new ItemEntry(witem, name, quality, isStack, stackSize));
        }

        // Sort by quality descending (highest first)
        result.sort((a, b) -> Double.compare(b.quality, a.quality));

        return result;
    }

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

    private WItem findItemInContainer(NInventory containerInv, ItemEntry entry) throws InterruptedException {
        ArrayList<WItem> items = containerInv.getItems();

        for (WItem witem : items) {
            if (witem == entry.witem) {
                return witem;
            }
        }

        // Try to find by characteristics
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

                if (Math.abs(quality - entry.quality) < 0.01) {
                    return witem;
                }
            }
        }

        return null;
    }

    private Results extractToPlayerInventory(NGameUI gui, WItem item, NInventory playerInv,
                                             String itemType) throws InterruptedException {

        if (item.item.contents != null && item.item.contents instanceof ItemStack) {
            return transferStackToPlayer(gui, item, playerInv);
        } else {
            return transferSingleToPlayer(gui, item, playerInv, itemType);
        }
    }

    private Results transferStackToPlayer(NGameUI gui, WItem stackItem, NInventory playerInv) throws InterruptedException {
        if (stackItem.item.contents != null && stackItem.item.contents instanceof ItemStack) {
            stackItem.item.wdgmsg("transfer", Coord.z);
            int id = stackItem.item.wdgid();
            NUtils.addTask(new ISRemoved(id));
            return Results.SUCCESS();
        }
        return Results.FAIL();
    }

    private Results transferSingleToPlayer(NGameUI gui, WItem item, NInventory playerInv,
                                           String itemType) throws InterruptedException {

        boolean isStackable = StackSupporter.isStackable(playerInv, itemType);

        if (isStackable) {
            ItemStack targetStack = playerInv.findNotFullStack(itemType);
            WItem targetSingle = playerInv.findNotStack(itemType);

            if (targetStack != null) {
                int targetStackSize = targetStack.wmap.size();
                NUtils.takeItemToHand(item);
                NUtils.itemact(((NGItem) ((GItem.ContentsWindow) targetStack.parent).cont).wi);
                NUtils.addTask(new WaitFreeHand());
                NUtils.addTask(new StackSizeChanged(targetStack, targetStackSize));
                return Results.SUCCESS();
            } else if (targetSingle != null) {
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
}
