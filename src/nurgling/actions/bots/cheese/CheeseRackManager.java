package nurgling.actions.bots.cheese;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.PathFinder;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.CloseTargetContainer;
import nurgling.actions.TransferWItemsToContainer;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

// Import the combined CheeseUtils class

/**
 * Handles cheese rack capacity checking and tray movement between areas
 */
public class CheeseRackManager {
    private final Coord TRAY_SIZE = new Coord(1, 2);
    
    public CheeseRackManager() {
    }
    
    /**
     * Get available inventory capacity for cheese trays
     */
    public int getInventoryCapacity(NGameUI gui) throws InterruptedException {
        // Calculate how many cheese trays can fit in current inventory
        return gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
    }
    
    /**
     * Handle tray placement - try racks first, then buffer containers
     * @return number of trays actually placed
     */
    public int handleTrayPlacement(NGameUI gui, CheeseBranch.Place targetPlace, int batchSize, String cheeseType) throws InterruptedException {
        // Try to place in racks first
        int placed = moveTraysToRacks(gui, targetPlace, batchSize, cheeseType);
        
        if (placed < batchSize) {
            gui.msg("Only placed " + placed + "/" + batchSize + " trays in racks, remaining need buffer container handling");
            // TODO: Implement proper buffer container handling for remaining trays
        }
        
        return placed;
    }
    
    /**
     * Move cheese trays to a specific area's racks
     */
    public int moveTraysToRacks(NGameUI gui, CheeseBranch.Place targetPlace, int quantity, String cheeseType) throws InterruptedException {
        int moved = 0;
        
        try {
            // Create fresh context to avoid caching issues
            NContext freshContext = new NContext(gui);
            NArea targetArea = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, targetPlace.toString());
            if (targetArea == null) {
                gui.msg("Target area not found: " + targetPlace);
                return 0;
            }
            
            ArrayList<Gob> racks = Finder.findGobs(targetArea, new NAlias("gfx/terobjs/cheeserack"));
            
            for (Gob rack : racks) {
                if (moved >= quantity) break;
                Container rackContainer = new Container(rack, "Rack");
                rackContainer.initattr(Container.Space.class);
                new PathFinder(rack).run(gui);
                new OpenTargetContainer(rackContainer).run(gui);
                
                // Check how many trays we can fit in this rack
                int availableSpace = gui.getInventory(rackContainer.cap).getNumberFreeCoord(TRAY_SIZE);
                int toMove = Math.min(availableSpace, quantity - moved);

                new CloseTargetContainer(rackContainer).run(gui);

                if (toMove > 0) {
                    // Get specific trays of the requested cheese type
                    ArrayList<WItem> specificTrays = getTraysOfType(gui, cheeseType, toMove);
                    if (!specificTrays.isEmpty()) {
                        // Transfer only the specific cheese trays to this rack
                        new TransferWItemsToContainer(rackContainer, specificTrays).run(gui);
                        moved += specificTrays.size();
                    }
                }
                
                new CloseTargetContainer(rackContainer).run(gui);
            }
            
            gui.msg("Moved " + moved + " trays to " + targetPlace + " area racks");
            
        } catch (Exception e) {
            gui.msg("Error moving trays to " + targetPlace + ": " + e.getMessage());
        }
        
        return moved;
    }
    
    /**
     * Get specific cheese trays of a particular type from inventory
     */
    private ArrayList<WItem> getTraysOfType(NGameUI gui, String cheeseType, int maxCount) throws InterruptedException {
        ArrayList<WItem> allTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        ArrayList<WItem> specificTrays = new ArrayList<>();
        
        for (WItem tray : allTrays) {
            if (specificTrays.size() >= maxCount) break;
            
            String contentName = CheeseUtils.getContentName(tray);
            if (cheeseType.equals(contentName)) {
                specificTrays.add(tray);
            }
        }
        
        return specificTrays;
    }
}