package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.bots.cheese.CheeseWorkflowUtils;
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
 * Returns rack capacity data via Results payload
 */
public class ClearRacksAndRecordCapacity implements Action {
    private final Coord TRAY_SIZE = new Coord(1, 2);
    private final CheeseWorkflowUtils cheeseWorkflowUtils = new CheeseWorkflowUtils();
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
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
        return Results.SUCCESS(rackCapacity);
    }
    
    /**
     * Clear ready cheese from a specific area's racks to its buffer containers
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
            
            for (Gob rack : racks) {
                // Check each rack for ready cheese to move
                checkRackForReadyCheese(gui, rack, place);
            }
            
        } catch (Exception e) {
            gui.msg("Error clearing " + place + " area: " + e.getMessage());
        }
    }
    
    /**
     * Check a single rack for ready cheese and move to buffer containers
     */
    private void checkRackForReadyCheese(NGameUI gui, Gob rack, CheeseBranch.Place place) {
        try {
            Container rackContainer = new Container(rack, "Rack");
            new PathFinder(rack).run(gui);
            new OpenTargetContainer(rackContainer).run(gui);
            
            // Get all cheese trays in this rack
            ArrayList<haven.WItem> trays = gui.getInventory(rackContainer.cap).getItems(new NAlias("Cheese Tray"));
            ArrayList<haven.WItem> readyTrays = new ArrayList<>();
            
            // Identify which trays are ready to move
            for (haven.WItem tray : trays) {
                if(cheeseWorkflowUtils.isCheeseReadyToMove(tray, place)) {
                    readyTrays.add(tray);
                }
            }
            
            new CloseTargetContainer(rackContainer).run(gui);
            
            // Take ready trays to inventory using the new action
            if (!readyTrays.isEmpty()) {
                gui.msg("Taking " + readyTrays.size() + " ready cheese trays to inventory from " + place + " rack");
                new TakeWItemsFromContainer(rackContainer, readyTrays).run(gui);
                
                // Now move the trays from inventory to buffer containers
                for (int i = 0; i < readyTrays.size(); i++) {
                    moveFromInventoryToBuffer(gui, place);
                }
            }
            
        } catch (Exception e) {
            gui.msg("Error checking rack in " + place + ": " + e.getMessage());
        }
    }
    
    /**
     * Move a cheese tray from inventory to buffer container in the same area
     */
    private void moveFromInventoryToBuffer(NGameUI gui, CheeseBranch.Place place) {
        try {
            // Find buffer containers in this area (containers within the same area as racks)
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area == null) return;
            
            // Find containers that can serve as buffers (not racks)
            ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob containerGob : containers) {
                Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                bufferContainer.initattr(Container.Space.class);
                new PathFinder(containerGob).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Check if container has space
                if (bufferContainer.getattr(Container.Space.class) != null && 
                    bufferContainer.getattr(Container.Space.class).getFreeSpace() > 0) {
                    
                    // Transfer the tray from inventory to buffer container
                    new TransferToContainer(bufferContainer, new NAlias("Cheese Tray"), 1).run(gui);
                    new CloseTargetContainer(bufferContainer).run(gui);
                    
                    gui.msg("Moved cheese tray from inventory to buffer container in " + place + " area");
                    return;
                }
                
                new CloseTargetContainer(bufferContainer).run(gui);
            }
            
            gui.msg("No space in buffer containers for " + place + " area");
            
        } catch (Exception e) {
            gui.msg("Error moving to buffer in " + place + ": " + e.getMessage());
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