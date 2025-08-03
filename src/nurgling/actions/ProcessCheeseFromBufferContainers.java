package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.actions.bots.cheese.CheeseSlicingManager;
import nurgling.actions.FreeInventory2;
import nurgling.actions.TakeWItemsFromContainer;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes cheese from all buffer containers across all areas
 * Handles slicing ready cheese and moving cheese to next aging stage
 * Use getTraysMovedToAreas() to get capacity impact after running
 */
public class ProcessCheeseFromBufferContainers implements Action {
    private CheeseSlicingManager slicingManager;
    private Map<CheeseBranch.Place, Integer> traysMovedToAreas = new HashMap<>();
    
    public ProcessCheeseFromBufferContainers() {
        this.slicingManager = new CheeseSlicingManager();
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Initialize tracking map
        traysMovedToAreas.clear();
        
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
        
        return Results.SUCCESS();
    }
    
    /**
     * Get the number of trays moved to each area during buffer processing
     * This impacts rack capacity - these areas now have fewer available slots
     * @return Map of area to number of trays moved to that area
     */
    public Map<CheeseBranch.Place, Integer> getTraysMovedToAreas() {
        return new HashMap<>(traysMovedToAreas);
    }
    
    /**
     * Process cheese from buffer containers in a specific area
     * 1. First pass: collect ready-to-slice cheese to inventory
     * 2. Free inventory when full (FreeInventory2)
     * 3. Second pass: move remaining cheese to next aging stage
     */
    private void processBufferContainers(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
            // Create fresh context to avoid caching issues
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area == null) {
                gui.msg("No cheese area found for " + place);
                return;
            }
            
            // Find buffer containers in this area
            ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            // Phase 1: Collect ready-to-slice cheese
            gui.msg("Phase 1: Collecting ready-to-slice cheese from " + place + " buffers");
            collectReadyToSliceCheese(gui, containers, place);
            
            // Phase 2: Move remaining cheese to next stages
            gui.msg("Phase 2: Moving remaining cheese to next stages from " + place + " buffers");
            moveRemainingCheeseToNextStage(gui, containers, place);
    }
    
    /**
     * Phase 1: Collect ready-to-slice cheese from buffer containers and slice them
     */
    private void collectReadyToSliceCheese(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        final haven.Coord SINGLE_SLOT = new haven.Coord(1, 1);
        NContext freshContext = new NContext(gui);
        
        for (Gob containerGob : containers) {
                Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                new PathFinder(containerGob).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Get all cheese trays from this container
                ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                
                // Process ready trays one by one to manage inventory space properly
                for (WItem tray : trays) {
                    if (CheeseUtils.isCheeseReadyToSlice(tray)) {
                        gui.msg("Found ready-to-slice cheese: " + CheeseUtils.getContentName(tray));
                        
                        // Check if inventory has space for tray + cheese pieces (worst case: 5 pieces + tray = 7 slots)
                        int availableSlots = gui.getInventory().getNumberFreeCoord(SINGLE_SLOT);
                        if (availableSlots < 7) {
                            gui.msg("Not enough inventory space for slicing (need 7 slots, have " + availableSlots + "). Freeing inventory...");
                            new CloseTargetContainer(bufferContainer).run(gui);
                            new FreeInventory2(freshContext).run(gui);
                            new PathFinder(containerGob).run(gui);
                            new OpenTargetContainer(bufferContainer).run(gui);
                        }
                        
                        // Take the tray to inventory
                        tray.item.wdgmsg("transfer", haven.Coord.z);
                        nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                        gui.msg("Took ready cheese to inventory: " + CheeseUtils.getContentName(tray));
                        
                        // Close container to slice the cheese
                        new CloseTargetContainer(bufferContainer).run(gui);
                        
                        // Find the tray we just took and slice it
                        ArrayList<WItem> inventoryTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
                        for (WItem inventoryTray : inventoryTrays) {
                            if (CheeseUtils.isCheeseReadyToSlice(inventoryTray)) {
                                gui.msg("Slicing cheese: " + CheeseUtils.getContentName(inventoryTray));
                                slicingManager.sliceCheese(gui, inventoryTray);
                                break; // Only slice one tray per iteration
                            }
                        }
                        
                        // Check if inventory is getting full after slicing
                        int remainingSpace = gui.getInventory().getNumberFreeCoord(SINGLE_SLOT);
                        if (remainingSpace < 7) {
                            gui.msg("Inventory getting full after slicing. Freeing inventory...");
                            new FreeInventory2(freshContext).run(gui);
                        }
                        
                        // Reopen container to continue
                        new PathFinder(containerGob).run(gui);
                        new OpenTargetContainer(bufferContainer).run(gui);
                    }
                }
                
                new CloseTargetContainer(bufferContainer).run(gui);
        }
        
        // Final inventory cleanup after processing all ready cheese
        gui.msg("Final inventory cleanup after slicing ready cheese");
        new FreeInventory2(freshContext).run(gui);
    }
    
    /**
     * Phase 2: Move remaining cheese to next aging stages
     * Process one cheese type at a time for efficient batching
     */
    private void moveRemainingCheeseToNextStage(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        
        // Process each container individually for cheese type batching
        Map<String, CheeseBranch.Place> cheeseTypeToDestination = new HashMap<>();
        
        // Collect cheese types and their destinations first
        for (Gob containerGob : containers) {
            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(bufferContainer).run(gui);
            
            ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
            for (WItem tray : trays) {
                if (CheeseUtils.shouldMoveToNextStage(tray, place)) {
                    String cheeseType = CheeseUtils.getContentName(tray);
                    CheeseBranch.Place nextStage = CheeseUtils.getNextStageLocation(tray, place);
                    
                    if (cheeseType != null && nextStage != null) {
                        cheeseTypeToDestination.put(cheeseType, nextStage);
                    }
                }
            }
            
            new CloseTargetContainer(bufferContainer).run(gui);
        }
        
        // Process each cheese type separately
        for (Map.Entry<String, CheeseBranch.Place> entry : cheeseTypeToDestination.entrySet()) {
            String cheeseType = entry.getKey();
            CheeseBranch.Place destination = entry.getValue();
            
            gui.msg("Processing " + cheeseType + " trays for move to " + destination);
            
            // Collect all cheese of this type from all containers
            while (true) {
                // Check if inventory has space
                int availableSpace = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
                if (availableSpace <= 0) {
                    gui.msg("Inventory full! Moving current batch of " + cheeseType + " to " + destination);
                    moveInventoryCheeseToDestination(gui, destination);
                    
                    // Navigate back to original area to continue processing
                    NContext freshContext = new NContext(gui);
                    freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
                }
                
                // Find matching cheese in any container
                ArrayList<WItem> matchingTrays = new ArrayList<>();
                Container currentContainer = null;
                
                for (Gob containerGob : containers) {
                    Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                    new PathFinder(containerGob).run(gui);
                    new OpenTargetContainer(bufferContainer).run(gui);
                    
                    ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                    for (WItem tray : trays) {
                        if (CheeseUtils.shouldMoveToNextStage(tray, place)) {
                            String currentCheeseType = CheeseUtils.getContentName(tray);
                            if (cheeseType.equals(currentCheeseType)) {
                                matchingTrays.add(tray);
                            }
                        }
                    }
                    
                    if (!matchingTrays.isEmpty()) {
                        currentContainer = bufferContainer;
                        break; // Found cheese in this container
                    }
                    
                    new CloseTargetContainer(bufferContainer).run(gui);
                }
                
                if (matchingTrays.isEmpty()) {
                    break; // No more cheese of this type found
                }
                
                // Take cheese using TakeWItemsFromContainer
                int traysToTake = Math.min(matchingTrays.size(), availableSpace);
                ArrayList<WItem> traysToTakeList = new ArrayList<>(matchingTrays.subList(0, traysToTake));
                
                new TakeWItemsFromContainer(currentContainer, traysToTakeList).run(gui);
                
                // Track these moves for capacity calculation
                traysMovedToAreas.put(destination, traysMovedToAreas.getOrDefault(destination, 0) + traysToTake);
                
                new CloseTargetContainer(currentContainer).run(gui);
            }
            
            // Move all collected cheese of this type to destination
            if (!gui.getInventory().getItems(new NAlias("Cheese Tray")).isEmpty()) {
                gui.msg("Moving collected " + cheeseType + " to " + destination);
                moveInventoryCheeseToDestination(gui, destination);
                
                // Navigate back to original area for next cheese type
                NContext freshContext = new NContext(gui);
                freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            }
        }
    }
    
    /**
     * Move cheese currently in inventory to a specific destination area
     */
    private void moveInventoryCheeseToDestination(NGameUI gui, CheeseBranch.Place destination) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (cheeseTrays.isEmpty()) {
            return;
        }
        
        gui.msg("Moving " + cheeseTrays.size() + " cheese trays to " + destination + " area");
        
        // Navigate to destination area
        NContext freshContext = new NContext(gui);
        NArea destinationArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, destination.toString());
        if (destinationArea == null) {
            gui.msg("No cheese racks area found for " + destination + ". Using FreeInventory2 as fallback.");
            new FreeInventory2(freshContext).run(gui);
            return;
        }
        
        // Find available racks in destination area
        ArrayList<Gob> racks = Finder.findGobs(destinationArea, new NAlias("gfx/terobjs/cheeserack"));
        if (racks.isEmpty()) {
            gui.msg("No cheese racks found in " + destination + " area. Using FreeInventory2 as fallback.");
            new FreeInventory2(freshContext).run(gui);
            return;
        }
        
        // Place cheese trays on racks
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        for (Gob rackGob : racks) {
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (cheeseTrays.isEmpty()) {
                break; // All cheese placed
            }
            
            Container rack = new Container(rackGob, "Rack");
            new PathFinder(rackGob).run(gui);
            new OpenTargetContainer(rack).run(gui);
            
            // Check available space in this rack
            int availableSpace = gui.getInventory(rack.cap).getNumberFreeCoord(TRAY_SIZE);
            if (availableSpace > 0) {
                // Transfer trays to this rack
                for (int i = 0; i < Math.min(availableSpace, cheeseTrays.size()); i++) {
                    WItem tray = cheeseTrays.get(i);
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                }
                gui.msg("Placed " + Math.min(availableSpace, cheeseTrays.size()) + " trays on rack in " + destination);
            }
            
            new CloseTargetContainer(rack).run(gui);
        }
        
        // If any cheese remains, use FreeInventory2 as fallback
        cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (!cheeseTrays.isEmpty()) {
            gui.msg("Warning: " + cheeseTrays.size() + " cheese trays couldn't fit in " + destination + " racks. Using FreeInventory2.");
            new FreeInventory2(freshContext).run(gui);
        }
    }
    
    /**
     * Helper class to track cheese location information
     */
    private static class CheeseLocation {
        public final WItem tray;
        public final Gob containerGob;
        public final Container container;
        
        public CheeseLocation(WItem tray, Gob containerGob, Container container) {
            this.tray = tray;
            this.containerGob = containerGob;
            this.container = container;
        }
    }
}