package nurgling.actions;

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
 * Debug tool to see what items are actually in containers
 */
public class DebugContainerContents implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== DEBUG CONTAINER CONTENTS ===");

        // Find all containers in current area
        ArrayList<Container> containers = findAllContainers(gui);

        for (Container container : containers) {
            System.out.println("\n--- CONTAINER: " + container.cap + " ---");

            // Open container
            if (!openContainer(gui, container)) {
                System.out.println("Failed to open container");
                continue;
            }

            NInventory inventory = gui.getInventory(container.cap);
            if (inventory == null) {
                System.out.println("No inventory found");
                closeContainer(gui, container);
                continue;
            }

            // List ALL items using direct widget access
            System.out.println("ALL ITEMS (direct widget access):");
            Widget widget = inventory.child;
            int itemCount = 0;
            while (widget != null) {
                if (widget instanceof WItem) {
                    WItem wItem = (WItem) widget;
                    try {
                        NGItem ngItem = (NGItem) wItem.item;
                        String itemName = ngItem.name();
                        String resourceName = ngItem.res != null ? ngItem.res.get().name : "null";

                        System.out.println("  Item " + (++itemCount) + ":");
                        System.out.println("    Name: " + itemName);
                        System.out.println("    Resource: " + resourceName);
                        System.out.println("    Is picklingjar resource: " + (resourceName != null && resourceName.contains("gfx/invobjs/picklingjar")));
                    } catch (Exception e) {
                        System.out.println("  Item " + (++itemCount) + ": ERROR - " + e.getMessage());
                    }
                }
                widget = widget.next;
            }

            System.out.println("Total items found: " + itemCount);

            // Test getItems() with different aliases
            System.out.println("\nTEST getItems() with different aliases:");

            String[] aliases = {"picklingjar", "pickling jar", "Pickling Jar", "jar"};
            for (String alias : aliases) {
                try {
                    NAlias testAlias = new NAlias(alias);
                    ArrayList<WItem> items = inventory.getItems(testAlias);
                    System.out.println("  Alias '" + alias + "': found " + items.size() + " items");

                    for (WItem item : items) {
                        NGItem ngItem = (NGItem) item.item;
                        System.out.println("    - " + ngItem.name());
                    }
                } catch (Exception e) {
                    System.out.println("  Alias '" + alias + "': ERROR - " + e.getMessage());
                }
            }

            // Test getItems() without alias (all items)
            try {
                ArrayList<WItem> allItems = inventory.getItems();
                System.out.println("\ngetItems() without alias: found " + allItems.size() + " items");
                for (WItem item : allItems) {
                    NGItem ngItem = (NGItem) item.item;
                    String itemName = ngItem.name();
                    String resourceName = ngItem.res != null ? ngItem.res.get().name : "null";
                    System.out.println("    - " + itemName + " (resource: " + resourceName + ")");
                }
            } catch (Exception e) {
                System.out.println("getItems() without alias: ERROR - " + e.getMessage());
            }

            closeContainer(gui, container);
        }

        System.out.println("=== DEBUG CONTAINER CONTENTS - Complete ===");
        return Results.SUCCESS();
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