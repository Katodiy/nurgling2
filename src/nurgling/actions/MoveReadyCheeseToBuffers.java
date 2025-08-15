package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.cheese.CheeseBranch;
import nurgling.actions.bots.cheese.CheeseRackOverlayUtils;
import nurgling.actions.bots.cheese.CheeseConstants;
import nurgling.actions.bots.cheese.CheeseInventoryOperations;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Efficiently moves ready cheese from racks to buffer containers in a specific area
 * Based on FreeContainers pattern - batch collect from racks, then distribute to buffers
 * Also calculates rack capacities to avoid redundant container operations
 */
public class MoveReadyCheeseToBuffers implements Action {
    
    public static class ResultWithCapacity {
        public final Results result;
        public final Map<Container, Integer> rackCapacities;
        
        public ResultWithCapacity(Results result, Map<Container, Integer> rackCapacities) {
            this.result = result;
            this.rackCapacities = rackCapacities;
        }
    }
    
    private final ArrayList<Container> racks;
    private final ArrayList<Container> buffers;
    private final CheeseBranch.Place place;
    
    public MoveReadyCheeseToBuffers(ArrayList<Container> racks, ArrayList<Container> buffers, CheeseBranch.Place place) {
        this.racks = racks;
        this.buffers = buffers;
        this.place = place;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ResultWithCapacity result = runWithCapacity(gui);
        return result.result;
    }
    
    /**
     * Run the action and return both result and rack capacities
     */
    public ResultWithCapacity runWithCapacity(NGameUI gui) throws InterruptedException {
        Map<Container, Integer> rackCapacities = new HashMap<>();
        // Use centralized cheese tray size constant
        
        // Phase 1: Collect all ready cheese from racks to inventory
        // First, filter racks using overlay status to skip empty ones
        ArrayList<Container> racksToProcess = new ArrayList<>();
        for (Container rack : racks) {
            Gob rackGob = Finder.findGob(rack.gobid);
            if (rackGob != null && CheeseRackOverlayUtils.mightHaveCheeseToMove(rackGob)) {
                racksToProcess.add(rack);
            } else {
                // Record capacity for empty racks without opening them
                if (rackGob != null && CheeseRackOverlayUtils.isRackEmpty(rackGob)) {
                    rackCapacities.put(rack, 3); // Empty rack can hold 3 trays
                }
            }
        }
        
        for (Container rack : racksToProcess) {
            navigateToContainer(gui, rack);
            new OpenTargetContainer(rack).run(gui);
            
            // Take ready cheese from this rack until inventory is full or rack is empty of ready cheese
            while (!new TakeReadyCheeseFromRack(rack, place, rackCapacities).run(gui).isSuccess) {
                // Inventory is full, need to distribute to buffers
                new CloseTargetContainer(rack).run(gui);
                distributeToBuffers(gui);
                navigateToContainer(gui, rack);
                new OpenTargetContainer(rack).run(gui);
            }
            
            new CloseTargetContainer(rack).run(gui);
        }
        
        // Phase 2: Final distribution of any remaining cheese in inventory
        distributeToBuffers(gui);
        
        return new ResultWithCapacity(Results.SUCCESS(), rackCapacities);
    }
    
    
    /**
     * Distribute cheese trays from inventory to buffer containers
     */
    private void distributeToBuffers(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> cheeseTrays = CheeseInventoryOperations.getCheeseTrays(gui);
        if (cheeseTrays.isEmpty()) {
            return; // Nothing to distribute
        }
        
        // Try to distribute to buffer containers
        for (Container buffer : buffers) {
            cheeseTrays = CheeseInventoryOperations.getCheeseTrays(gui);
            if (cheeseTrays.isEmpty()) {
                break; // All distributed
            }
            
            navigateToContainer(gui, buffer);
            new OpenTargetContainer(buffer).run(gui);
            
            // Check space and transfer trays if there's room
            // Use centralized cheese tray size constant
            int availableSpace = gui.getInventory(buffer.cap).getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE);
            
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
        // Use centralized cheese tray size constant
        
        while (true) {
            ArrayList<WItem> cheeseTrays = CheeseInventoryOperations.getCheeseTrays(gui);
            if (cheeseTrays.isEmpty()) {
                break; // No more trays to transfer
            }
            
            int availableSpace = gui.getInventory(buffer.cap).getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE);
            if (availableSpace <= 0) {
                break; // Buffer is full
            }
            
            // Transfer one tray
            new TransferToContainer(buffer, CheeseConstants.CHEESE_TRAY_ALIAS).run(gui);
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
     * Action to take ready cheese from a specific rack and optionally calculate capacity
     */
    private static class TakeReadyCheeseFromRack implements Action {
        private final Container rack;
        private final CheeseBranch.Place place;
        private final Map<Container, Integer> capacityMap;
        
        public TakeReadyCheeseFromRack(Container rack, CheeseBranch.Place place, Map<Container, Integer> capacityMap) {
            this.rack = rack;
            this.place = place;
            this.capacityMap = capacityMap;
        }
        
        @Override
        public Results run(NGameUI gui) throws InterruptedException {
            // Use centralized cheese tray size constant
            
            // Check if inventory has space
            int inventorySpace = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
            if (inventorySpace <= 2) {
                return Results.FAIL(); // Inventory full
            }
            
            // Find ALL ready cheese trays in the rack
            ArrayList<WItem> allTrays = CheeseInventoryOperations.getCheeseTraysFromContainer(gui, "Rack");
            ArrayList<WItem> readyTrays = new ArrayList<>();
            
            for (WItem tray : allTrays) {
                if (CheeseUtils.isCheeseReadyToMove(tray, place) || CheeseUtils.isCheeseReadyToSlice(tray)) {
                    readyTrays.add(tray);
                }
            }
            
            if (readyTrays.isEmpty()) {
                // Calculate capacity if not already done (rack with no ready cheese)
                if (capacityMap != null && !capacityMap.containsKey(rack)) {
                    int capacity = gui.getInventory("Rack").getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE);
                    capacityMap.put(rack, capacity);
                }
                return Results.SUCCESS(); // No ready cheese left in this rack
            }
            
            // Check if ALL ready trays can fit in inventory
            if (readyTrays.size() > inventorySpace) {
                return Results.FAIL(); // Can't fit all ready cheese, need to distribute inventory first
            }
            
            // Take ALL ready trays to inventory
            new TakeWItemsFromContainer(rack, readyTrays).run(gui);

            new OpenTargetContainer(rack).run(gui);
            
            // Calculate capacity after taking cheese (if not already done)
            if (capacityMap != null && !capacityMap.containsKey(rack)) {
                int capacity = gui.getInventory(rack.cap).getNumberFreeCoord(CheeseConstants.CHEESE_TRAY_SIZE);
                capacityMap.put(rack, capacity);
            }

            new CloseTargetContainer(rack).run(gui);
            
            return Results.SUCCESS(); // Rack is now empty of ready cheese
        }
    }
}