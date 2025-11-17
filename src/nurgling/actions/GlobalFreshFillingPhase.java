package nurgling.actions;

import haven.ItemInfo;
import haven.Widget;
import haven.WItem;
import haven.res.ui.stackinv.ItemStack;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * GlobalFreshFillingPhase - Phase 3 of Pickling Bot
 *
 * Fills all available jar space with fresh vegetables from storage
 * Uses global phase processing for maximum efficiency
 * Starts with beetroots as requested
 */
public class GlobalFreshFillingPhase implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== GLOBAL FRESH FILLING PHASE - Starting ===");

        boolean workRemaining = true;
        int totalItemsFilled = 0;
        int cycleNumber = 1;

        while (workRemaining) {
            System.out.println("\n--- FILLING CYCLE " + cycleNumber + " ---");

            // Step 0: Pre-check - Navigate to jar area and check if any jars need vegetables
            if (!navigateToPicklingJarArea(gui)) {
                System.out.println("ERROR: Could not navigate to pickling jar area for pre-check");
                return Results.FAIL();
            }

            int totalAvailableSpace = countTotalAvailableJarSpace(gui);
            if (totalAvailableSpace == 0) {
                System.out.println("No jars have available space for vegetables - Global Fresh Filling Phase complete");
                workRemaining = false;
                break;
            }

            System.out.println("Found " + totalAvailableSpace + " available jar slots for vegetables");

            // Step 1: Navigate to fresh vegetable storage area and collect beetroots
            int vegetablesCollected = collectFreshVegetablesToInventory(gui);
            if (vegetablesCollected == 0) {
                System.out.println("No fresh vegetables available - Global Fresh Filling Phase complete");
                workRemaining = false;
                break;
            }

            System.out.println("Collected " + vegetablesCollected + " beetroots from storage");

            // Step 2: Fill jars in all containers with vegetables from inventory
            // (We're already in jar area from the TakeItems2 navigation back)
            int itemsFilled = fillJarsWithVegetables(gui);
            System.out.println("Filled " + itemsFilled + " items into jars");
            totalItemsFilled += itemsFilled;

            // Step 3: Check if more work remains
            if (itemsFilled == 0) {
                // No items were filled, either no space or no vegetables left
                System.out.println("No items filled this cycle - checking if work remains");

                // Check if there are still jars with available space
                boolean hasEmptyJarSpace = checkForEmptyJarSpace(gui);
                boolean hasVegetablesInInventory = countVegetablesInInventory(gui) > 0;

                if (hasEmptyJarSpace && !hasVegetablesInInventory) {
                    System.out.println("Jars have space but no vegetables in inventory - trying to collect more");
                    // Continue to next cycle to collect more vegetables
                } else {
                    System.out.println("All jars filled or no more vegetables available");
                    workRemaining = false;
                }
            }

            cycleNumber++;
            Thread.sleep(1000); // Brief pause between cycles
        }

        System.out.println("=== GLOBAL FRESH FILLING PHASE SUMMARY ===");
        System.out.println("Total cycles: " + (cycleNumber - 1));
        System.out.println("Total items filled: " + totalItemsFilled);
        System.out.println("=== GLOBAL FRESH FILLING PHASE - Complete ===");

        return Results.SUCCESS();
    }

    /**
     * Collect beetroots using proper NContext/TakeItems2 pattern (like cheese bot)
     * Returns number of vegetables collected
     */
    private int collectFreshVegetablesToInventory(NGameUI gui) throws InterruptedException {
        System.out.println("Collecting beetroots using NContext/TakeItems2 pattern...");

        // Create context and register beetroot requirement (like cheese bot)
        NContext context = new NContext(gui);
        context.addInItem("Beetroot", null);

        // Calculate how many beetroots we can take based on available inventory space
        NInventory playerInventory = gui.getInventory();
        int maxItems = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));

        if (maxItems <= 0) {
            System.out.println("No space in player inventory for beetroots");
            return 0;
        }

        System.out.println("Player inventory has " + maxItems + " free slots, collecting beetroots...");

        // Use TakeItems2 to automatically find and collect beetroots (like cheese bot)
        Results result = new TakeItems2(context, "Beetroot", maxItems).run(gui);

        if (!result.isSuccess) {
            System.out.println("Failed to collect beetroots using TakeItems2");
            return 0;
        }

        // Navigate back to pickling jar area after TakeItems2 (same as GlobalBrinePhase pattern)
        if (!navigateToPicklingJarArea(gui)) {
            System.out.println("ERROR: Could not navigate back to pickling jar area after collecting beetroots");
            return 0;
        }

        // Count actual beetroots collected
        ArrayList<WItem> collectedBeetroots = playerInventory.getItems(new NAlias("Beetroot"));
        int totalCollected = collectedBeetroots.size();

        System.out.println("Successfully collected " + totalCollected + " beetroots using TakeItems2");
        return totalCollected;
    }


    /**
     * Navigate to the area specialized for "picklingJars"
     */
    private boolean navigateToPicklingJarArea(NGameUI gui) throws InterruptedException {
        System.out.println("Navigating to pickling jar area...");
        NContext context = new NContext(gui);
        try {
            nurgling.areas.NArea jarArea = context.getSpecArea(Specialisation.SpecName.picklingJars);
            if (jarArea == null) {
                System.out.println("ERROR: No area specialized for 'picklingJars' found");
                return false;
            }

            return true; // Using simplified navigation like GlobalBrinePhase
        } catch (Exception e) {
            System.out.println("ERROR navigating to pickling jar area: " + e.getMessage());
            return false;
        }
    }


    /**
     * Fill jars in all containers with vegetables from player inventory
     */
    private int fillJarsWithVegetables(NGameUI gui) throws InterruptedException {
        System.out.println("Filling jars with vegetables from inventory...");

        // Find all containers in current area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in pickling jar area");
            return 0;
        }

        int totalItemsFilled = 0;

        for (Container container : containers) {
            System.out.println("Processing container: " + container.cap);

            // Open container
            if (!openContainer(gui, container)) {
                System.out.println("Failed to open container: " + container.cap);
                continue;
            }

            // Fill jars in this container
            int itemsFilled = fillJarsInContainer(gui, container);
            totalItemsFilled += itemsFilled;

            closeContainer(gui, container);

            if (itemsFilled > 0) {
                System.out.println("  → Filled " + itemsFilled + " items in " + container.cap);
            }
        }

        return totalItemsFilled;
    }

    /**
     * Fill jars in a specific container with vegetables from player inventory
     */
    private int fillJarsInContainer(NGameUI gui, Container container) throws InterruptedException {
        int itemsFilled = 0;

        NInventory inventory = gui.getInventory(container.cap);
        if (inventory == null) {
            return itemsFilled;
        }

        // Find all pickling jars
        NAlias picklingJarAlias = new NAlias("Pickling Jar");
        ArrayList<WItem> jars = inventory.getItems(picklingJarAlias);

        System.out.println("    Found " + jars.size() + " pickling jars in container");

        for (WItem jar : jars) {
            // Check jar capacity and fill with vegetables
            int itemsFilledInJar = fillSingleJarWithVegetables(gui, jar);
            itemsFilled += itemsFilledInJar;

            // Check if we're out of vegetables in inventory
            if (countVegetablesInInventory(gui) == 0) {
                System.out.println("    → Out of vegetables in inventory, stopping");
                break;
            }
        }

        return itemsFilled;
    }

    /**
     * Fill a single jar with vegetables from player inventory
     * Returns number of items filled
     * Uses proven approach from Test37
     */
    private int fillSingleJarWithVegetables(NGameUI gui, WItem jar) throws InterruptedException {
        int itemsFilled = 0;

        try {
            // Get jar's internal inventory using direct contents access (Test37 method)
            haven.GItem jarGItem = jar.item;
            if (jarGItem.contents == null) {
                System.out.println("      → Cannot access jar contents");
                return itemsFilled;
            }

            NInventory jarDirectInventory = (NInventory) jarGItem.contents;
            int availableSpace = jarDirectInventory.getNumberFreeCoord(haven.Coord.of(1, 1));

            if (availableSpace <= 0) {
                System.out.println("      → Jar is full");
                return itemsFilled;
            }

            System.out.println("      → Jar has " + availableSpace + " available slots");

            // Get fresh beetroots from player inventory each time (Test37 approach)
            NInventory playerInventory = gui.getInventory();

            // Transfer beetroots one by one using Test37's proven method
            for (int i = 0; i < availableSpace; i++) {
                // Get fresh beetroot list each iteration (Test37 approach)
                ArrayList<WItem> currentBeetroots = findBeetsInPlayerInventory(playerInventory);
                if (currentBeetroots.isEmpty()) {
                    System.out.println("        → No more beetroots in player inventory");
                    break;
                }

                WItem beetroot = currentBeetroots.get(0);
                System.out.println("        → Transferring beetroot " + (i + 1) + " directly to jar contents");

                try {
                    // Check if beetroot is in a stack and use appropriate transfer method (Test37 logic)
                    if (beetroot.parent instanceof ItemStack) {
                        System.out.println("        → Beetroot is in a stack, using stack transfer method");
                        ItemStack stack = (ItemStack) beetroot.parent;
                        stack.wdgmsg("invxf", jarDirectInventory.wdgid(), 1);
                    } else {
                        System.out.println("        → Beetroot is individual, using direct transfer method");
                        beetroot.item.wdgmsg("invxf", jarDirectInventory.wdgid(), 1);
                    }

                    Thread.sleep(300);
                    itemsFilled++;
                    System.out.println("        → Transferred beetroot to jar contents inventory (ID: " + jarDirectInventory.wdgid() + ")");

                } catch (Exception e) {
                    System.out.println("        → ERROR transferring to jar contents: " + e.getMessage());
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("      → ERROR processing jar: " + e.getMessage());
        }

        return itemsFilled;
    }

    /**
     * Get available space in jar's internal inventory
     */
    private int getJarAvailableSpace(NInventory jarInventory) {
        try {
            // Count current items in jar
            int currentItems = 0;
            Widget widget = jarInventory.child;
            while (widget != null) {
                if (widget instanceof WItem) {
                    currentItems++;
                }
                widget = widget.next;
            }

            // Jar capacity is typically 4-6 items, let's assume 4 for safety
            int maxCapacity = 4;
            return Math.max(0, maxCapacity - currentItems);

        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Find beetroots in player inventory using direct widget access (Test37 method)
     */
    private ArrayList<WItem> findBeetsInPlayerInventory(NInventory playerInventory) {
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

        return beetroots;
    }

    /**
     * Check if item is a beetroot (Test37 method)
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
     * Count vegetables in player inventory
     */
    private int countVegetablesInInventory(NGameUI gui) {
        try {
            NInventory playerInventory = gui.getInventory();
            ArrayList<WItem> beetroots = findBeetsInPlayerInventory(playerInventory);
            return beetroots.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Count total available space across all jars in all containers
     * Returns the number of vegetable slots available
     */
    private int countTotalAvailableJarSpace(NGameUI gui) throws InterruptedException {
        System.out.println("Counting total available jar space across all containers...");

        int totalAvailableSpace = 0;
        ArrayList<Container> containers = findAllContainers(gui);

        for (Container container : containers) {
            if (!openContainer(gui, container)) {
                System.out.println("  Failed to open container: " + container.cap);
                continue;
            }

            NInventory inventory = gui.getInventory(container.cap);
            if (inventory != null) {
                NAlias picklingJarAlias = new NAlias("Pickling Jar");
                ArrayList<WItem> jars = inventory.getItems(picklingJarAlias);

                int containerAvailableSpace = 0;
                for (WItem jar : jars) {
                    try {
                        NGItem jarNGItem = (NGItem) jar.item;
                        NInventory jarInventory = (NInventory) jarNGItem.contents;

                        if (jarInventory != null) {
                            int jarSpace = getJarAvailableSpace(jarInventory);
                            containerAvailableSpace += jarSpace;
                        }
                    } catch (Exception e) {
                        // Ignore errors, continue checking
                    }
                }

                if (containerAvailableSpace > 0) {
                    System.out.println("  " + container.cap + " has " + containerAvailableSpace + " available jar slots");
                }
                totalAvailableSpace += containerAvailableSpace;
            }

            closeContainer(gui, container);
        }

        System.out.println("Total available jar space: " + totalAvailableSpace + " slots");
        return totalAvailableSpace;
    }

    /**
     * Check if there are jars with available space in current area
     */
    private boolean checkForEmptyJarSpace(NGameUI gui) throws InterruptedException {
        ArrayList<Container> containers = findAllContainers(gui);

        for (Container container : containers) {
            if (!openContainer(gui, container)) {
                continue;
            }

            NInventory inventory = gui.getInventory(container.cap);
            if (inventory != null) {
                NAlias picklingJarAlias = new NAlias("Pickling Jar");
                ArrayList<WItem> jars = inventory.getItems(picklingJarAlias);

                for (WItem jar : jars) {
                    try {
                        NGItem jarNGItem = (NGItem) jar.item;
                        NInventory jarInventory = (NInventory) jarNGItem.contents;

                        if (jarInventory != null && getJarAvailableSpace(jarInventory) > 0) {
                            closeContainer(gui, container);
                            return true; // Found jar with space
                        }
                    } catch (Exception e) {
                        // Ignore errors, continue checking
                    }
                }
            }

            closeContainer(gui, container);
        }

        return false; // No jars with available space found
    }

    // Utility methods (same as GlobalBrinePhase)

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
}