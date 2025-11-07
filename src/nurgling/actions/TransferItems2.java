package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.areas.NContext;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.*;

public class TransferItems2 implements Action
{
    final NContext cnt;
    HashSet<String> items;

    static HashSet<String> orderList = new HashSet<>();
    static {
        orderList.add("Moose Antlers");
        orderList.add("Flipper Bones");
        orderList.add("Red Deer Antlers");
        orderList.add("Wolf's Claws");
        orderList.add("Bear Tooth");
        orderList.add("Lynx Claws");
        orderList.add("Boar Tusk");
        orderList.add("Billygoat Horn");
        orderList.add("Bog Turtle Shell");
        orderList.add("Boreworm Beak");
        orderList.add("Cachalot Tooth");
        orderList.add("Roe Deer Antlers");
        orderList.add("Wildgoat Horn");
        orderList.add("Mole's Pawbone");
        orderList.add("Orca Tooth");
        orderList.add("Adder Skeleton");
        orderList.add("Ant Chitin");
        orderList.add("Bee Chitin");
        orderList.add("Mammoth Tusk");
        orderList.add("Cave Louse Chitin");
        orderList.add("Crabshell");
        orderList.add("Trollbone");
        orderList.add("Walrus Tusk");
        orderList.add("Troll Tusks");
        orderList.add("Whale Bone Material");
        orderList.add("Wishbone");
    }

    public TransferItems2(NContext context, HashSet<String> items)
    {
        this.cnt = context;
        this.items = items;
    }

    /**
     * Helper class to store item transfer information
     */
    private static class ItemTransfer {
        String itemName;
        double quality;
        String areaId;

        ItemTransfer(String itemName, double quality, String areaId) {
            this.itemName = itemName;
            this.quality = quality;
            this.areaId = areaId;
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        // Step 1: Sort items into priority/non-priority (preserve existing orderList behavior)
        ArrayList<String> before = new ArrayList<>();
        ArrayList<String> after = new ArrayList<>();

        for (String item : items)
        {
            if(orderList.contains(item))
            {
                before.add(item);
            }
            else
            {
                after.add(item);
            }
        }
        ArrayList<String> resitems = new ArrayList<>();
        resitems.addAll(before);
        resitems.addAll(after);

        // Step 2: Group items by destination area
        Map<String, List<ItemTransfer>> itemsByArea = new LinkedHashMap<>();

        for(String item : resitems) {
            TreeMap<Double,String> areas = cnt.getOutAreas(item);
            if(areas != null) {
                for (Double quality : areas.descendingKeySet()) {
                    if (!NUtils.getGameUI().getInventory().getItems(new NAlias(item), quality).isEmpty()) {
                        String areaId = areas.get(quality);
                        itemsByArea.computeIfAbsent(areaId, k -> new ArrayList<>())
                            .add(new ItemTransfer(item, quality, areaId));
                    }
                }
            }
        }

        // Step 3: Optimize area visit order using RouteGraph
        List<String> optimizedAreaOrder = optimizeAreaVisitOrder(gui, itemsByArea);

        // Step 4: Visit each area in optimized order and transfer all items for that area
        for (String areaId : optimizedAreaOrder) {
            List<ItemTransfer> itemsForArea = itemsByArea.get(areaId);

            for (ItemTransfer itemTransfer : itemsForArea) {
                // Get storages and perform the transfer
                ArrayList<NContext.ObjectStorage> storages = cnt.getOutStorages(itemTransfer.itemName, itemTransfer.quality);
                for (NContext.ObjectStorage output : storages) {
                    if (output instanceof NContext.Pile) {
                        new TransferToPiles(cnt.getRCArea(areaId), new NAlias(itemTransfer.itemName),
                            (int)itemTransfer.quality).run(gui);
                    }
                    if (output instanceof Container) {
                        new TransferToContainer((Container) output, new NAlias(itemTransfer.itemName),
                            (int)itemTransfer.quality).run(gui);
                    }
                    if (output instanceof NContext.Barrel) {
                        new TransferToBarrel(Finder.findGob(((NContext.Barrel) output).barrel),
                            new NAlias(itemTransfer.itemName)).run(gui);
                    }
                }
            }
        }

        return Results.SUCCESS();
    }

    /**
     * Optimizes the order to visit areas using RouteGraph's greedy nearest-neighbor algorithm
     */
    private List<String> optimizeAreaVisitOrder(NGameUI gui, Map<String, List<ItemTransfer>> itemsByArea) {
        if (itemsByArea.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            RouteGraph graph = ((NMapView) gui.map).routeGraphManager.getGraph();
            RoutePoint playerPos = graph.findNearestPointToPlayer(gui);

            if (playerPos == null) {
                return new ArrayList<>(itemsByArea.keySet());
            }

            // Get RoutePoints for each area
            Map<String, RoutePoint> areaRoutePoints = new HashMap<>();
            for (String areaId : itemsByArea.keySet()) {
                RoutePoint rp = cnt.getRoutePoint(areaId);
                if (rp != null) {
                    areaRoutePoints.put(areaId, rp);
                }
            }

            if (areaRoutePoints.isEmpty()) {
                return new ArrayList<>(itemsByArea.keySet());
            }

            // Optimize the visit order
            List<RoutePoint> optimizedRoutePoints = graph.optimizeVisitOrder(playerPos, areaRoutePoints.values());

            // Convert back to area IDs
            List<String> optimizedAreaIds = new ArrayList<>();
            for (RoutePoint rp : optimizedRoutePoints) {
                for (Map.Entry<String, RoutePoint> entry : areaRoutePoints.entrySet()) {
                    if (entry.getValue().id == rp.id) {
                        optimizedAreaIds.add(entry.getKey());
                        break;
                    }
                }
            }

            // Add any areas that weren't in the optimized list (no route points)
            for (String areaId : itemsByArea.keySet()) {
                if (!optimizedAreaIds.contains(areaId)) {
                    optimizedAreaIds.add(areaId);
                }
            }

            return optimizedAreaIds;

        } catch (Exception e) {
            NUtils.getGameUI().error("Route optimization failed, using default order: " + e.getMessage());
            return new ArrayList<>(itemsByArea.keySet());
        }
    }

}
