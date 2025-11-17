package nurgling.actions;

import haven.GItem;
import haven.Widget;
import haven.WItem;
import haven.res.ui.stackinv.ItemStack;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.StackSizeChanged;
import nurgling.tasks.WaitFreeHand;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;

import java.util.ArrayList;

/**
 * Test37: Finds all pickling jars with available space and fills them with beetroots from player inventory.
 * Uses direct jar contents access and proper transfer patterns.
 */
public class JarFillerWithBeetroots implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== JAR FILLER WITH BEETROOTS - Starting Filling ===");

        // Step 1: Check player inventory for beetroots
        NInventory playerInventory = gui.getInventory();
        if (playerInventory == null) {
            System.out.println("ERROR: Could not access player inventory");
            return Results.FAIL();
        }

        ArrayList<WItem> playerBeetroots = findBeetsInInventory(playerInventory);
        if (playerBeetroots.isEmpty()) {
            System.out.println("No beetroots found in player inventory");
            return Results.SUCCESS();
        }

        System.out.println("Found " + playerBeetroots.size() + " beetroot(s) in player inventory");

        // Step 2: Find all containers in the area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in current area");
            return Results.SUCCESS();
        }

        int totalBeetsTransferred = 0;
        int totalJarsProcessed = 0;

        // Step 3: Process each container
        for (int c = 0; c < containers.size(); c++) {
            Container container = containers.get(c);
            System.out.println("=== PROCESSING CONTAINER " + (c + 1) + "/" + containers.size() + " ===");
            System.out.println("Container: " + container.cap);

            // Open container
            if (!openContainer(gui, container)) {
                System.out.println("Failed to open container: " + container.cap);
                continue;
            }

            // Find all pickling jars in this container
            ArrayList<WItem> picklingJars = findAllPicklingJarsInContainer(container.cap, gui);

            if (picklingJars.isEmpty()) {
                System.out.println("No pickling jars found in this container");
                closeContainer(gui, container);
                continue;
            }

            System.out.println("Found " + picklingJars.size() + " pickling jar(s) in this container");

            // Process each jar for available space
            for (int j = 0; j < picklingJars.size(); j++) {
                WItem jar = picklingJars.get(j);
                totalJarsProcessed++;

                System.out.println("\n--- PROCESSING JAR " + (j + 1) + "/" + picklingJars.size() + " ---");

                int beetsAdded = fillJarWithBeetroots(gui, jar, playerBeetroots, totalJarsProcessed);
                totalBeetsTransferred += beetsAdded;

                // Check if we're out of beetroots
                if (playerBeetroots.isEmpty()) {
                    System.out.println("No more beetroots in inventory, stopping");
                    closeContainer(gui, container);
                    return finishFilling(totalBeetsTransferred, totalJarsProcessed);
                }

                Thread.sleep(300); // Brief pause between jars
            }

            // Close container before moving to next
            closeContainer(gui, container);
            Thread.sleep(500);
        }

        return finishFilling(totalBeetsTransferred, totalJarsProcessed);
    }

    /**
     * Find beetroots in player inventory
     */
    private ArrayList<WItem> findBeetsInInventory(NInventory playerInventory) {
        ArrayList<WItem> beetroots = new ArrayList<>();

        Widget widget = playerInventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;
                if (isBeetroot(wItem)) {
                    beetroots.add(wItem);
                }
            }
            widget = widget.next;
        }

        System.out.println("  Scanning player inventory: found " + beetroots.size() + " beetroot(s)");
        return beetroots;
    }

    /**
     * Check if item is a beetroot
     */
    private boolean isBeetroot(WItem item) {
        try {
            if (item == null || item.item == null) {
                return false;
            }

            NGItem ngItem = (NGItem) item.item;
            String itemName = ngItem.name();
            String resourceName = ngItem.res != null ? ngItem.res.get().name : null;

            // Check by name and resource path
            if (itemName != null && itemName.toLowerCase().contains("beetroot")) {
                return true;
            }
            if (resourceName != null && resourceName.contains("gfx/invobjs/beet")) {
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find all containers in the area (reusing logic from other bots)
     */
    private ArrayList<Container> findAllContainers(NGameUI gui) {
        ArrayList<Container> containers = new ArrayList<>();

        for (String containerResource : NContext.contcaps.keySet()) {
            String containerType = NContext.contcaps.get(containerResource);
            ArrayList<haven.Gob> containerGobs = Finder.findGobs(new NAlias(containerResource));

            for (haven.Gob containerGob : containerGobs) {
                Container container = new Container(containerGob, containerType);
                containers.add(container);
            }
        }

        return containers;
    }

    /**
     * Find all pickling jars in container using direct widget access
     */
    private ArrayList<WItem> findAllPicklingJarsInContainer(String containerCap, NGameUI gui) {
        ArrayList<WItem> picklingJars = new ArrayList<>();

        NInventory inventory = gui.getInventory(containerCap);
        if (inventory == null) {
            return picklingJars;
        }

        // Direct widget access (same pattern as other tests)
        Widget widget = inventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;
                if (isPicklingJar(wItem)) {
                    picklingJars.add(wItem);
                }
            }
            widget = widget.next;
        }

        return picklingJars;
    }

    /**
     * Check if item is a pickling jar (same logic as other tests)
     */
    private boolean isPicklingJar(WItem item) {
        try {
            if (item == null || item.item == null) {
                return false;
            }

            NGItem ngItem = (NGItem) item.item;
            String resourceName = ngItem.res != null ? ngItem.res.get().name : null;

            // Check by resource path (handles both with/without brine)
            return resourceName != null && resourceName.contains("gfx/invobjs/picklingjar");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fill a specific jar with beetroots from player inventory
     */
    private int fillJarWithBeetroots(NGameUI gui, WItem jarItem, ArrayList<WItem> availableBeetroots, int jarNumber) throws InterruptedException {
        System.out.println("Filling Jar #" + jarNumber + "...");

        // First check jar space using direct contents access
        GItem jarGItem = jarItem.item;
        if (jarGItem.contents == null) {
            System.out.println("  Cannot access jar contents");
            return 0;
        }

        NInventory jarDirectInventory = (NInventory) jarGItem.contents;
        int freeSlots = jarDirectInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
        System.out.println("  Available slots in jar: " + freeSlots);

        if (freeSlots <= 0) {
            System.out.println("  Jar is full - skipping");
            return 0;
        }

        // NEW APPROACH: Direct manipulation of jar inventory using contents
        System.out.println("  Using direct jar inventory access method...");

        int beetsAdded = 0;
        NInventory playerInventory = gui.getInventory();

        // Transfer beetroots one by one using direct inventory manipulation
        for (int i = 0; i < freeSlots && i < availableBeetroots.size(); i++) {
            try {
                // Get fresh beetroot list
                ArrayList<WItem> currentBeetroots = findBeetsInInventory(playerInventory);
                if (currentBeetroots.isEmpty()) {
                    System.out.println("    No more beetroots in player inventory");
                    break;
                }

                WItem beetroot = currentBeetroots.get(0);
                System.out.println("    Transferring beetroot " + (i + 1) + " directly to jar contents");

                // Check if this beetroot is in a stack
                if (beetroot.parent instanceof ItemStack) {
                    System.out.println("    → Beetroot is in a stack, using stack transfer method");
                    // For stacked items, use the stack widget to transfer
                    ItemStack stack = (ItemStack) beetroot.parent;
                    stack.wdgmsg("invxf", jarDirectInventory.wdgid(), 1);
                } else {
                    System.out.println("    → Beetroot is individual, using direct transfer method");
                    // For individual items, transfer directly
                    beetroot.item.wdgmsg("invxf", jarDirectInventory.wdgid(), 1);
                }

                Thread.sleep(300);

                beetsAdded++;
                System.out.println("    → Transferred beetroot to jar contents inventory (ID: " + jarDirectInventory.wdgid() + ")");

            } catch (Exception e) {
                System.out.println("    → ERROR transferring to jar contents: " + e.getMessage());
                break;
            }
        }

        if (beetsAdded > 0) {
            System.out.println("  Successfully added " + beetsAdded + " beetroot(s) to this jar");
        } else {
            System.out.println("  No beetroots added to this jar");
        }

        return beetsAdded;
    }

    /**
     * Container operation helpers
     */
    private boolean openContainer(NGameUI gui, Container container) throws InterruptedException {
        try {
            new OpenTargetContainer(container).run(gui);
            Thread.sleep(1000);
            return gui.getInventory(container.cap) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void closeContainer(NGameUI gui, Container container) {
        try {
            new CloseTargetContainer(container).run(gui);
            Thread.sleep(500);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Complete filling process and return results
     */
    private Results finishFilling(int totalBeetsTransferred, int totalJarsProcessed) {
        System.out.println("=== FILLING SUMMARY ===");
        System.out.println("Jars processed: " + totalJarsProcessed);
        System.out.println("Beetroots transferred: " + totalBeetsTransferred);
        System.out.println("=== JAR FILLER WITH BEETROOTS - Filling Complete ===");

        return Results.SUCCESS();
    }
}