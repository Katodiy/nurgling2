package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NGItem;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Efficiently moves ready cheese from racks to buffer containers in a specific area
 * Based on FreeContainers pattern - batch collect from racks, then distribute to buffers
 */
public class MoveReadyCheeseToBuffers implements Action {
    
    private ArrayList<Container> racks;
    private ArrayList<Container> buffers;
    private CheeseBranch.Place place;
    
    public MoveReadyCheeseToBuffers(ArrayList<Container> racks, ArrayList<Container> buffers, CheeseBranch.Place place) {
        this.racks = racks;
        this.buffers = buffers;
        this.place = place;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        
        // Phase 1: Collect all ready cheese from racks to inventory
        for (Container rack : racks) {
            navigateToContainer(gui, rack);
            new OpenTargetContainer(rack).run(gui);
            
            // Take ready cheese from this rack until inventory is full or rack is empty of ready cheese
            Results takeResult;
            while (!(takeResult = new TakeReadyCheeseFromRack(rack, place).run(gui)).isSuccess) {
                // Inventory is full, need to distribute to buffers
                new CloseTargetContainer(rack).run(gui);
                distributeToBuffers(gui, context);
                navigateToContainer(gui, rack);
                new OpenTargetContainer(rack).run(gui);
            }
            
            new CloseTargetContainer(rack).run(gui);
        }
        
        // Phase 2: Final distribution of any remaining cheese in inventory
        distributeToBuffers(gui, context);
        
        return Results.SUCCESS();
    }
    
    
    /**
     * Distribute cheese trays from inventory to buffer containers
     */
    private void distributeToBuffers(NGameUI gui, NContext context) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (cheeseTrays.isEmpty()) {
            return; // Nothing to distribute
        }
        
        gui.msg("Distributing " + cheeseTrays.size() + " cheese trays to buffer containers in " + place);
        
        // Try to distribute to buffer containers
        for (Container buffer : buffers) {
            cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (cheeseTrays.isEmpty()) {
                break; // All distributed
            }
            
            navigateToContainer(gui, buffer);
            new OpenTargetContainer(buffer).run(gui);
            
            // Check space and transfer trays if there's room
            final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
            int availableSpace = gui.getInventory(buffer.cap).getNumberFreeCoord(TRAY_SIZE);
            
            if (availableSpace > 0) {
                // Transfer as many trays as possible to this buffer
                buffer.initattr(Container.Space.class);
                transferTraysToBuffer(gui, buffer);
            }
            
            new CloseTargetContainer(buffer).run(gui);
        }
        
        // Check if any trays are left
        cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
        if (!cheeseTrays.isEmpty()) {
            gui.msg("Warning: " + cheeseTrays.size() + " cheese trays couldn't be stored in buffers (no space)");
        }
    }
    
    
    /**
     * Transfer cheese trays from inventory to an open buffer container
     */
    private void transferTraysToBuffer(NGameUI gui, Container buffer) throws InterruptedException {
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        
        while (true) {
            ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            if (cheeseTrays.isEmpty()) {
                break; // No more trays to transfer
            }
            
            int availableSpace = gui.getInventory(buffer.cap).getNumberFreeCoord(TRAY_SIZE);
            if (availableSpace <= 0) {
                break; // Buffer is full
            }
            
            // Transfer one tray
            new TransferToContainer(buffer, new NAlias("Cheese Tray")).run(gui);
        }
    }
    
    /**
     * Navigate to a container (rack or buffer)
     */
    private void navigateToContainer(NGameUI gui, Container container) throws InterruptedException {
        Gob gob = Finder.findGob(container.gobid);
        if (gob != null) {
            new PathFinder(gob).run(gui);
        }
    }
    
    /**
     * Action to take ready cheese from a specific rack
     */
    private static class TakeReadyCheeseFromRack implements Action {
        private Container rack;
        private CheeseBranch.Place place;
        
        public TakeReadyCheeseFromRack(Container rack, CheeseBranch.Place place) {
            this.rack = rack;
            this.place = place;
        }
        
        @Override
        public Results run(NGameUI gui) throws InterruptedException {
            final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
            
            // Check if inventory has space
            int inventorySpace = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
            if (inventorySpace <= 2) {
                return Results.FAIL(); // Inventory full
            }
            
            // Find ALL ready cheese trays in the rack
            ArrayList<WItem> allTrays = gui.getInventory(rack.cap).getItems(new NAlias("Cheese Tray"));
            ArrayList<WItem> readyTrays = new ArrayList<>();
            
            for (WItem tray : allTrays) {
                if (CheeseUtils.isCheeseReadyToMove(tray, place)) {
                    readyTrays.add(tray);
                }
            }
            
            if (readyTrays.isEmpty()) {
                return Results.SUCCESS(); // No ready cheese left in this rack
            }
            
            // Check if ALL ready trays can fit in inventory
            if (readyTrays.size() > inventorySpace) {
                return Results.FAIL(); // Can't fit all ready cheese, need to distribute inventory first
            }
            
            // Take ALL ready trays to inventory
            new TakeWItemsFromContainer(rack, readyTrays).run(gui);
            
            return Results.SUCCESS(); // Rack is now empty of ready cheese
        }
    }
}