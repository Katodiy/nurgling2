package nurgling.actions;

import haven.GItem;
import haven.Gob;
import haven.WItem;
import haven.Widget;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.tasks.FindNInventory;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;

import java.util.ArrayList;

import static haven.OCache.posres;

/**
 * Debug utility to analyze pickling jar contents and understand how readiness status is represented
 * This bot will find a single pickling jar, open it, and log all available information
 * about its contents, including any potential readiness indicators
 */
public class PicklingJarAnalyzer implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== PICKLING JAR ANALYZER - Starting Analysis ===");

        // Step 1: Find all containers in the area
        ArrayList<Container> containers = findAllContainers(gui);
        if (containers.isEmpty()) {
            System.out.println("No containers found in current area");
            return Results.FAIL();
        }

        int totalJarsAnalyzed = 0;

        // Step 2: Process each container and analyze all pickling jars
        for (int c = 0; c < containers.size(); c++) {
            Container container = containers.get(c);
            System.out.println("=== ANALYZING CONTAINER " + (c + 1) + "/" + containers.size() + " ===");
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

            // Analyze each pickling jar one by one
            for (int j = 0; j < picklingJars.size(); j++) {
                WItem jar = picklingJars.get(j);
                System.out.println("\n--- ANALYZING PICKLING JAR " + (j + 1) + "/" + picklingJars.size() + " ---");

                totalJarsAnalyzed++;
                analyzeIndividualPicklingJar(gui, jar, totalJarsAnalyzed);

                // Small delay between jars
                Thread.sleep(500);
            }

            // Close container before moving to next
            closeContainer(gui, container);
            Thread.sleep(500);
        }

        System.out.println("=== ANALYSIS SUMMARY ===");
        System.out.println("Total jars analyzed: " + totalJarsAnalyzed);
        System.out.println("=== PICKLING JAR ANALYZER - Analysis Complete ===");
        return Results.SUCCESS();
    }


    /**
     * Find all containers in the area (reusing logic from Bot 38)
     */
    private ArrayList<Container> findAllContainers(NGameUI gui) {
        ArrayList<Container> containers = new ArrayList<>();

        for (String containerResource : NContext.contcaps.keySet()) {
            String containerType = NContext.contcaps.get(containerResource);
            ArrayList<Gob> containerGobs = Finder.findGobs(new NAlias(containerResource));

            for (Gob containerGob : containerGobs) {
                Container container = new Container(containerGob, containerType);
                containers.add(container);
            }
        }

        return containers;
    }

    /**
     * Find ALL pickling jars in a specific container (modified from Test 38 logic)
     */
    private ArrayList<WItem> findAllPicklingJarsInContainer(String containerCap, NGameUI gui) {
        ArrayList<WItem> picklingJars = new ArrayList<>();

        NInventory inventory = gui.getInventory(containerCap);
        if (inventory == null) {
            return picklingJars;
        }

        System.out.println("  Searching for all pickling jars in container: " + containerCap);

        // EXACT same widget iteration as Test 38, but collect all jars
        Widget widget = inventory.child;
        int widgetCount = 0;
        while (widget != null) {
            widgetCount++;

            if (widget instanceof WItem) {
                WItem wItem = (WItem) widget;

                // Use EXACT same pickling jar detection as Test 38
                if (isPicklingJarDirect(wItem)) {
                    picklingJars.add(wItem);
                    System.out.println("  Found pickling jar - Widget " + widgetCount);
                    logWidgetDetailsSimple(wItem, widgetCount);
                }
            }

            widget = widget.next;
        }

        System.out.println("  Total widgets checked: " + widgetCount);
        System.out.println("  Total pickling jars found: " + picklingJars.size());
        return picklingJars;
    }

    /**
     * EXACT same pickling jar detection logic as Test 38
     */
    private boolean isPicklingJarDirect(WItem item) {
        try {
            if (item == null || item.item == null) {
                return false;
            }

            NGItem ngItem = (NGItem) item.item;

            String itemName = ngItem.name();
            String resourceName = ngItem.res != null ? ngItem.res.get().name : null;

            // Check multiple criteria for pickling jar identification
            boolean isPicklingJar = false;

            // Check by resource path (from requirements)
            if (resourceName != null && resourceName.contains("gfx/invobjs/picklingjar")) {
                isPicklingJar = true;
            }

            // Check by name (case insensitive)
            if (itemName != null) {
                String lowerName = itemName.toLowerCase();
                if (lowerName.contains("pickling") && lowerName.contains("jar")) {
                    isPicklingJar = true;
                }
                // Also check for exact match
                if (lowerName.equals("pickling jar")) {
                    isPicklingJar = true;
                }
            }

            // Alternative resource checks
            if (resourceName != null && resourceName.toLowerCase().contains("picklingjar")) {
                isPicklingJar = true;
            }

            return isPicklingJar;

        } catch (Exception e) {
            System.out.println("      Error checking if pickling jar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Simple widget logging (same as Test 38 but less verbose)
     */
    private void logWidgetDetailsSimple(WItem wItem, int index) {
        try {
            NGItem ngItem = (NGItem) wItem.item;
            String name = ngItem.name();
            String resource = ngItem.res != null ? ngItem.res.get().name : null;

            System.out.println("    Widget " + index + ": " + (name != null ? name : "NULL") +
                             " | Resource: " + (resource != null ? resource : "NULL") +
                             " | Validation: " + (NGItem.validateItem(wItem) ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("    Widget " + index + ": ERROR - " + e.getMessage());
        }
    }

    /**
     * Helper methods for container operations
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
     * Analyze an individual pickling jar using direct contents access
     */
    private void analyzeIndividualPicklingJar(NGameUI gui, WItem jar, int jarNumber) throws InterruptedException {
        System.out.println("JAR #" + jarNumber + ":");

        // Get brine status
        NGItem ngJar = (NGItem) jar.item;
        String resource = ngJar.res != null ? ngJar.res.get().name : "";
        boolean hasBrine = resource.contains("picklebrine");
        System.out.println("  Brine: " + (hasBrine ? "YES" : "NO"));

        // Get brine level
        if (hasBrine) {
            double brineLevel = getBrineLevel(ngJar);
            System.out.println("  Brine Level: " + brineLevel + "L");
        }

        // Analyze the jar's contents directly
        analyzePicklingJarContents(gui, jar);
    }

    /**
     * Get brine level from jar
     */
    private double getBrineLevel(NGItem jarItem) {
        try {
            for (NGItem.NContent content : jarItem.content()) {
                String contentName = content.name();
                if (contentName.contains("l of Pickling Brine")) {
                    String[] parts = contentName.split(" l of");
                    return Double.parseDouble(parts[0]);
                }
            }
        } catch (Exception e) {
            // Handle parsing errors silently
        }
        return 0.0;
    }




    /**
     * Analyze the contents of a specific pickling jar using DIRECT ITEM CONTENTS ACCESS
     */
    private void analyzePicklingJarContents(NGameUI gui, WItem jarItem) throws InterruptedException {
        // Access the jar's contents directly via its GItem.contents property
        GItem jarGItem = jarItem.item;
        if (jarGItem.contents == null) {
            System.out.println("  Items: 0 (empty jar)");
            return;
        }

        // The contents is the inventory widget of this specific jar
        Widget jarContentsWidget = jarGItem.contents;
        if (!(jarContentsWidget instanceof NInventory)) {
            System.out.println("  Items: ERROR - Invalid contents");
            return;
        }

        NInventory jarInventory = (NInventory) jarContentsWidget;

        // Use DIRECT WIDGET ACCESS to get items from THIS specific jar
        ArrayList<WItem> items = getItemsDirectAccess(jarInventory);
        System.out.println("  Items: " + items.size());

        if (items.isEmpty()) {
            return;
        }

        // Analyze each item in this specific jar
        for (int i = 0; i < items.size(); i++) {
            WItem item = items.get(i);
            System.out.print("    Item " + (i + 1) + ": ");
            analyzePickledItem(gui, item, i);
        }
    }

    /**
     * Get items using direct widget access (same pattern as jar detection)
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
     * Analyze a single pickled item for readiness indicators
     */
    private void analyzePickledItem(NGameUI gui, WItem item, int index) {
        NGItem ngItem = (NGItem) item.item;

        // Get readiness from Drying object
        double readiness = getItemReadiness(ngItem);

        String itemName = ngItem.name() != null ? ngItem.name() : "Unknown";
        System.out.println(itemName + " - Readiness: " + (readiness * 100) + "%");
    }

    /**
     * Extract readiness percentage from item's Drying object
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
     * Analyze inventory-level properties that might indicate liquid levels or other states
     */
    private void analyzeInventoryProperties(NGameUI gui, NInventory inventory) throws InterruptedException {
        System.out.println("--- INVENTORY PROPERTIES ---");
        System.out.println("Inventory size: " + inventory.sz);
        System.out.println("Free space: " + inventory.getNumberFreeCoord(haven.Coord.of(1, 1)));

        // Check for any special inventory attributes
        System.out.println("Inventory class: " + inventory.getClass().getSimpleName());

        // Note: Window caption would require different access method
    }

    /**
     * Log all currently open windows to help identify alternative window names
     */
    private void logAllOpenWindows(NGameUI gui) {
        System.out.println("--- ALL OPEN WINDOWS ---");
        // This is a best-effort attempt to log windows
        // The actual implementation may need adjustment based on the UI structure
        System.out.println("(Window logging would require access to UI window manager)");
    }

    /**
     * Analyze brine level in a jar using the same pattern as bucketIsFull
     */
    private void analyzeBrineLevel(NGItem jarItem) {
        System.out.println("  --- BRINE LEVEL ANALYSIS ---");

        try {
            // Get content information (same as bucketIsFull method)
            Iterable<NGItem.NContent> contents = jarItem.content();

            if (contents == null || !contents.iterator().hasNext()) {
                System.out.println("  No content information available");
                return;
            }

            System.out.println("  Content entries found:");
            int contentCount = 0;
            boolean foundLiquid = false;

            for (NGItem.NContent content : contents) {
                contentCount++;
                String contentName = content.name();
                System.out.println("    Content " + contentCount + ": '" + contentName + "'");

                // Look for liquid level indicators (numbers in content name)
                if (contentName.contains("0")) {
                    System.out.println("    → Found '0' - possibly 0 liters");
                    foundLiquid = true;
                }
                if (contentName.contains("1")) {
                    System.out.println("    → Found '1' - possibly 1 liter");
                    foundLiquid = true;
                }
                if (contentName.contains("2")) {
                    System.out.println("    → Found '2' - possibly 2 liters");
                    foundLiquid = true;
                }
                if (contentName.contains("10")) {
                    System.out.println("    → Found '10' - possibly 10 liters (FULL)");
                    foundLiquid = true;
                }

                // Check for other liquid indicators
                if (contentName.toLowerCase().contains("brine") ||
                    contentName.toLowerCase().contains("liquid") ||
                    contentName.toLowerCase().contains("water")) {
                    System.out.println("    → Contains liquid keyword: " + contentName);
                    foundLiquid = true;
                }
            }

            System.out.println("  Total content entries: " + contentCount);
            System.out.println("  Liquid indicators found: " + foundLiquid);

        } catch (Exception e) {
            System.out.println("  ERROR analyzing brine level: " + e.getMessage());
        }
    }

    /**
     * Close the pickling jar window
     */
    private void closePicklingJarWindow(NGameUI gui) {
        System.out.println("--- CLOSING PICKLING JAR ---");
        try {
            if (gui.getWindow("Pickling Jar") != null) {
                gui.getWindow("Pickling Jar").hide();
                System.out.println("Pickling Jar window closed");
            }
        } catch (Exception e) {
            System.out.println("Note: Could not close window automatically - " + e.getMessage());
        }
    }
}