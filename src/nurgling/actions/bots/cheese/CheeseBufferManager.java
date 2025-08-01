package nurgling.actions.bots.cheese;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.PathFinder;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.CloseTargetContainer;
import nurgling.actions.TransferToContainer;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Handles buffer zone operations - clearing racks and processing buffer containers
 */
public class CheeseBufferManager {
    private NContext context;
    private CheeseWorkflowUtils utils;
    private CheeseSlicingManager slicingManager;
    
    public CheeseBufferManager(NContext context, CheeseWorkflowUtils utils, CheeseSlicingManager slicingManager) {
        this.context = context;
        this.utils = utils;
        this.slicingManager = slicingManager;
    }
    
    /**
     * OPTIMIZED: Clear ready cheese from all racks and record capacity in single pass
     * @return map of rack capacity for each area
     */
    public java.util.Map<CheeseBranch.Place, Integer> clearRacksAndRecordCapacity(NGameUI gui) throws InterruptedException {
        java.util.Map<CheeseBranch.Place, Integer> rackCapacity = new java.util.HashMap<>();
        
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
        return rackCapacity;
    }
    
    /**
     * Calculate rack capacity for a specific area (extracted from CheeseRackManager logic)
     */
    private int calculateRackCapacityInArea(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
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
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        
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
    
    /**
     * Clear ready cheese from a specific area's racks to its buffer containers
     */
    private void clearReadyCheeseFromArea(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        try {
            // Create a fresh context to avoid caching issues when navigating between areas
            NContext freshContext = new NContext(gui);
            
            // Get the cheese racks area for this specific place
            NArea rackArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (rackArea == null) {
                gui.msg("No " + place + " cheese racks area configured");
                return;
            }
            
            gui.msg("Found " + place + " area with ID: " + rackArea.id + ", navigating there");
            
            // Get all racks in this area
            ArrayList<Gob> racks = Finder.findGobs(rackArea, new NAlias("gfx/terobjs/cheeserack"));
            
            for (Gob rack : racks) {
                Container rackContainer = new Container(rack, "Rack");
                new PathFinder(rack).run(gui);
                new OpenTargetContainer(rackContainer).run(gui);
                
                // Check each tray in the rack
                ArrayList<WItem> traysInRack = gui.getInventory(rackContainer.cap).getItems(new NAlias("Cheese Tray"));
                
                for (WItem tray : traysInRack) {
                    if (utils.isCheeseReadyToMove(tray, place)) {
                        // Move this tray to buffer containers in same area
                        moveToBufferContainer(gui, tray, place);
                    }
                }
                
                new CloseTargetContainer(rackContainer).run(gui);
            }
            
        } catch (Exception e) {
            gui.msg("Error clearing " + place + " area: " + e.getMessage());
        }
    }
    
    /**
     * Move a tray to buffer containers in the same area
     */
    private void moveToBufferContainer(NGameUI gui, WItem tray, CheeseBranch.Place place) throws InterruptedException {
        try {
            // Create fresh context to avoid caching issues
            NContext freshContext = new NContext(gui);
            
            // Get the cheese racks area (same area, but look for containers instead of racks)
            NArea rackArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (rackArea == null) return;
            
            // Find containers in this area to use as buffer storage
            ArrayList<Gob> containers = Finder.findGobs(rackArea, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob container : containers) {
                Container bufferContainer = new Container(container, NContext.contcaps.get(container.ngob.name));
                new PathFinder(container).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Check if there's space in this container
                if (bufferContainer.getattr(Container.Space.class) != null && 
                    bufferContainer.getattr(Container.Space.class).getFreeSpace() > 0) {
                    
                    // Transfer the tray to buffer container
                    new TransferToContainer(bufferContainer, new NAlias("Cheese Tray"), 1).run(gui);
                    new CloseTargetContainer(bufferContainer).run(gui);
                    
                    gui.msg("Moved " + tray.item.res.get().name + " to buffer container in " + place + " area");
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
     * Phase 2: Process cheese from all buffer containers
     */
    public void processCheeseFromBufferContainers(NGameUI gui) throws InterruptedException {
        CheeseBranch.Place[] places = {
                CheeseBranch.Place.outside,
                CheeseBranch.Place.inside,
                CheeseBranch.Place.mine,
                CheeseBranch.Place.cellar
        };
        
        for (CheeseBranch.Place place : places) {
            gui.msg("Processing cheese from " + place + " buffer containers");
            processBufferContainers(gui, place);
        }
    }
    
    /**
     * Process cheese from buffer containers in a specific area
     */
    private void processBufferContainers(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        // Get cheese trays from buffer containers in this area
        ArrayList<WItem> bufferTrays = getTraysFromBufferContainers(gui, place);
        
        for (WItem tray : bufferTrays) {
            String resourcePath = tray.item.res.get().name;
            
            // Check if this cheese is ready to slice (final product)
            if (utils.isCheeseReadyToSlice(tray)) {
                gui.msg("Slicing " + resourcePath + " - final product!");
                slicingManager.sliceCheeseAndReturnEmptyTray(gui, tray);
            } 
            // Check if cheese should move to next stage
            else if (utils.shouldMoveToNextStage(tray, place)) {
                CheeseBranch.Place nextPlace = utils.getNextStageLocation(tray, place);
                if (nextPlace != null) {
                    gui.msg("Moving " + resourcePath + " from " + place + " to " + nextPlace);
                    moveCheeseToNextStage(gui, tray, nextPlace);
                }
            }
        }
    }
    
    /**
     * Get cheese trays from buffer containers in the area
     */
    private ArrayList<WItem> getTraysFromBufferContainers(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        ArrayList<WItem> bufferTrays = new ArrayList<>();
        
        try {
            // Create fresh context to avoid caching issues
            NContext freshContext = new NContext(gui);
            
            // Get the cheese racks area
            NArea rackArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (rackArea == null) return bufferTrays;
            
            // Find containers in this area (buffer containers)
            ArrayList<Gob> containers = Finder.findGobs(rackArea, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob container : containers) {
                Container bufferContainer = new Container(container, NContext.contcaps.get(container.ngob.name));
                new PathFinder(container).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Get all cheese trays from this buffer container
                ArrayList<WItem> containerTrays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                bufferTrays.addAll(containerTrays);
                
                new CloseTargetContainer(bufferContainer).run(gui);
            }
            
        } catch (Exception e) {
            gui.msg("Error getting trays from " + place + " buffer containers: " + e.getMessage());
        }
        
        return bufferTrays;
    }
    
    /**
     * Move cheese to next stage area
     */
    private void moveCheeseToNextStage(NGameUI gui, WItem tray, CheeseBranch.Place nextPlace) throws InterruptedException {
        // Move the tray from buffer container to the next area's racks
        // TODO: Implement this movement
        gui.msg("Moving cheese to " + nextPlace + " (implementation needed)");
    }
}