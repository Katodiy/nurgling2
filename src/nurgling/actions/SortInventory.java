package nurgling.actions;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import haven.res.ui.tt.stackn.Stack;
import nurgling.*;
import nurgling.tasks.*;
import nurgling.tools.StackSupporter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sorts inventory items by name, resource name, and quality.
 * Moves all 1x1 items to fill empty slots from top-left, sorted alphabetically and by quality.
 */
public class SortInventory implements Action {
    
    public static final String[] EXCLUDE_WINDOWS = new String[]{
        "Character Sheet",
        "Study",
        "Chicken Coop",
        "Belt",
        "Pouch",
        "Purse",
        "Cauldron",
        "Finery Forge",
        "Fireplace",
        "Frame",
        "Herbalist Table",
        "Kiln",
        "Ore Smelter",
        "Smith's Smelter",
        "Oven",
        "Pane mold",
        "Rack",
        "Smoke shed",
        "Stack Furnace",
        "Steelbox",
        "Tub"
    };
    
    public static final Comparator<WItem> ITEM_COMPARATOR = (a, b) -> {
        // Both items must be NGItem
        if (!(a.item instanceof NGItem) || !(b.item instanceof NGItem)) {
            return 0;
        }
        
        NGItem itemA = (NGItem) a.item;
        NGItem itemB = (NGItem) b.item;
        
        // Compare by name first
        String nameA = itemA.name();
        String nameB = itemB.name();
        if (nameA == null) nameA = "";
        if (nameB == null) nameB = "";
        int nameCompare = nameA.compareTo(nameB);
        if (nameCompare != 0) return nameCompare;

        // Then by quality (higher quality first)
        // Use stack quality if available, otherwise use item quality
        double qualA = getEffectiveQuality(itemA);
        double qualB = getEffectiveQuality(itemB);
        return Double.compare(qualB, qualA);
    };
    
    /**
     * Get effective quality for an item, considering stack quality for stacked items
     */
    private static double getEffectiveQuality(NGItem item) {
        // First try to get stack quality (for stacked items)
        Stack stackInfo = item.getInfo(Stack.class);
        if (stackInfo != null && stackInfo.quality > 0) {
            return stackInfo.quality;
        }
        // Fall back to individual item quality
        if (item.quality != null && item.quality > 0) {
            return item.quality;
        }
        return -1; // No quality available
    }
    
    private final NInventory inventory;
    private volatile boolean cancelled = false;
    private static volatile SortInventory current;
    private static final Object lock = new Object();
    
    public SortInventory(NInventory inventory) {
        this.inventory = inventory;
    }
    
    /**
     * Check if cursor is default (not holding anything or special cursor)
     */
    private boolean isDefaultCursor(NGameUI gui) {
        return gui.vhand == null;
    }
    
    /**
     * Get item size in inventory cells
     */
    private Coord getItemSize(WItem item) {
        if (item.item.spr != null) {
            return item.item.spr.sz().div(UI.scale(32));
        }
        return new Coord(1, 1);
    }
    
    /**
     * Get item position in inventory grid
     */
    private Coord getItemPos(WItem item) {
        return item.c.sub(1, 1).div(Inventory.sqsz);
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Check for default cursor
        if (!isDefaultCursor(gui)) {
            gui.error("Need default cursor to sort inventory!");
            return Results.FAIL();
        }
        
        // Cancel any previous sort operation
        cancel();
        synchronized (lock) {
            current = this;
        }
        
        try {
            doSort(gui);
        } finally {
            synchronized (lock) {
                if (current == this) {
                    current = null;
                }
            }
        }
        
        if (!cancelled) {
            gui.msg("Inventory sorted!");
        }
        
        return cancelled ? Results.FAIL() : Results.SUCCESS();
    }
    
    private void doSort(NGameUI gui) throws InterruptedException {
        // Build grid of blocked cells (including sqmask and multi-cell items)
        boolean[][] grid = new boolean[inventory.isz.x][inventory.isz.y];
        
        // Apply sqmask if present
        boolean[] mask = inventory.sqmask;
        if (mask != null) {
            int mo = 0;
            for (int y = 0; y < inventory.isz.y; y++) {
                for (int x = 0; x < inventory.isz.x; x++) {
                    grid[x][y] = mask[mo++];
                }
            }
        }
        
        // Collect all items and mark multi-cell items as blocked
        List<WItem> items = new ArrayList<>();
        for (Widget wdg = inventory.lchild; wdg != null; wdg = wdg.prev) {
            if (cancelled) return;
            
            if (wdg.visible && wdg instanceof WItem) {
                WItem wItem = (WItem) wdg;
                Coord sz = getItemSize(wItem);
                Coord loc = getItemPos(wItem);
                
                if (sz.x * sz.y == 1) {
                    // 1x1 items can be sorted
                    items.add(wItem);
                } else {
                    // Multi-cell items stay in place, mark cells as blocked
                    for (int x = 0; x < sz.x; x++) {
                        for (int y = 0; y < sz.y; y++) {
                            int gx = loc.x + x;
                            int gy = loc.y + y;
                            if (gx >= 0 && gx < inventory.isz.x && gy >= 0 && gy < inventory.isz.y) {
                                grid[gx][gy] = true;
                            }
                        }
                    }
                }
            }
        }
        
        if (items.isEmpty()) {
            return;
        }
        
        // Sort items and create position mapping
        List<Object[]> sorted = items.stream()
            .filter(witem -> getItemSize(witem).x * getItemSize(witem).y == 1)
            .sorted(ITEM_COMPARATOR)
            .map(witem -> new Object[]{
                witem, 
                getItemPos(witem),  // current pos
                new Coord(0, 0)     // target pos (will be filled)
            })
            .collect(Collectors.toList());
        
        // Assign target positions
        int cur_x = -1, cur_y = 0;
        for (Object[] a : sorted) {
            if (cancelled) return;
            
            while (true) {
                cur_x += 1;
                if (cur_x == inventory.isz.x) {
                    cur_x = 0;
                    cur_y += 1;
                    if (cur_y == inventory.isz.y) break;
                }
                if (!grid[cur_x][cur_y]) {
                    a[2] = new Coord(cur_x, cur_y);
                    break;
                }
            }
            if (cur_y == inventory.isz.y) break;
        }
        
        // Move items to their target positions
        for (Object[] a : sorted) {
            if (cancelled) return;
            
            Coord currentPos = (Coord) a[1];
            Coord targetPos = (Coord) a[2];
            
            // Skip if already in right place
            if (currentPos.equals(targetPos)) {
                continue;
            }
            
            WItem wItem = (WItem) a[0];
            
            // Check if item is still valid
            if (wItem.item == null) {
                continue;
            }
            
            // Take item to hand
            NUtils.takeItemToHand(wItem);
            
            Object[] handu = a;
            while (handu != null) {
                if (cancelled) {
                    // Drop item back if cancelled
                    if (gui.vhand != null) {
                        NUtils.dropToInv(inventory);
                    }
                    return;
                }
                
                Coord dropPos = (Coord) handu[2];
                
                // Drop item at target position
                inventory.wdgmsg("drop", dropPos);
                
                // Find item that was at the target position (it's now in hand)
                Object[] b = null;
                for (Object[] x : sorted) {
                    if (((Coord) x[1]).equals(dropPos)) {
                        b = x;
                        break;
                    }
                }
                
                // Update current position
                handu[1] = handu[2];
                handu = b;
                
                // Wait a bit for the swap to happen
                if (handu != null) {
                    // Wait until we have something in hand or hand is free
                    NUtils.getUI().core.addTask(new WaitTicks(2));
                }
            }
            
            // Wait for hand to be free after chain is complete
            if (gui.vhand != null) {
                NUtils.getUI().core.addTask(new WaitFreeHand());
            }
        }

        // Second pass: sort individual items across same-type stacks by quality
        if (!cancelled) {
            sortWithinStacks(gui);
        }
    }
    
    /**
     * Cancel the current sort operation
     */
    public static void cancel() {
        synchronized (lock) {
            if (current != null) {
                current.cancelled = true;
                current = null;
            }
        }
    }
    
    /**
     * Check if a sort operation is currently running
     */
    public static boolean isRunning() {
        synchronized (lock) {
            return current != null;
        }
    }
    
    /**
     * Sort a specific inventory
     */
    public static void sort(NInventory inv) {
        if (!isValidInventory(inv)) {
            return;
        }
        
        NGameUI gui = NUtils.getGameUI();
        if (gui == null) return;
        
        // Check cursor
        if (gui.vhand != null) {
            gui.error("Need default cursor to sort inventory!");
            return;
        }
        
        Thread t = new Thread(() -> {
            try {
                new SortInventory(inv).run(gui);
            } catch (InterruptedException e) {
                gui.msg("Sort interrupted");
            }
        }, "InventorySorter");
        
        gui.biw.addObserve(t);
        t.start();
    }
    
    /**
     * Check if inventory is valid for sorting (not in excluded windows)
     */
    private static boolean isValidInventory(NInventory inv) {
        if (inv == null) return false;

        Window wnd = inv.getparent(Window.class);
        if (wnd != null) {
            String caption = wnd.cap;
            if (caption != null) {
                for (String excluded : EXCLUDE_WINDOWS) {
                    if (caption.contains(excluded)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // =========================================================================
    // Within-Stack Sorting (Second Pass) — Cycle-Chase Algorithm
    // =========================================================================

    private static class BufferLocation {
        final NInventory inv;
        final Coord coord;

        BufferLocation(NInventory inv, Coord coord) {
            this.inv = inv;
            this.coord = coord;
        }
    }

    /**
     * Sorts individual items across same-type stacks so that the highest
     * quality items are concentrated in the first stacks (descending).
     * Uses a cycle-chase permutation sort with a single-slot buffer.
     */
    private void sortWithinStacks(NGameUI gui) throws InterruptedException {
        // Wait a moment for the first-pass to fully settle in the UI
        NUtils.getUI().core.addTask(new WaitTicks(3));

        // Find item names that have at least one stack
        Set<String> namesWithStacks = new HashSet<>();
        for (Widget wdg = inventory.lchild; wdg != null; wdg = wdg.prev) {
            if (cancelled) return;
            if (!(wdg instanceof WItem)) continue;
            WItem w = (WItem) wdg;
            if (!(w.item instanceof NGItem)) continue;
            NGItem ng = (NGItem) w.item;
            if (ng.name() != null && w.item.contents instanceof ItemStack) {
                namesWithStacks.add(ng.name());
            }
        }
        if (namesWithStacks.isEmpty()) return;

        // Find buffer slot once
        BufferLocation buffer = findBuffer(gui);
        if (buffer == null) {
            gui.msg("Need 1 free inventory slot to sort within stacks");
            return;
        }

        for (String itemName : namesWithStacks) {
            if (cancelled) return;
            performCycleSort(gui, itemName, buffer);
        }
    }

    private List<List<Float>> computeTargetState(List<Float> sortedQualities, List<Integer> slotSizes) {
        List<List<Float>> target = new ArrayList<>();
        int idx = 0;
        for (int size : slotSizes) {
            List<Float> slot = new ArrayList<>();
            for (int j = 0; j < size && idx < sortedQualities.size(); j++, idx++) {
                slot.add(sortedQualities.get(idx));
            }
            target.add(slot);
        }
        return target;
    }

    private boolean isAlreadySorted(List<List<Float>> current, List<List<Float>> target) {
        for (int i = 0; i < current.size(); i++) {
            if (!multisetEquals(current.get(i), target.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean multisetEquals(List<Float> a, List<Float> b) {
        if (a.size() != b.size()) return false;
        List<Float> bCopy = new ArrayList<>(b);
        for (float v : a) {
            int idx = findFloatIdx(bCopy, v);
            if (idx < 0) return false;
            bCopy.remove(idx);
        }
        return true;
    }

    /**
     * Scans the inventory fresh for all slots of the given item type.
     * Returns a list of (position, qualities) pairs, sorted by position.
     * Only includes slots that have at least one item with non-null quality.
     */
    private List<Object[]> freshScan(String itemName) {
        List<Object[]> slots = new ArrayList<>();
        for (Widget wdg = inventory.lchild; wdg != null; wdg = wdg.prev) {
            if (!(wdg instanceof WItem)) continue;
            WItem w = (WItem) wdg;
            if (!(w.item instanceof NGItem)) continue;
            NGItem ng = (NGItem) w.item;
            if (!itemName.equals(ng.name())) continue;
            if (getItemSize(w).x * getItemSize(w).y != 1) continue;

            Coord pos = getItemPos(w);
            List<Float> quals = getSlotQualities(pos);
            if (!quals.isEmpty()) {
                slots.add(new Object[]{pos, quals});
            }
        }
        // Sort by position (top-to-bottom, left-to-right) for stable ordering
        slots.sort((a, b) -> {
            Coord pa = (Coord) a[0], pb = (Coord) b[0];
            return pa.y != pb.y ? Integer.compare(pa.y, pb.y) : Integer.compare(pa.x, pb.x);
        });
        return slots;
    }

    /**
     * Cycle-chase sorting: repeatedly find misplaced items and resolve
     * permutation cycles using a single-slot buffer.
     *
     * Each cycle starts with a FRESH inventory scan so positions, sizes,
     * and qualities are never stale.
     */
    private void performCycleSort(NGameUI gui, String itemName,
            BufferLocation buffer) throws InterruptedException {

        int cycleNum = 0;
        boolean announced = false;
        while (!cancelled) {
            cycleNum++;

            // === Fresh scan each cycle ===
            List<Object[]> scan = freshScan(itemName);
            if (scan.size() < 2) break;

            List<Coord> positions = new ArrayList<>();
            List<List<Float>> current = new ArrayList<>();
            List<Integer> slotSizes = new ArrayList<>();
            List<Float> allQualities = new ArrayList<>();

            for (Object[] entry : scan) {
                Coord pos = (Coord) entry[0];
                @SuppressWarnings("unchecked")
                List<Float> quals = (List<Float>) entry[1];
                positions.add(pos);
                current.add(quals);
                slotSizes.add(quals.size());
                allQualities.addAll(quals);
            }

            if (allQualities.size() < 2) break;

            // Compute target: all qualities sorted descending, distributed by slot sizes
            List<Float> sortedQualities = new ArrayList<>(allQualities);
            sortedQualities.sort(Collections.reverseOrder());
            List<List<Float>> target = computeTargetState(sortedQualities, slotSizes);

            // Find a misplacement
            int fromSlot = -1;
            float excessQ = 0;
            int toSlot = -1;

            outer:
            for (int s = 0; s < current.size(); s++) {
                List<Float> excess = multisetDiff(current.get(s), target.get(s));
                for (float q : excess) {
                    for (int t = 0; t < target.size(); t++) {
                        if (t == s) continue;
                        List<Float> deficit = multisetDiff(target.get(t), current.get(t));
                        if (containsFloat(deficit, q)) {
                            fromSlot = s;
                            excessQ = q;
                            toSlot = t;
                            break outer;
                        }
                    }
                }
            }

            if (fromSlot < 0) {
                System.out.println("[StackSort] " + itemName + ": all sorted after " + (cycleNum - 1) + " cycles");
                break;
            }

            if (!announced) {
                gui.msg("Sorting within " + itemName + " stacks...");
                announced = true;
            }

            System.out.println("[StackSort] === " + itemName + " cycle " + cycleNum
                    + ": move q=" + excessQ + " from slot " + fromSlot + " → slot " + toSlot + " ===");

            // --- Execute one cycle ---

            // Step 1: take excess item → buffer
            takeItemFromSlot(positions.get(fromSlot), excessQ);
            dropToBuffer(buffer);
            int bufferTarget = toSlot;
            int vacancy = fromSlot;

            // Step 2: chain — fill each vacancy from another slot
            // Use the target computed at cycle start (stable within this cycle)
            int chainStep = 0;
            while (bufferTarget != vacancy && !cancelled) {
                chainStep++;

                // Re-scan only the current state, keep target fixed
                List<List<Float>> chainCurrent = new ArrayList<>();
                for (Coord pos : positions) {
                    chainCurrent.add(getSlotQualities(pos));
                }

                List<Float> vacancyDeficit = multisetDiff(target.get(vacancy), chainCurrent.get(vacancy));

                float fillerQ = 0;
                int fillerSlot = -1;
                for (float needed : vacancyDeficit) {
                    for (int s = 0; s < chainCurrent.size(); s++) {
                        if (s == vacancy) continue;
                        List<Float> excess = multisetDiff(chainCurrent.get(s), target.get(s));
                        if (containsFloat(excess, needed)) {
                            fillerQ = needed;
                            fillerSlot = s;
                            break;
                        }
                    }
                    if (fillerSlot >= 0) break;
                }

                if (fillerSlot < 0) {
                    System.out.println("[StackSort]   Chain BROKE at step " + chainStep
                            + " — no filler for deficit=" + vacancyDeficit);
                    break;
                }

                System.out.println("[StackSort]   chain step " + chainStep + ": q=" + fillerQ
                        + " from slot " + fillerSlot + " → slot " + vacancy);

                takeItemFromSlot(positions.get(fillerSlot), fillerQ);
                addItemToSlot(positions.get(vacancy));
                vacancy = fillerSlot;
            }

            // Step 3: close cycle — buffer item → vacancy
            if (!cancelled) {
                System.out.println("[StackSort]   close: buffer → slot " + vacancy);
                retrieveFromBuffer(buffer);
                addItemToSlot(positions.get(vacancy));
            } else {
                if (gui.vhand == null) {
                    retrieveFromBuffer(buffer);
                }
                if (gui.vhand != null) {
                    NUtils.dropToInv(inventory);
                    NUtils.addTask(new WaitFreeHand());
                }
                return;
            }

            if (cycleNum > 500) {
                System.out.println("[StackSort] Safety limit (500 cycles). Aborting.");
                gui.msg("Stack sort: too many cycles, aborting");
                break;
            }
        }
    }

    // --- Buffer operations ---

    private BufferLocation findBuffer(NGameUI gui) throws InterruptedException {
        // Prefer a free slot in the inventory being sorted
        Coord freeCoord = inventory.findFreeCoord(new Coord(1, 1));
        if (freeCoord != null) {
            return new BufferLocation(inventory, freeCoord);
        }

        // Fall back to player inventory (when sorting a container)
        if (inventory != gui.maininv) {
            NInventory playerInv = gui.getInventory();
            if (playerInv != null) {
                Coord playerFree = playerInv.findFreeCoord(new Coord(1, 1));
                if (playerFree != null) {
                    return new BufferLocation(playerInv, playerFree);
                }
            }
        }

        return null;
    }

    private void dropToBuffer(BufferLocation buffer) throws InterruptedException {
        if (NUtils.getGameUI().vhand == null) return;
        buffer.inv.wdgmsg("drop", buffer.coord);
        NUtils.addTask(new WaitFreeHand());
    }

    private void retrieveFromBuffer(BufferLocation buffer) throws InterruptedException {
        WItem item = findSlotItemAtPos(buffer.inv, buffer.coord);
        if (item != null) {
            NUtils.takeItemToHand(item);
        }
    }

    // --- Slot operations ---

    /**
     * Takes a specific item (identified by quality) from a slot to hand.
     * Handles stacks (2+), stacks dissolving (2→1), and single items.
     */
    private void takeItemFromSlot(Coord pos, float quality) throws InterruptedException {
        WItem slotItem = findSlotItemAtPos(pos);
        if (slotItem == null) return;

        if (slotItem.item.contents instanceof ItemStack) {
            ItemStack stack = (ItemStack) slotItem.item.contents;
            int originalSize = stack.wmap.size();

            WItem target = null;
            for (GItem gi : stack.order) {
                if (gi instanceof NGItem) {
                    NGItem ng = (NGItem) gi;
                    if (ng.quality != null && Math.abs(ng.quality - quality) < 0.001f) {
                        target = stack.wmap.get(gi);
                        break;
                    }
                }
            }
            if (target == null) return;

            NUtils.takeItemToHand(target);

            if (originalSize <= 2) {
                if (stack.parent != null) {
                    NUtils.addTask(new ISRemovedLoftar(
                            ((GItem.ContentsWindow) stack.parent).cont.wdgid(),
                            stack, originalSize));
                }
            } else {
                NUtils.addTask(new StackSizeChanged(stack, originalSize));
            }
        } else {
            int wdgid = slotItem.item.wdgid();
            NUtils.takeItemToHand(slotItem);
            NUtils.addTask(new ISRemoved(wdgid));
        }
    }

    /**
     * Adds the hand item to a slot. Handles empty slots, single items
     * (creates a stack), and existing stacks (grows the stack).
     */
    private void addItemToSlot(Coord pos) throws InterruptedException {
        if (NUtils.getGameUI().vhand == null) return;

        WItem slotItem = findSlotItemAtPos(pos);

        if (slotItem == null) {
            inventory.wdgmsg("drop", pos);
            NUtils.addTask(new WaitFreeHand());
        } else if (slotItem.item.contents instanceof ItemStack) {
            ItemStack stack = (ItemStack) slotItem.item.contents;
            int oldSize = stack.wmap.size();
            NUtils.itemact(slotItem);
            NUtils.addTask(new WaitFreeHand());
            NUtils.addTask(new StackSizeChanged(stack, oldSize));
        } else {
            NUtils.itemact(slotItem);
            NUtils.addTask(new WaitFreeHand());
            NUtils.getUI().core.addTask(new WaitTicks(2));
        }
    }

    // --- Scan and lookup helpers ---

    private List<Float> getSlotQualities(Coord pos) {
        WItem slotItem = findSlotItemAtPos(pos);
        if (slotItem == null) return new ArrayList<>();

        List<Float> qualities = new ArrayList<>();
        if (slotItem.item.contents instanceof ItemStack) {
            ItemStack stack = (ItemStack) slotItem.item.contents;
            for (GItem gi : stack.order) {
                if (gi instanceof NGItem && ((NGItem) gi).quality != null) {
                    qualities.add(((NGItem) gi).quality);
                }
            }
        } else if (slotItem.item instanceof NGItem) {
            NGItem ng = (NGItem) slotItem.item;
            if (ng.quality != null) {
                qualities.add(ng.quality);
            }
        }
        return qualities;
    }

    private WItem findSlotItemAtPos(Coord gridPos) {
        return findSlotItemAtPos(inventory, gridPos);
    }

    private static WItem findSlotItemAtPos(NInventory inv, Coord gridPos) {
        for (Widget wdg = inv.lchild; wdg != null; wdg = wdg.prev) {
            if (wdg instanceof WItem) {
                WItem w = (WItem) wdg;
                Coord pos = w.c.sub(1, 1).div(Inventory.sqsz);
                if (pos.equals(gridPos)) {
                    return w;
                }
            }
        }
        return null;
    }

    // --- Multiset utilities for quality comparison ---

    /**
     * Returns elements in {@code a} that are not matched in {@code b} (multiset difference).
     */
    private static List<Float> multisetDiff(List<Float> a, List<Float> b) {
        List<Float> bCopy = new ArrayList<>(b);
        List<Float> diff = new ArrayList<>();
        for (float v : a) {
            int idx = findFloatIdx(bCopy, v);
            if (idx >= 0) {
                bCopy.remove(idx);
            } else {
                diff.add(v);
            }
        }
        return diff;
    }

    private static boolean containsFloat(List<Float> list, float val) {
        return findFloatIdx(list, val) >= 0;
    }

    private static int findFloatIdx(List<Float> list, float val) {
        for (int i = 0; i < list.size(); i++) {
            if (Math.abs(list.get(i) - val) < 0.001f) {
                return i;
            }
        }
        return -1;
    }
}
