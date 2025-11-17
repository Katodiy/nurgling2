package nurgling.actions;

import haven.GItem;
import haven.Widget;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * GlobalBrinePhase - Phase 1 of Pickling Bot
 *
 * Ensures all pickling jars have adequate brine levels (≥1.0 liters)
 * Uses global phase processing for maximum efficiency
 */
public class GlobalBrinePhase implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== GLOBAL BRINE PHASE - Starting ===");

        boolean workRemaining = true;
        int totalJarsFilled = 0;
        int cycleNumber = 1;

        while (workRemaining) {
            System.out.println("\n--- BRINE CYCLE " + cycleNumber + " ---");

            // Step 1: Navigate to pickling jar area
            if (!navigateToPicklingJarArea(gui)) {
                System.out.println("ERROR: Could not navigate to pickling jar area");
                return Results.FAIL();
            }

            // Step 2: Collect jars needing brine from all containers
            ArrayList<JarContainerPair> jarsNeedingBrine = collectJarsNeedingBrine(gui);
            if (jarsNeedingBrine.isEmpty()) {
                System.out.println("No more jars need brine - Global Brine Phase complete");
                workRemaining = false;
                break;
            }

            System.out.println("Found " + jarsNeedingBrine.size() + " jars needing brine across all containers");

            // Step 3: Navigate to brine barrel area
            NArea barrelArea = navigateToBrineBarrelArea(gui);
            if (barrelArea == null) {
                System.out.println("ERROR: Could not navigate to brine barrel area");
                // Return jars to containers before failing
                returnJarsToContainers(gui, jarsNeedingBrine);
                return Results.FAIL();
            }

            // Step 4: Fill all jars at barrel
            haven.Gob brineBarrel = findBrineBarrel(gui, barrelArea);
            if (brineBarrel == null) {
                System.out.println("ERROR: No brine barrel found in area");
                returnJarsToContainers(gui, jarsNeedingBrine);
                return Results.FAIL();
            }

            int jarsFilled = fillJarsAtBarrel(gui, brineBarrel, jarsNeedingBrine);
            System.out.println("Filled " + jarsFilled + " jars at brine barrel");
            totalJarsFilled += jarsFilled;

            // Step 5: Navigate back to jar area and return filled jars
            if (!navigateToPicklingJarArea(gui)) {
                System.out.println("ERROR: Could not navigate back to pickling jar area");
                return Results.FAIL();
            }

            returnJarsToContainers(gui, jarsNeedingBrine);

            cycleNumber++;
            Thread.sleep(1000); // Brief pause between cycles
        }

        System.out.println("=== GLOBAL BRINE PHASE SUMMARY ===");
        System.out.println("Total cycles: " + (cycleNumber - 1));
        System.out.println("Total jars filled: " + totalJarsFilled);
        System.out.println("=== GLOBAL BRINE PHASE - Complete ===");

        return Results.SUCCESS();
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

            return true;
        } catch (Exception e) {
            System.out.println("ERROR navigating to pickling jar area: " + e.getMessage());
            return false;
        }
    }

    /**
     * Navigate to the area specialized for "barrel" with subtype "Pickling Brine"
     */
    private NArea navigateToBrineBarrelArea(NGameUI gui) throws InterruptedException {
        System.out.println("Navigating to brine barrel area...");

        try {
            NContext context = new NContext(gui);
            return context.getSpecArea(Specialisation.SpecName.barrel, "Pickling Brine");
        } catch (Exception e) {
            System.out.println("ERROR navigating to brine barrel area: " + e.getMessage());
            return null;
        }
    }

    /**
     * Collect all jars needing brine from all containers in the current area
     * Takes as many as fit in player inventory
     */
    private ArrayList<JarContainerPair> collectJarsNeedingBrine(NGameUI gui) throws InterruptedException {
        ArrayList<JarContainerPair> jarsNeedingBrine = new ArrayList<>();

        // Find all containers in the area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in pickling jar area");
            return jarsNeedingBrine;
        }

        System.out.println("Scanning " + containers.size() + " containers for jars needing brine...");

        NInventory playerInventory = gui.getInventory();
        int maxJars = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));

        // Process containers until inventory is full or no more jars need brine
        for (Container container : containers) {
            if (jarsNeedingBrine.size() >= maxJars) {
                System.out.println("Player inventory full, stopping collection this cycle");
                break;
            }

            System.out.println("Processing container: " + container.cap);

            // Open container
            if (!openContainer(gui, container)) {
                System.out.println("Failed to open container: " + container.cap);
                continue;
            }

            // Find jars needing brine in this container
            ArrayList<WItem> containerJarsNeedingBrine = findJarsNeedingBrineInContainer(gui, container);

            // Take jars to inventory (respecting capacity limit)
            for (WItem jar : containerJarsNeedingBrine) {
                if (jarsNeedingBrine.size() >= maxJars) {
                    break;
                }

                try {
                    // Transfer jar to player inventory
                    jar.item.wdgmsg("transfer", haven.Coord.z);
                    Thread.sleep(300);

                    // Wait for transfer to complete
                    NUtils.addTask(new ISRemoved(jar.item.wdgid()));

                    // Track which container this jar came from
                    jarsNeedingBrine.add(new JarContainerPair(jar, container));

                    double brineLevel = getBrineLevel(jar);
                    System.out.println("  → Took jar to inventory (brine: " + brineLevel + "L) from " + container.cap);

                } catch (Exception e) {
                    System.out.println("  → ERROR taking jar to inventory: " + e.getMessage());
                }
            }

            closeContainer(gui, container);
        }

        return jarsNeedingBrine;
    }

    /**
     * Find jars needing brine in a specific container
     */
    private ArrayList<WItem> findJarsNeedingBrineInContainer(NGameUI gui, Container container) {
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
                    if (brineLevel < 1.0) {
                        jarsNeedingBrine.add(wItem);
                        System.out.println("    → Found jar needing brine: " + brineLevel + "L");
                    }
                }
            }
            widget = widget.next;
        }

        return jarsNeedingBrine;
    }

    /**
     * Find brine barrel in current area
     */
    private haven.Gob findBrineBarrel(NGameUI gui, NArea barrelArea) throws InterruptedException {
        System.out.println("Looking for brine barrel in area...");

        // Look for barrel gobs
        ArrayList<haven.Gob> barrels = Finder.findGobs(barrelArea, new NAlias("barrel"));

        for (haven.Gob barrel : barrels) {
            System.out.println("  Found barrel at: " + barrel.rc);
            // Since we're in the specialized "Pickling Brine" area, assume it's the right barrel
            return barrel;
        }

        System.out.println("  No barrels found");
        return null;
    }

    /**
     * Fill all jars at the brine barrel using proven logic from Test38
     */
    private int fillJarsAtBarrel(NGameUI gui, haven.Gob barrel, ArrayList<JarContainerPair> jarsToFill) throws InterruptedException {
        System.out.println("Pathfinding to brine barrel...");

        // Pathfind to barrel
        new nurgling.actions.PathFinder(barrel).run(gui);
        Thread.sleep(1000);

        System.out.println("Starting jar filling process...");

        int jarsFilled = 0;
        NInventory playerInventory = gui.getInventory();

        // Process all jars in player inventory
        for (int i = 0; i < jarsToFill.size(); i++) {
            try {
                // Find current jars in player inventory (fresh scan each time)
                ArrayList<WItem> currentJars = findJarsInInventory(playerInventory);
                if (currentJars.isEmpty()) {
                    System.out.println("  No more jars in player inventory");
                    break;
                }

                WItem jar = currentJars.get(0); // Process first available jar
                double brineBeforeInteraction = getBrineLevel(jar);
                System.out.println("  Processing jar " + (i + 1) + "/" + jarsToFill.size() +
                                 " (current brine: " + brineBeforeInteraction + " liters)");

                // Fill single jar using proven method from Test38
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
     * Fill a single jar with brine - proven logic from Test38
     */
    private boolean fillSingleJar(NGameUI gui, WItem jar, haven.Gob barrel) throws InterruptedException {
        double brineBeforeInteraction = getBrineLevel(jar);

        // Take jar to hand
        NUtils.takeItemToHand(jar);
        Thread.sleep(300);

        // Right-click jar on barrel
        NUtils.activateItem(barrel);
        Thread.sleep(1200); // Wait for server response

        // Check jar state after interaction (jar is now in hand)
        WItem jarInHand = gui.vhand;
        double brineAfterFirstTry = 0.0;
        if (jarInHand != null) {
            brineAfterFirstTry = getBrineLevel(jarInHand);
        }

        // If brine increased, success
        if (brineAfterFirstTry > brineBeforeInteraction) {
            // Put jar back in inventory
            if (jarInHand != null) {
                jarInHand.item.wdgmsg("transfer", haven.Coord.z);
                Thread.sleep(300);
            }
            return true;
        }

        // If jar is empty after right-click, try again to distinguish cases
        if (brineAfterFirstTry == 0.0) {
            // Second attempt
            NUtils.activateItem(barrel);
            Thread.sleep(1200);

            jarInHand = gui.vhand;
            double brineAfterSecondTry = 0.0;
            if (jarInHand != null) {
                brineAfterSecondTry = getBrineLevel(jarInHand);
            }

            // Put jar back in inventory in all cases
            if (jarInHand != null) {
                jarInHand.item.wdgmsg("transfer", haven.Coord.z);
                Thread.sleep(300);
            }

            // If still empty, barrel is empty
            return brineAfterSecondTry > 0.0;
        }

        // If brine level stayed the same, might be already full
        boolean isAlreadyFull = brineAfterFirstTry >= 1.0;

        // Put jar back in inventory
        if (jarInHand != null) {
            jarInHand.item.wdgmsg("transfer", haven.Coord.z);
            Thread.sleep(300);
        }

        return isAlreadyFull;
    }

    /**
     * Return all jars to their original containers
     */
    private void returnJarsToContainers(NGameUI gui, ArrayList<JarContainerPair> jarsToReturn) throws InterruptedException {
        System.out.println("Returning jars to their original containers...");

        // Group jars by container for efficiency
        java.util.Map<Container, java.util.List<JarContainerPair>> jarsByContainer =
            jarsToReturn.stream().collect(java.util.stream.Collectors.groupingBy(pair -> pair.container));

        for (java.util.Map.Entry<Container, java.util.List<JarContainerPair>> entry : jarsByContainer.entrySet()) {
            Container container = entry.getKey();
            java.util.List<JarContainerPair> jarsForContainer = entry.getValue();

            System.out.println("Returning " + jarsForContainer.size() + " jars to " + container.cap);

            // Open container
            if (!openContainer(gui, container)) {
                System.out.println("Failed to open container for returning jars: " + container.cap);
                continue;
            }

            // Return jars currently in player inventory
            NInventory playerInventory = gui.getInventory();
            ArrayList<WItem> currentJars = findJarsInInventory(playerInventory);

            int jarsReturned = 0;
            for (WItem jar : currentJars) {
                if (jarsReturned >= jarsForContainer.size()) {
                    break; // Don't return more jars than we took from this container
                }

                try {
                    jar.item.wdgmsg("transfer", haven.Coord.z);
                    Thread.sleep(300);
                    jarsReturned++;
                    System.out.println("  → Returned jar to " + container.cap);
                } catch (Exception e) {
                    System.out.println("  → ERROR returning jar: " + e.getMessage());
                }
            }

            closeContainer(gui, container);
        }
    }

    /**
     * Helper class to track jar-container relationships
     */
    private static class JarContainerPair {
        final WItem jar;
        final Container container;

        JarContainerPair(WItem jar, Container container) {
            this.jar = jar;
            this.container = container;
        }
    }

    // Utility methods from Test38 and other research bots

    private double getBrineLevel(WItem jarItem) {
        try {
            NGItem ngItem = (NGItem) jarItem.item;

            for (NGItem.NContent content : ngItem.content()) {
                String contentName = content.name();
                if (contentName.contains("l of Pickling Brine")) {
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
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

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