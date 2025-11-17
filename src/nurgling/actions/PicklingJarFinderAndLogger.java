package nurgling.actions;

import haven.Gob;
import haven.Widget;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NArea;
import nurgling.areas.NContext;

import java.util.ArrayList;

/**
 * Pickling jar collection utility that finds containers with pickling jars
 * and collects them to player inventory. Searches containers, opens them,
 * and transfers as many jars as will fit into player inventory.
 */
public class PicklingJarFinderAndLogger implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== PICKLING JAR COLLECTOR - Starting Collection ===");

        // Step 1: Get current area information
        logCurrentAreaInfo(gui);

        // Step 2: Find all containers in the area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in current area");
            return Results.SUCCESS();
        }

        // Step 3: Collect jars from containers into player inventory
        int totalPicklingJarsCollected = 0;
        for (int i = 0; i < containers.size(); i++) {
            Container container = containers.get(i);
            System.out.println("=== CONTAINER " + (i + 1) + "/" + containers.size() + " ===");

            int jarsCollected = collectJarsFromContainer(gui, container);
            totalPicklingJarsCollected += jarsCollected;

            // Check if player inventory is getting full
            NInventory playerInventory = gui.getInventory();
            if (playerInventory != null) {
                int freeSlots = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
                System.out.println("Player inventory free slots: " + freeSlots);

                if (freeSlots < 3) {
                    System.out.println("Player inventory nearly full, stopping collection");
                    break;
                }
            }

            // Small delay between containers to avoid overwhelming the system
            Thread.sleep(500);
        }

        // Step 4: Summary
        System.out.println("=== COLLECTION SUMMARY ===");
        System.out.println("Containers searched: " + Math.min(containers.size(), totalPicklingJarsCollected > 0 ? containers.size() : 0));
        System.out.println("Total pickling jars collected: " + totalPicklingJarsCollected);

        System.out.println("=== PICKLING JAR COLLECTOR - Collection Complete ===");
        return Results.SUCCESS();
    }

    /**
     * Log information about the current area
     */
    private void logCurrentAreaInfo(NGameUI gui) {
        System.out.println("--- CURRENT AREA INFO ---");

        try {
            NContext context = new NContext(gui);
            // Try to get area information
            System.out.println("Context created successfully");

            // Log player position
            if (gui.map != null && gui.map.player() != null) {
                System.out.println("Player position: " + gui.map.player().rc);
            }

        } catch (Exception e) {
            System.out.println("Could not get detailed area info: " + e.getMessage());
        }
    }

    /**
     * Find all containers in the current area
     */
    private ArrayList<Container> findAllContainers(NGameUI gui) {
        System.out.println("--- FINDING CONTAINERS ---");

        ArrayList<Container> containers = new ArrayList<>();

        // Get all known container types from NContext
        for (String containerResource : NContext.contcaps.keySet()) {
            String containerType = NContext.contcaps.get(containerResource);

            ArrayList<Gob> containerGobs = Finder.findGobs(new NAlias(containerResource));

            if (!containerGobs.isEmpty()) {
                System.out.println("Found " + containerGobs.size() + " " + containerType + "(s)");

                for (Gob containerGob : containerGobs) {
                    Container container = new Container(containerGob, containerType);
                    containers.add(container);

                    // Log container details
                    System.out.println("  Container: " + containerType + " at " + containerGob.rc);
                    System.out.println("  Resource: " + containerResource);
                    System.out.println("  Model attr: " + (containerGob.ngob != null ? containerGob.ngob.getModelAttribute() : "null"));
                }
            }
        }

        System.out.println("Total containers found: " + containers.size());
        return containers;
    }

    /**
     * Collect pickling jars from a specific container
     */
    private int collectJarsFromContainer(NGameUI gui, Container container) throws InterruptedException {
        System.out.println("Collecting jars from container: " + container.cap + " (ID: " + container.gobid + ")");

        try {
            // Open the container
            if (!openContainer(gui, container)) {
                System.out.println("ERROR: Failed to open container: " + container.cap);
                return 0;
            }

            // Get container inventory
            NInventory containerInventory = gui.getInventory(container.cap);
            if (containerInventory == null) {
                System.out.println("ERROR: Could not access inventory for: " + container.cap);
                closeContainer(gui, container);
                return 0;
            }

            // Collect pickling jars to player inventory
            int jarsCollected = collectPicklingJarsToInventory(gui, containerInventory, container);

            // Close the container
            closeContainer(gui, container);

            return jarsCollected;

        } catch (Exception e) {
            System.out.println("ERROR: Error collecting from container " + container.cap + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Open a container
     */
    private boolean openContainer(NGameUI gui, Container container) throws InterruptedException {
        try {
            // Use existing OpenTargetContainer action pattern
            new OpenTargetContainer(container).run(gui);

            // Wait a moment for the container to open
            Thread.sleep(1000);

            return gui.getInventory(container.cap) != null;

        } catch (Exception e) {
            System.out.println("Failed to open container: " + e.getMessage());
            return false;
        }
    }

    /**
     * Close a container
     */
    private void closeContainer(NGameUI gui, Container container) {
        try {
            // Use existing CloseTargetContainer action pattern
            new CloseTargetContainer(container).run(gui);
            Thread.sleep(500); // Brief pause after closing

        } catch (Exception e) {
            System.out.println("Note: Could not close container automatically: " + e.getMessage());
        }
    }

    /**
     * Collect pickling jars from container to player inventory
     */
    private int collectPicklingJarsToInventory(NGameUI gui, NInventory containerInventory, Container container) throws InterruptedException {
        System.out.println("  Collecting jars from: " + container.cap);

        // Use DIRECT WIDGET ACCESS like Test35 to find pickling jars
        ArrayList<WItem> picklingJars = findPicklingJarsDirectAccess(containerInventory);
        System.out.println("  Total widgets scanned, found: " + picklingJars.size() + " pickling jar(s)");

        if (picklingJars.isEmpty()) {
            System.out.println("  No pickling jars found in this container");
            return 0;
        }

        System.out.println("  Found " + picklingJars.size() + " pickling jar(s) to collect");

        int jarsCollected = 0;
        NInventory playerInventory = gui.getInventory();

        for (WItem jar : picklingJars) {
            // Check if we have space in player inventory
            if (playerInventory != null) {
                int freeSlots = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
                if (freeSlots < 1) {
                    System.out.println("  Player inventory full, stopping collection");
                    break;
                }
            }

            try {
                System.out.println("  Collecting jar: " + ((NGItem) jar.item).name());

                // Check if there's space in player inventory (proper way)
                if (playerInventory != null && playerInventory.getNumberFreeCoord(jar) > 0) {
                    // Transfer jar using proper transfer message
                    jar.item.wdgmsg("transfer", haven.Coord.z);

                    // Wait for the item to be removed from container (proper way)
                    NUtils.addTask(new ISRemoved(jar.item.wdgid()));

                    jarsCollected++;
                    System.out.println("  → Successfully transferred jar to inventory");
                } else {
                    System.out.println("  No free space in inventory for jar");
                    break;
                }

            } catch (Exception e) {
                System.out.println("  ERROR: Could not collect jar: " + e.getMessage());
            }
        }

        System.out.println("  Successfully collected " + jarsCollected + " jar(s)");
        return jarsCollected;
    }

    /**
     * Determine if an item is a pickling jar
     */
    private boolean isPicklingJar(WItem item) {
        if (item == null || item.item == null) {
            return false;
        }

        NGItem ngItem = (NGItem) item.item;

        String itemName = ngItem.name() != null ? ngItem.name().toLowerCase() : "";
        String resourceName = ngItem.res != null ? ngItem.res.get().name.toLowerCase() : "";

        // Check multiple criteria for pickling jar identification
        boolean isPicklingJar = false;

        // Check by resource path (from requirements)
        if (resourceName.contains("gfx/invobjs/picklingjar")) {
            isPicklingJar = true;
        }

        // Check by name
        if (itemName.contains("pickling") && itemName.contains("jar")) {
            isPicklingJar = true;
        }

        // Alternative checks
        if (itemName.contains("Pickling Jar") || resourceName.contains("Pickling Jar")) {
            isPicklingJar = true;
        }

        return isPicklingJar;
    }

    /**
     * Find pickling jars using direct widget access (same method as Test35)
     */
    private ArrayList<WItem> findPicklingJarsDirectAccess(NInventory containerInventory) {
        ArrayList<WItem> picklingJars = new ArrayList<>();

        Widget widget = containerInventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;

                // Check if this is a pickling jar using resource path
                try {
                    NGItem ngItem = (NGItem) wItem.item;
                    String resource = ngItem.res != null ? ngItem.res.get().name : "";

                    if (resource.contains("gfx/invobjs/picklingjar")) {
                        picklingJars.add(wItem);
                    }
                } catch (Exception e) {
                    // Skip invalid items
                }
            }
            widget = widget.next;
        }

        return picklingJars;
    }
}