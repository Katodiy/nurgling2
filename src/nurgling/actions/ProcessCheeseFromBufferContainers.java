package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseWorkflowUtils;
import nurgling.actions.bots.cheese.CheeseSlicingManager;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

/**
 * Processes cheese from all buffer containers across all areas
 * Handles slicing ready cheese and moving cheese to next aging stage
 */
public class ProcessCheeseFromBufferContainers implements Action {
    private CheeseWorkflowUtils utils;
    private CheeseSlicingManager slicingManager;
    
    public ProcessCheeseFromBufferContainers() {
        this.utils = new CheeseWorkflowUtils();
        this.slicingManager = new CheeseSlicingManager();
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
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
     * Process cheese from buffer containers in a specific area
     * Process one container at a time: open → process trays → close → move to next
     */
    private void processBufferContainers(NGameUI gui, CheeseBranch.Place place) throws InterruptedException {
        try {
            // Create fresh context to avoid caching issues
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area == null) {
                gui.msg("No cheese area found for " + place);
                return;
            }
            
            // Find buffer containers in this area
            ArrayList<Gob> containers = Finder.findGobs(area, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            for (Gob containerGob : containers) {
                gui.msg("Processing buffer container in " + place);
                processOneBufferContainer(gui, containerGob, place);
            }
            
        } catch (Exception e) {
            gui.msg("Error processing buffer containers in " + place + ": " + e.getMessage());
        }
    }
    
    /**
     * Process cheese trays in one specific buffer container
     */
    private void processOneBufferContainer(NGameUI gui, Gob containerGob, CheeseBranch.Place place) throws InterruptedException {
        try {
            Container bufferContainer = new Container(containerGob, NContext.contcaps.get(containerGob.ngob.name));
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(bufferContainer).run(gui);
            
            // Get all cheese trays from this specific container
            ArrayList<WItem> trays = gui.getInventory(bufferContainer.cap).getItems(new NAlias("Cheese Tray"));
            
            // Process each tray in this container
            for (WItem tray : trays) {
                String resourcePath = tray.item.res.get().name;
                
                // Check if this cheese is ready to slice (final product)
                if (utils.isCheeseReadyToSlice(tray)) {
                    gui.msg("Found ready cheese for slicing: " + resourcePath);
                    slicingManager.sliceCheese(gui, tray);
                    
                } else if (utils.shouldMoveToNextStage(tray, place)) {
                    // This cheese needs to move to the next aging area
                    CheeseBranch.Place nextStage = utils.getNextStageLocation(tray, place);
                    if (nextStage != null) {
                        gui.msg("Moving " + resourcePath + " from " + place + " to " + nextStage);
                        // TODO: Implement moving cheese between areas
                        // For now, we'll leave it in the buffer
                    }
                } else {
                    // Cheese is not ready to move yet, leave it in buffer
                    gui.msg("Cheese " + resourcePath + " in " + place + " needs more aging time");
                }
            }
            
            new CloseTargetContainer(bufferContainer).run(gui);
            
        } catch (Exception e) {
            gui.msg("Error processing container in " + place + ": " + e.getMessage());
        }
    }
    
}