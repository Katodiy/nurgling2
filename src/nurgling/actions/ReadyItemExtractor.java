package nurgling.actions;

import haven.GItem;
import haven.Widget;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;

import java.util.ArrayList;

/**
 * Extracts ready (100% pickled) items from pickling jars into player inventory.
 * Uses direct jar contents access and readiness detection via Drying.done field.
 */
public class ReadyItemExtractor implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== READY ITEM EXTRACTOR - Starting Extraction ===");

        // Step 1: Find all containers in the area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in current area");
            return Results.SUCCESS();
        }

        int totalReadyItemsExtracted = 0;
        int totalJarsProcessed = 0;

        // Step 2: Process each container
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

            // Process each jar for ready items
            for (int j = 0; j < picklingJars.size(); j++) {
                WItem jar = picklingJars.get(j);
                totalJarsProcessed++;

                System.out.println("\n--- PROCESSING JAR " + (j + 1) + "/" + picklingJars.size() + " ---");

                int extractedFromJar = extractReadyItemsFromJar(gui, jar, totalJarsProcessed);
                totalReadyItemsExtracted += extractedFromJar;

                // Check player inventory space
                NInventory playerInventory = gui.getInventory();
                if (playerInventory != null) {
                    int freeSlots = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
                    if (freeSlots < 2) {
                        System.out.println("Player inventory nearly full, stopping extraction");
                        closeContainer(gui, container);
                        return finishExtraction(totalReadyItemsExtracted, totalJarsProcessed);
                    }
                }

                Thread.sleep(300); // Brief pause between jars
            }

            // Close container before moving to next
            closeContainer(gui, container);
            Thread.sleep(500);
        }

        return finishExtraction(totalReadyItemsExtracted, totalJarsProcessed);
    }

    /**
     * Find all containers in the area (same logic from other bots)
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

        // Direct widget access (same pattern as Test35)
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
     * Check if item is a pickling jar (same logic as research bots)
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
     * Extract ready items from a specific pickling jar
     */
    private int extractReadyItemsFromJar(NGameUI gui, WItem jarItem, int jarNumber) throws InterruptedException {
        System.out.println("Processing Jar #" + jarNumber + "...");

        // Get jar's direct contents
        GItem jarGItem = jarItem.item;
        if (jarGItem.contents == null) {
            System.out.println("  Empty jar - no items to check");
            return 0;
        }

        Widget jarContentsWidget = jarGItem.contents;
        if (!(jarContentsWidget instanceof NInventory)) {
            System.out.println("  Invalid jar contents");
            return 0;
        }

        NInventory jarInventory = (NInventory) jarContentsWidget;

        // Get all items in this jar using direct widget access
        ArrayList<WItem> itemsInJar = getItemsDirectAccess(jarInventory);

        if (itemsInJar.isEmpty()) {
            System.out.println("  Empty jar - no items");
            return 0;
        }

        System.out.println("  Found " + itemsInJar.size() + " item(s) in jar");

        int extractedCount = 0;

        // Check each item for readiness and extract if ready
        for (int i = 0; i < itemsInJar.size(); i++) {
            WItem item = itemsInJar.get(i);
            NGItem ngItem = (NGItem) item.item;

            double readiness = getItemReadiness(ngItem);
            String itemName = ngItem.name() != null ? ngItem.name() : "Unknown";

            System.out.println("    Item " + (i + 1) + ": " + itemName + " - Readiness: " + (readiness * 100) + "%");

            // Extract if 100% ready
            if (readiness >= 1.0) {
                System.out.println("    → EXTRACTING ready item: " + itemName);

                try {
                    // Extract item using take action
                    item.item.wdgmsg("take", haven.Coord.z);
                    Thread.sleep(200); // Brief delay for UI response
                    extractedCount++;
                } catch (Exception e) {
                    System.out.println("    → ERROR extracting item: " + e.getMessage());
                }
            }
        }

        if (extractedCount > 0) {
            System.out.println("  Extracted " + extractedCount + " ready item(s) from this jar");
        } else {
            System.out.println("  No ready items found in this jar");
        }

        return extractedCount;
    }

    /**
     * Get items using direct widget access (same as research bots)
     */
    private ArrayList<WItem> getItemsDirectAccess(NInventory inventory) {
        ArrayList<WItem> items = new ArrayList<>();

        Widget widget = inventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;
                if (NGItem.validateItem(wItem)) {
                    items.add(wItem);
                }
            }
            widget = widget.next;
        }

        return items;
    }

    /**
     * Extract readiness percentage from Drying.done field (from research findings)
     */
    private double getItemReadiness(NGItem ngItem) {
        try {
            if (ngItem.info != null && ngItem.info.size() > 0) {
                for (Object infoObj : ngItem.info) {
                    if (infoObj.getClass().getName().contains("drying.Drying")) {
                        java.lang.reflect.Field doneField = infoObj.getClass().getField("done");
                        return (Double) doneField.get(infoObj);
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
        return 0.0; // Default to 0% if no readiness found
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
     * Complete extraction process and return results
     */
    private Results finishExtraction(int totalReadyItemsExtracted, int totalJarsProcessed) {
        System.out.println("=== EXTRACTION SUMMARY ===");
        System.out.println("Jars processed: " + totalJarsProcessed);
        System.out.println("Ready items extracted: " + totalReadyItemsExtracted);
        System.out.println("=== READY ITEM EXTRACTOR - Extraction Complete ===");

        return Results.SUCCESS();
    }
}