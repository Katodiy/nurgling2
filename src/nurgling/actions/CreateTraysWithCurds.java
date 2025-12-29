package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.actions.bots.cheese.CheeseConstants;
import nurgling.actions.bots.cheese.CheeseInventoryOperations;

import java.util.ArrayList;

public class CreateTraysWithCurds implements Action {
    private final String curdType;
    private final int count;
    private final String cheeseTrayType = CheeseConstants.CHEESE_TRAY_NAME;
    NAlias cheeseTrayAlias = CheeseConstants.CHEESE_TRAY_ALIAS;
    private int lastTraysCreated = 0;

    public CreateTraysWithCurds(String curdType, int count) {
        this.curdType = curdType;
        this.count = count;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        context.addInItem(curdType, null);
        context.addInItem(cheeseTrayType, null);

        int traysCreated = 0;
        // Pre-fetch empty trays to minimize storage trips
        fetchEmptyTraysForBatch(gui, context, count);
        
        while (traysCreated < count) {
            // 1. Find an empty tray (should be available from pre-fetch)
            WItem emptyTray = getNextEmptyTray(gui);
            if (emptyTray == null) {
                gui.error("No empty tray available - this shouldn't happen after pre-fetch");
                break;
            }

           // 2. Make sure you have 4 curds
            ArrayList<WItem> curds = gui.getInventory().getItems(curdType);
            if (curds.size() < CheeseConstants.CURDS_PER_TRAY) {
                int needed = CheeseConstants.CURDS_PER_TRAY - curds.size();
                
                // Check if storage has at least 'needed' curds available
                ArrayList<NContext.ObjectStorage> curdStorages = context.getInStorages(curdType);
                int availableInStorage = 0;
                
                if (curdStorages != null && !curdStorages.isEmpty()) {
                    for (NContext.ObjectStorage storage : curdStorages) {
                        if (storage instanceof Container) {
                            Container container = (Container) storage;
                            Gob containerGob = Finder.findGob(container.gobid);
                            if (containerGob != null) {
                                new PathFinder(containerGob).run(gui);
                                new OpenTargetContainer(container).run(gui);
                                
                                ArrayList<WItem> curdsInContainer = gui.getInventory(container.cap).getItems(curdType);
                                availableInStorage += curdsInContainer.size();
                                
                                new CloseTargetContainer(container).run(gui);
                            }
                        }
                    }
                }
                
                // Only proceed if we have enough curds in storage
                if (availableInStorage < needed) {
                    gui.error("Not enough curds available to fill a tray (need " + needed + ", found " + availableInStorage + ")");
                    break;
                }
                
                // Now safely take the curds
                Results curdRes = new TakeItems2(context, curdType, needed).run(gui);
                if (!curdRes.isSuccess || gui.getInventory().getItems(curdType).size() < CheeseConstants.CURDS_PER_TRAY) {
                    gui.error("Failed to retrieve curds from storage");
                    break;
                }

                curds = gui.getInventory().getItems(curdType);
            }

            // 3. Use 4 curds on the tray
            for (int j = 0; j < CheeseConstants.CURDS_PER_TRAY; j++) {
                new UseItemOnItem(new NAlias(curdType), emptyTray).run(gui);
                int expected = curds.size() - (j + 1);
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(), new NAlias(curdType), expected));
            }

            // 4. Transfer filled tray to correct area (removed non-functional CheeseAreaMatcher call)

            traysCreated++;
        }

        lastTraysCreated = traysCreated;
        return Results.SUCCESS();
    }
    
    /**
     * Get the number of trays created in the last run
     * @return Number of trays created, or 0 if not yet run
     */
    public int getLastTraysCreated() {
        return lastTraysCreated;
    }

    /**
     * Pre-fetch empty trays for the entire batch, reserving 4 slots for curds
     */
    private void fetchEmptyTraysForBatch(NGameUI gui, NContext context, int traysNeeded) throws InterruptedException {
        // Calculate how many empty trays we already have
        int emptyTraysInInventory = countEmptyTraysInInventory(gui);
        int traysToFetch = traysNeeded - emptyTraysInInventory;
        
        if (traysToFetch <= 0) {
            return;
        }
        
        // Calculate available space for cheese trays (1x2 each) and reserve space for curds
        // Use centralized cheese tray size constant
        int availableTraySlots = CheeseInventoryOperations.getAvailableCheeseTraySlotsInInventory(gui);
        int availableSlots = availableTraySlots - CheeseConstants.RESERVED_CURD_SLOTS; // Reserve slots for curds
        
        // Limit trays to fetch by available inventory space
        traysToFetch = Math.min(traysToFetch, availableSlots);
        
        if (traysToFetch <= 0) {
            return;
        }
        
        ArrayList<NContext.ObjectStorage> storages = context.getInStorages(cheeseTrayType);
        if (storages == null || storages.isEmpty()) {
            return;
        }
        
        int traysObtained = 0;
        for (NContext.ObjectStorage storage : storages) {
            if (storage instanceof Container && traysObtained < traysToFetch) {
                Container container = (Container) storage;
                traysObtained += fetchMultipleTraysFromContainer(gui, container, traysToFetch - traysObtained);
            }
        }
    }
    
    /**
     * Count empty trays currently in inventory
     */
    private int countEmptyTraysInInventory(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        int emptyCount = 0;
        for (WItem tray : trays) {
            if (CheeseUtils.isEmpty(tray)) {
                emptyCount++;
            }
        }
        return emptyCount;
    }
    
    /**
     * Fetch multiple empty trays from a specific container
     * @return number of empty trays actually obtained
     */
    private int fetchMultipleTraysFromContainer(NGameUI gui, Container container, int maxTrays) throws InterruptedException {
            Gob containerGob = Finder.findGob(container.gobid);
            if (containerGob == null) return 0;
            
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(container).run(gui);
            
            // Get all cheese trays in this container
            ArrayList<WItem> trays = gui.getInventory(container.cap).getItems(cheeseTrayAlias);
            int emptyTraysFound = 0;
            int emptyTraysTransferred = 0;
            
            // First pass: count empty trays available
            for (WItem tray : trays) {
                if (CheeseUtils.isEmpty(tray)) {
                    emptyTraysFound++;
                }
            }
            
            if (emptyTraysFound == 0) {
                new CloseTargetContainer(container).run(gui);
                return 0;
            }
            
            // Second pass: transfer empty trays up to maxTrays
            int traysToTake = Math.min(emptyTraysFound, maxTrays);
            
            for (WItem tray : trays) {
                if (emptyTraysTransferred >= traysToTake) break;
                
                if (CheeseUtils.isEmpty(tray)) {
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                    emptyTraysTransferred++;
                }
            }
            
            new CloseTargetContainer(container).run(gui);
            return emptyTraysTransferred;
    }

    private WItem getNextEmptyTray(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        for (WItem tray : trays) {
            // Use content inspection instead of resource name
            if (CheeseUtils.isEmpty(tray)) {
                return tray;
            }
        }
        return null;
    }

}
