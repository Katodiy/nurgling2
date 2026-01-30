package nurgling.actions;

import haven.*;
import haven.res.ui.tt.stackn.Stack;
import nurgling.*;
import nurgling.tasks.*;

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
}
