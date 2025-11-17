package nurgling.actions;

import haven.GItem;
import haven.Widget;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;

import java.util.ArrayList;

/**
 * Test38: Pickling Brine Filler Bot
 *
 * Workflow:
 * 1. Open container, scan jars for brine levels
 * 2. Take non-full jars to inventory (as many as fit)
 * 3. Pathfind to barrel, fill jars by right-clicking on barrel
 * 4. Return to container, drop off filled jars
 * 5. Repeat until all jars are filled
 */
public class PicklingBrineFiller implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== PICKLING BRINE FILLER - Starting Brine Filling ===");

        // Step 1: Find all containers in the area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in current area");
            return Results.SUCCESS();
        }

        int totalJarsProcessed = 0;
        int totalJarsFilled = 0;

        // Step 2: Process each container
        for (int c = 0; c < containers.size(); c++) {
            Container container = containers.get(c);
            System.out.println("=== PROCESSING CONTAINER " + (c + 1) + "/" + containers.size() + " ===");
            System.out.println("Container: " + container.cap);

            // Process all jars in this container with multiple trips
            int jarsFilled = processContainerJars(gui, container);
            totalJarsFilled += jarsFilled;
        }

        System.out.println("=== BRINE FILLING SUMMARY ===");
        System.out.println("Containers processed: " + containers.size());
        System.out.println("Total jars filled: " + totalJarsFilled);
        System.out.println("=== PICKLING BRINE FILLER - Filling Complete ===");

        return Results.SUCCESS();
    }

    /**
     * Process all jars in a container with multiple trips as needed
     */
    private int processContainerJars(NGameUI gui, Container container) throws InterruptedException {
        int totalJarsFilled = 0;
        int tripNumber = 1;

        while (true) {
            System.out.println("\n--- TRIP " + tripNumber + " ---");

            // Open container
            if (!openContainer(gui, container)) {
                System.out.println("Failed to open container: " + container.cap);
                break;
            }

            // Find all jars that need filling
            ArrayList<WItem> jarsNeedingBrine = findJarsNeedingBrine(gui, container);
            if (jarsNeedingBrine.isEmpty()) {
                System.out.println("No more jars need brine filling in this container");
                closeContainer(gui, container);
                break;
            }

            System.out.println("Found " + jarsNeedingBrine.size() + " jars needing brine");

            // Take as many jars as fit in inventory
            ArrayList<WItem> jarsToFill = takeJarsToInventory(gui, jarsNeedingBrine);
            if (jarsToFill.isEmpty()) {
                System.out.println("Could not take any jars to inventory");
                closeContainer(gui, container);
                break;
            }

            System.out.println("Took " + jarsToFill.size() + " jars to inventory");
            closeContainer(gui, container);

            // Find barrel with pickling brine
            haven.Gob barrel = findPicklingBrineBarrel(gui);
            if (barrel == null) {
                System.out.println("No barrel with pickling brine found nearby");
                // Return jars to container before failing
                returnJarsToContainer(gui, container, jarsToFill);
                break;
            }

            // Fill jars at barrel
            int jarsFilled = fillJarsAtBarrel(gui, barrel, jarsToFill);
            System.out.println("Filled " + jarsFilled + " jars at barrel");

            // Return filled jars to container
            returnJarsToContainer(gui, container, jarsToFill);

            totalJarsFilled += jarsFilled;
            tripNumber++;

            // Small delay between trips
            Thread.sleep(1000);
        }

        return totalJarsFilled;
    }

    /**
     * Find all jars in container that need brine filling (< 1 liter)
     */
    private ArrayList<WItem> findJarsNeedingBrine(NGameUI gui, Container container) {
        ArrayList<WItem> jarsNeedingBrine = new ArrayList<>();

        NInventory inventory = gui.getInventory(container.cap);
        if (inventory == null) {
            return jarsNeedingBrine;
        }

        // Direct widget access to find pickling jars
        Widget widget = inventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;
                if (isPicklingJar(wItem)) {
                    double brineLevel = getBrineLevel(wItem);
                    System.out.println("  Jar brine level: " + brineLevel + " liters");

                    if (brineLevel < 1.0) {
                        jarsNeedingBrine.add(wItem);
                        System.out.println("    → Needs filling");
                    } else {
                        System.out.println("    → Already full");
                    }
                }
            }
            widget = widget.next;
        }

        return jarsNeedingBrine;
    }

    /**
     * Get brine level from jar using same logic as PicklingJarAnalyzer
     */
    private double getBrineLevel(WItem jarItem) {
        try {
            NGItem ngItem = (NGItem) jarItem.item;

            // Iterate through jar contents to find brine
            for (NGItem.NContent content : ngItem.content()) {
                String contentName = content.name();
                if (contentName.contains("l of Pickling Brine")) {
                    // Parse brine level: "X.XX l of Pickling Brine"
                    String[] parts = contentName.split(" ");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].equals("l") && i > 0) {
                            try {
                                return Double.parseDouble(parts[i - 1]);
                            } catch (NumberFormatException e) {
                                break;
                            }
                        }
                    }
                }
            }
            return 0.0; // No brine found
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Check if item is a pickling jar
     */
    private boolean isPicklingJar(WItem item) {
        try {
            if (item == null || item.item == null) {
                return false;
            }

            NGItem ngItem = (NGItem) item.item;
            String resourceName = ngItem.res != null ? ngItem.res.get().name : null;

            return resourceName != null && resourceName.contains("gfx/invobjs/picklingjar");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Take as many jars to inventory as will fit
     */
    private ArrayList<WItem> takeJarsToInventory(NGameUI gui, ArrayList<WItem> jarsNeedingBrine) throws InterruptedException {
        ArrayList<WItem> jarsTaken = new ArrayList<>();
        NInventory playerInventory = gui.getInventory();

        if (playerInventory == null) {
            return jarsTaken;
        }

        for (WItem jar : jarsNeedingBrine) {
            // Check if there's space in player inventory
            int freeSlots = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
            if (freeSlots < 1) {
                System.out.println("  Player inventory full, stopping collection for this trip");
                break;
            }

            try {
                // Transfer jar to player inventory
                jar.item.wdgmsg("transfer", haven.Coord.z);
                Thread.sleep(300);

                // Wait for transfer to complete
                NUtils.addTask(new ISRemoved(jar.item.wdgid()));

                jarsTaken.add(jar);
                System.out.println("  → Took jar to inventory (brine level: " + getBrineLevel(jar) + " liters)");

            } catch (Exception e) {
                System.out.println("  → ERROR taking jar to inventory: " + e.getMessage());
                break;
            }
        }

        return jarsTaken;
    }

    /**
     * Find barrel containing pickling brine nearby
     */
    private haven.Gob findPicklingBrineBarrel(NGameUI gui) {
        System.out.println("Looking for barrel with pickling brine...");

        // Look for barrel gobs
        ArrayList<haven.Gob> barrels = Finder.findGobs(new NAlias("barrel"));

        for (haven.Gob barrel : barrels) {
            System.out.println("  Found barrel at: " + barrel.rc);
            // For now, assume any barrel contains pickling brine
            // TODO: Add logic to check barrel contents if needed
            return barrel;
        }

        System.out.println("  No barrels found");
        return null;
    }

    /**
     * Fill jars at barrel using right-click interaction
     */
    private int fillJarsAtBarrel(NGameUI gui, haven.Gob barrel, ArrayList<WItem> jarsToFill) throws InterruptedException {
        System.out.println("Pathfinding to barrel...");

        // Pathfind to barrel
        new nurgling.actions.PathFinder(barrel).run(gui);
        Thread.sleep(1000);

        System.out.println("Starting jar filling process...");

        int jarsFilled = 0;
        NInventory playerInventory = gui.getInventory();

        // Get fresh jar list from player inventory
        for (int i = 0; i < jarsToFill.size(); i++) {
            try {
                // Find current jars in player inventory
                ArrayList<WItem> currentJars = findJarsInInventory(playerInventory);
                if (currentJars.isEmpty()) {
                    System.out.println("  No more jars in player inventory");
                    break;
                }

                WItem jar = currentJars.get(0);
                double brineBeforeInteraction = getBrineLevel(jar);
                System.out.println("  Processing jar " + (i + 1) + "/" + jarsToFill.size() +
                                 " (current brine: " + brineBeforeInteraction + " liters)");

                // Right-click jar on barrel
                boolean success = fillSingleJar(gui, jar, barrel);
                if (success) {
                    jarsFilled++;
                    System.out.println("    → Successfully filled jar");
                } else {
                    System.out.println("    → Barrel appears to be empty, stopping");
                    break;
                }

                Thread.sleep(500);

            } catch (Exception e) {
                System.out.println("  → ERROR filling jar: " + e.getMessage());
                break;
            }
        }

        return jarsFilled;
    }

    /**
     * Fill a single jar with the complex retry logic
     */
    private boolean fillSingleJar(NGameUI gui, WItem jar, haven.Gob barrel) throws InterruptedException {
        double brineBeforeInteraction = getBrineLevel(jar);
        System.out.println("    → Jar brine BEFORE interaction: " + brineBeforeInteraction + " liters");

        // First right-click attempt
        System.out.println("    → Taking jar to hand...");
        NUtils.takeItemToHand(jar);
        Thread.sleep(300);

        System.out.println("    → Right-clicking jar on barrel (attempt 1) at position: " + barrel.rc);
        NUtils.activateItem(barrel);
        Thread.sleep(1200); // Longer wait for server response

        // Check jar state after first interaction (jar is now in hand)
        WItem jarInHand = gui.vhand;
        double brineAfterFirstTry = 0.0;
        if (jarInHand != null) {
            brineAfterFirstTry = getBrineLevel(jarInHand);
        }
        System.out.println("    → Brine after first try: " + brineAfterFirstTry + " liters");

        // If brine increased, success
        if (brineAfterFirstTry > brineBeforeInteraction) {
            // Put jar back in inventory before returning success
            if (jarInHand != null) {
                jarInHand.item.wdgmsg("transfer", haven.Coord.z);
                Thread.sleep(300);
                System.out.println("    → Placed filled jar back in inventory");
            }
            return true;
        }

        // If jar is empty after right-click, try again to distinguish cases
        if (brineAfterFirstTry == 0.0) {
            System.out.println("    → Jar is empty, trying again to check if barrel is empty");

            // Second right-click attempt
            System.out.println("    → Activating barrel again (attempt 2) at position: " + barrel.rc);
            NUtils.activateItem(barrel);
            Thread.sleep(1200); // Longer wait for server response

            // Check jar state after second interaction (still in hand)
            jarInHand = gui.vhand;
            double brineAfterSecondTry = 0.0;
            if (jarInHand != null) {
                brineAfterSecondTry = getBrineLevel(jarInHand);
            }
            System.out.println("    → Brine after second try: " + brineAfterSecondTry + " liters");

            // If still empty, barrel is empty
            if (brineAfterSecondTry == 0.0) {
                System.out.println("    → Barrel is empty");
                // Put jar back in inventory before returning failure
                if (jarInHand != null) {
                    jarInHand.item.wdgmsg("transfer", haven.Coord.z);
                    Thread.sleep(300);
                    System.out.println("    → Placed empty jar back in inventory");
                }
                return false;
            } else {
                // Jar was filled on second try
                // Put jar back in inventory before returning success
                if (jarInHand != null) {
                    jarInHand.item.wdgmsg("transfer", haven.Coord.z);
                    Thread.sleep(300);
                    System.out.println("    → Placed filled jar back in inventory");
                }
                return true;
            }
        }

        // If brine level stayed the same (not empty, not increased), might be full already
        boolean isAlreadyFull = brineAfterFirstTry >= 1.0;

        // Put jar back in inventory in all cases
        if (jarInHand != null) {
            jarInHand.item.wdgmsg("transfer", haven.Coord.z);
            Thread.sleep(300);
            System.out.println("    → Placed jar back in inventory (already full or no change)");
        }

        return isAlreadyFull;
    }

    /**
     * Find jars in player inventory
     */
    private ArrayList<WItem> findJarsInInventory(NInventory inventory) {
        ArrayList<WItem> jars = new ArrayList<>();

        Widget widget = inventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;
                if (isPicklingJar(wItem)) {
                    jars.add(wItem);
                }
            }
            widget = widget.next;
        }

        return jars;
    }

    /**
     * Return jars to container
     */
    private void returnJarsToContainer(NGameUI gui, Container container, ArrayList<WItem> jarsToReturn) throws InterruptedException {
        System.out.println("Returning jars to container...");

        if (!openContainer(gui, container)) {
            System.out.println("Failed to open container for returning jars");
            return;
        }

        // Get current jars in player inventory
        NInventory playerInventory = gui.getInventory();
        ArrayList<WItem> currentJars = findJarsInInventory(playerInventory);

        for (WItem jar : currentJars) {
            try {
                jar.item.wdgmsg("transfer", haven.Coord.z);
                Thread.sleep(300);
                System.out.println("  → Returned jar to container");
            } catch (Exception e) {
                System.out.println("  → ERROR returning jar: " + e.getMessage());
            }
        }

        closeContainer(gui, container);
        System.out.println("Returned " + currentJars.size() + " jars to container");
    }

    /**
     * Container operation helpers (reused from other bots)
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