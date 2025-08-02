package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Clears ready cheese from all racks to buffer containers and records rack capacity
 * Use getLastRecordedCapacity() to access the capacity data after running
 */
public class ClearRacksAndRecordCapacity implements Action {
    private final Coord TRAY_SIZE = new Coord(1, 2);
    private Map<CheeseBranch.Place, Integer> lastRecordedCapacity = new HashMap<>();
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        lastRecordedCapacity = new HashMap<>();
        Map<CheeseBranch.Place, Integer> rackCapacity = new HashMap<>();
        
        CheeseBranch.Place[] places = {
                CheeseBranch.Place.outside,
                CheeseBranch.Place.inside,
                CheeseBranch.Place.mine,
                CheeseBranch.Place.cellar
        };
        
        for (CheeseBranch.Place place : places) {
            gui.msg("=== Clearing " + place + " area and recording capacity ===");
            
            // Step 1: Clear ready cheese from racks to buffer containers
            gui.msg("1. Clearing ready cheese from " + place + " racks");
            clearReadyCheeseFromArea(gui, place);
            
            // Step 2: Check rack capacity while we're already in this area
            gui.msg("2. Checking rack capacity in " + place);
            int capacity = calculateRackCapacityInArea(gui, place);
            rackCapacity.put(place, capacity);
            gui.msg(place + " racks can fit " + capacity + " more trays");
        }
        
        gui.msg("=== Full picture obtained - rack capacity across all areas ===");
        lastRecordedCapacity = rackCapacity;
        return Results.SUCCESS();
    }
    
    /**
     * Get the last recorded rack capacity data
     * @return Map of place to available capacity, or empty map if not yet recorded
     */
    public Map<CheeseBranch.Place, Integer> getLastRecordedCapacity() {
        return new HashMap<>(lastRecordedCapacity);
    }
    
    /**
     * Clear ready cheese from a specific area's racks to its buffer containers
     * Efficiently batch collect from all racks, then batch drop to buffers
     */
    private void clearReadyCheeseFromArea(NGameUI gui, CheeseBranch.Place place) {
        try {
            // Create a fresh context to avoid caching issues when navigating between areas
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area == null) {
                gui.msg("No cheese racks area found for " + place);
                return;
            }
            
            // Find all cheese racks in this area
            ArrayList<Gob> racks = Finder.findGobs(area, new NAlias("gfx/terobjs/cheeserack"));
            gui.msg("Found " + racks.size() + " cheese racks in " + place + " area");
            
            // Keep processing racks until all ready cheese is collected
            boolean foundReadyCheese = true;
            while (foundReadyCheese) {
                foundReadyCheese = batchCollectFromRacks(gui, racks, place, area);
                if (foundReadyCheese) {
                    // Drop collected cheese to buffer containers
                    dropToBufferContainers(gui, area, place);
                }
            }
            
            gui.msg("Finished clearing ready cheese from " + place + " area");
            
        } catch (Exception e) {
            gui.msg("Error clearing " + place + " area: " + e.getMessage());
        }
    }
    
    /**
     * Batch collect ready cheese from all racks until inventory is full
     * @return true if any ready cheese was found and collected
     */
    private boolean batchCollectFromRacks(NGameUI gui, ArrayList<Gob> racks, CheeseBranch.Place place, NArea area) throws InterruptedException {
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        boolean foundAnything = false;
        
        for (Gob rack : racks) {
            try {
                // Check if inventory has space for at least one tray
                int availableSpace = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
                if (availableSpace <= 0) {
                    gui.msg("Inventory full, stopping collection from racks");
                    break;
                }
                
                Container rackContainer = new Container(rack, "Rack");
                new PathFinder(rack).run(gui);
                new OpenTargetContainer(rackContainer).run(gui);
                
                // Get all cheese trays in this rack
                ArrayList<haven.WItem> trays = gui.getInventory(rackContainer.cap).getItems(new NAlias("Cheese Tray"));
                ArrayList<haven.WItem> readyTrays = new ArrayList<>();
                
                // Identify which trays are ready to move
                for (haven.WItem tray : trays) {
                    if(CheeseUtils.isCheeseReadyToMove(tray, place)) {
                        readyTrays.add(tray);
                    }
                }
                
//                new CloseTargetContainer(rackContainer).run(gui);
                
                // Take ready trays to inventory (up to available space)
                if (!readyTrays.isEmpty()) {
                    int maxTraysToTake = Math.min(readyTrays.size(), availableSpace);
                    ArrayList<haven.WItem> traysToTake = new ArrayList<>(readyTrays.subList(0, maxTraysToTake));
                    
                    gui.msg("Taking " + traysToTake.size() + " ready cheese trays from " + place + " rack to inventory");
                    new TakeWItemsFromContainer(rackContainer, traysToTake).run(gui);
                    foundAnything = true;
                    
                    // Check remaining space after taking trays
                    availableSpace = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
                    if (availableSpace <= 0) {
                        gui.msg("Inventory full after taking from this rack");
                        break;
                    }
                }
                
            } catch (Exception e) {
                gui.msg("Error collecting from rack in " + place + ": " + e.getMessage());
            }
        }
        
        return foundAnything;
    }
    
    /**
     * Drop all cheese trays from inventory to buffer containers in the area
     */
    private void dropToBufferContainers(NGameUI gui, NArea area, CheeseBranch.Place place) throws InterruptedException {
        try {
            // Count how many cheese trays we have in inventory
            ArrayList<haven.WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (cheeseTrays.isEmpty()) {
                gui.msg("No cheese trays in inventory to drop off");
                return;
            }
            
            gui.msg("Dropping " + cheeseTrays.size() + " cheese trays to buffer containers in " + place + " area");
            
            // Find buffer containers that can serve as buffers (not racks)
            ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob containerGob : containers) {
                // Check if we still have trays to drop
                cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
                if (cheeseTrays.isEmpty()) {
                    gui.msg("All cheese trays dropped off");
                    break;
                }
                
                Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                bufferContainer.initattr(Container.Space.class);
                new PathFinder(containerGob).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Check if container has space for 1x2 cheese trays
                final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
                int containerSpace = gui.getInventory(bufferContainer.cap).getNumberFreeCoord(TRAY_SIZE);
                
                if (containerSpace > 0) {
                    // Transfer all possible trays to this buffer container
                    int traysToTransfer = Math.min(cheeseTrays.size(), containerSpace);
                    
                    for (int i = 0; i < traysToTransfer; i++) {
                        new TransferToContainer(bufferContainer, new NAlias("Cheese Tray"), 1).run(gui);
                    }
                    
                    gui.msg("Transferred " + traysToTransfer + " trays to buffer container in " + place + " area");
                }
                
                new CloseTargetContainer(bufferContainer).run(gui);
            }
            
            // Check if any trays are left
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (!cheeseTrays.isEmpty()) {
                gui.msg("Warning: " + cheeseTrays.size() + " cheese trays couldn't be stored (no buffer space)");
            }
            
        } catch (Exception e) {
            gui.msg("Error dropping to buffer in " + place + ": " + e.getMessage());
        }
    }
    
    /**
     * Calculate rack capacity for a specific area
     */
    private int calculateRackCapacityInArea(NGameUI gui, CheeseBranch.Place place) {
        try {
            // Create fresh context for each area to avoid caching issues
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area != null) {
                return calculateRackSpaceInArea(gui, area);
            }
        } catch (Exception e) {
            gui.msg("Could not access " + place + " cheese racks: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Calculate available space in cheese racks in a specific area
     */
    private int calculateRackSpaceInArea(NGameUI gui, NArea area) throws InterruptedException {
        ArrayList<Gob> racks = Finder.findGobs(area, new NAlias("gfx/terobjs/cheeserack"));
        int totalSpace = 0;
        
        for (Gob rack : racks) {
            Container rackContainer = new Container(rack, "Rack");
            new PathFinder(rack).run(gui);
            new OpenTargetContainer(rackContainer).run(gui);
            
            int freeSpace = gui.getInventory(rackContainer.cap).getNumberFreeCoord(TRAY_SIZE);
            totalSpace += freeSpace;
            
            new CloseTargetContainer(rackContainer).run(gui);
        }
        
        return totalSpace;
    }
}