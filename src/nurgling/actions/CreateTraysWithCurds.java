package nurgling.actions;

import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.actions.bots.cheese.CheeseTrayUtils;

import java.util.ArrayList;

public class CreateTraysWithCurds implements Action {
    private final String curdType;
    private final int count;
    private final String cheeseTrayType = "Cheese Tray";
    NAlias cheeseTrayAlias = new NAlias("Cheese Tray");

    public CreateTraysWithCurds() {
        this("Cow's Curd", 2);
    }

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
//            gui.getInventory().getItems(new NAlias(new ArrayList<>(Arrays.asList("cheesetray")), new ArrayList<>(Arrays.asList("curd")) ))
            ArrayList<WItem> curds = gui.getInventory().getItems(curdType);
            if (curds.size() < 4) {
                Results curdRes = new TakeItems2(context, curdType, 4 - curds.size()).run(gui);
                if (!curdRes.isSuccess || gui.getInventory().getItems(curdType).size() < 4) {
                    gui.error("Not enough curds available to fill a tray");
                    break;
                }

                curds = gui.getInventory().getItems(curdType);
            }

            // 3. Use 4 curds on the tray
            for (int j = 0; j < 4; j++) {
                new UseItemOnItem(new NAlias(curdType), emptyTray).run(gui);
                int expected = curds.size() - (j + 1);
                NUtils.getUI().core.addTask(new WaitItems(gui.getInventory(), new NAlias(curdType), expected));
            }

            // 4. Transfer filled tray to correct area (removed non-functional CheeseAreaMatcher call)

            traysCreated++;
        }

        return Results.SUCCESS(traysCreated);
    }

    /**
     * Pre-fetch empty trays for the entire batch, reserving 4 slots for curds
     */
    private void fetchEmptyTraysForBatch(NGameUI gui, NContext context, int traysNeeded) throws InterruptedException {
        // Calculate how many empty trays we already have
        int emptyTraysInInventory = countEmptyTraysInInventory(gui);
        int traysToFetch = traysNeeded - emptyTraysInInventory;
        
        if (traysToFetch <= 0) {
            gui.msg("Already have enough empty trays in inventory: " + emptyTraysInInventory);
            return;
        }
        
        // Calculate available space for cheese trays (1x2 each) and reserve space for curds
        final haven.Coord TRAY_SIZE = new haven.Coord(1, 2);
        int availableTraySlots = gui.getInventory().getNumberFreeCoord(TRAY_SIZE);
        int availableSlots = availableTraySlots - 2; // Reserve 2 tray-sized slots for 4 curds (1x1 each)
        
        // Limit trays to fetch by available inventory space
        traysToFetch = Math.min(traysToFetch, availableSlots);
        
        if (traysToFetch <= 0) {
            gui.msg("Not enough inventory space to fetch more trays (reserving 4 slots for curds)");
            return;
        }
        
        gui.msg("Fetching " + traysToFetch + " empty trays from storage (have " + emptyTraysInInventory + ", need " + traysNeeded + ")");
        
        ArrayList<NContext.ObjectStorage> storages = context.getInStorages(cheeseTrayType);
        if (storages == null || storages.isEmpty()) {
            gui.error("No storage containers configured for " + cheeseTrayType);
            return;
        }
        
        int traysObtained = 0;
        for (NContext.ObjectStorage storage : storages) {
            if (storage instanceof Container && traysObtained < traysToFetch) {
                Container container = (Container) storage;
                traysObtained += fetchMultipleTraysFromContainer(gui, container, traysToFetch - traysObtained);
            }
        }
        
        gui.msg("Successfully obtained " + traysObtained + " additional empty trays");
    }
    
    /**
     * Count empty trays currently in inventory
     */
    private int countEmptyTraysInInventory(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        int emptyCount = 0;
        for (WItem tray : trays) {
            if (CheeseTrayUtils.isEmpty(tray)) {
                emptyCount++;
            }
        }
        return emptyCount;
    }
    
    /**
     * Manually fetch cheese trays from storage containers
     * Take any cheese tray and verify it's empty - if not, continue searching
     */
    private boolean fetchEmptyTrayFromStorage(NGameUI gui, NContext context) throws InterruptedException {
        ArrayList<NContext.ObjectStorage> storages = context.getInStorages(cheeseTrayType);
        if (storages == null || storages.isEmpty()) {
            gui.error("No storage containers configured for " + cheeseTrayType);
            return false;
        }
        
        for (NContext.ObjectStorage storage : storages) {
            if (storage instanceof Container) {
                Container container = (Container) storage;
                if (tryTakeEmptyTrayFromContainer(gui, container)) {
                    return true; // Successfully took an empty tray
                }
            }
        }
        
        gui.msg("No empty cheese trays found in any storage container");
        return false;
    }
    
    /**
     * Fetch multiple empty trays from a specific container
     * @return number of empty trays actually obtained
     */
    private int fetchMultipleTraysFromContainer(NGameUI gui, Container container, int maxTrays) throws InterruptedException {
        try {
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
                if (CheeseTrayUtils.isEmpty(tray)) {
                    emptyTraysFound++;
                }
            }
            
            if (emptyTraysFound == 0) {
                new CloseTargetContainer(container).run(gui);
                gui.msg("No empty trays in this container");
                return 0;
            }
            
            // Second pass: transfer empty trays up to maxTrays
            int traysToTake = Math.min(emptyTraysFound, maxTrays);
            gui.msg("Found " + emptyTraysFound + " empty trays, taking " + traysToTake);
            
            for (WItem tray : trays) {
                if (emptyTraysTransferred >= traysToTake) break;
                
                if (CheeseTrayUtils.isEmpty(tray)) {
                    tray.item.wdgmsg("transfer", haven.Coord.z);
                    NUtils.addTask(new nurgling.tasks.ISRemoved(tray.item.wdgid()));
                    emptyTraysTransferred++;
                }
            }
            
            new CloseTargetContainer(container).run(gui);
            gui.msg("Transferred " + emptyTraysTransferred + " empty trays from container");
            return emptyTraysTransferred;
            
        } catch (Exception e) {
            gui.msg("Error fetching multiple trays from container: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Try to take exactly 1 cheese tray from a specific container
     */
    private boolean tryTakeEmptyTrayFromContainer(NGameUI gui, Container container) throws InterruptedException {
        try {
            Gob containerGob = Finder.findGob(container.gobid);
            if (containerGob == null) return false;
            
            new PathFinder(containerGob).run(gui);
            new OpenTargetContainer(container).run(gui);
            
            // Check if this container has any cheese trays
            ArrayList<WItem> trays = gui.getInventory(container.cap).getItems(cheeseTrayAlias);
            if (!trays.isEmpty()) {
                // Take exactly 1 cheese tray from this container to inventory
                // TransferToContainer moves FROM inventory TO container, but we want the opposite
                // So we'll manually take the first tray we find
                WItem firstTray = trays.get(0);
                firstTray.item.wdgmsg("transfer", haven.Coord.z);
                
                // Wait for the transfer to complete
                NUtils.addTask(new nurgling.tasks.ISRemoved(firstTray.item.wdgid()));
                
                new CloseTargetContainer(container).run(gui);
                
                // Check if the tray we got is actually empty
                WItem takenTray = getNextEmptyTray(gui);
                if (takenTray != null) {
                    gui.msg("Successfully took empty cheese tray from storage");
                    return true;
                } else {
                    gui.msg("Took filled tray, looking in next container...");
                    return false;
                }
            } else {
                new CloseTargetContainer(container).run(gui);
                gui.msg("No cheese trays in this container");
            }
            
            return false;
            
        } catch (Exception e) {
            gui.msg("Error taking from container: " + e.getMessage());
            return false;
        }
    }

    private WItem getNextEmptyTray(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> trays = gui.getInventory().getItems(cheeseTrayAlias);
        for (WItem tray : trays) {
            // Use content inspection instead of resource name
            if (CheeseTrayUtils.isEmpty(tray)) {
                return tray;
            }
        }
        return null;
    }

}
