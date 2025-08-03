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
     */
    private void moveRemainingCheeseToNextStage(NGameUI gui, ArrayList<Gob> containers, CheeseBranch.Place place) throws InterruptedException {
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        
        for (Gob containerGob : containers) {
                Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
                new PathFinder(containerGob).run(gui);
                new OpenTargetContainer(bufferContainer).run(gui);
                
                // Get remaining cheese trays from this container
                ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
                ArrayList<WItem> toMoveTrays = new ArrayList<>();
                
                // Find trays that should move to next stage
                for (WItem tray : trays) {
                    if (CheeseUtils.shouldMoveToNextStage(tray, place)) {
                        CheeseBranch.Place nextStage = CheeseUtils.getNextStageLocation(tray, place);
                        if (nextStage != null) {
                            toMoveTrays.add(tray);
                            gui.msg("Will move " + CheeseUtils.getContentName(tray) + " from " + place + " to " + nextStage);
                        }
                    }
                }
                
                // Take trays to inventory in batches (respecting inventory space)
                for (WItem tray : toMoveTrays) {
                    // Check if inventory has space
                    int availableSpace = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
                    if (availableSpace <= 0) {
                        gui.msg("Inventory full! Moving current batch to next stages...");
                        new CloseTargetContainer(bufferContainer).run(gui);
                        moveInventoryCheeseToNextStages(gui, place);
                        new PathFinder(containerGob).run(gui);
                        new OpenTargetContainer(bufferContainer).run(gui);
                    }
                    
                    // Take tray to inventory for movement
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    nurgling.NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                    
                    // Track this move for capacity calculation
                    CheeseBranch.Place nextStage = CheeseUtils.getNextStageLocation(tray, place);
                    if (nextStage != null) {
                        traysMovedToAreas.put(nextStage, traysMovedToAreas.getOrDefault(nextStage, 0) + 1);
                        gui.msg("Tracked move: " + nextStage + " capacity reduced by 1");
                    }
                }
                
                new CloseTargetContainer(bufferContainer).run(gui);
        }
        
        // Move any remaining cheese in inventory to their next stages
        gui.msg("Moving final batch of cheese to next stages");
        moveInventoryCheeseToNextStages(gui, place);
    }
    
    /**
     * Move cheese currently in inventory to their appropriate next stage areas
     */
    private void moveInventoryCheeseToNextStages(NGameUI gui, CheeseBranch.Place currentPlace) throws InterruptedException {
        // TODO: Implement actual movement to next stage areas
        // For now, just use FreeInventory2 to store them appropriately
        gui.msg("Moving inventory cheese to next stages (using FreeInventory2 for now)");
//        new FreeInventory2().run(gui);
    }
    
}