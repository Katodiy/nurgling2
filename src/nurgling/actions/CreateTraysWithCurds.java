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
        while (traysCreated < count) {
            // 1. Find an empty tray
            WItem emptyTray = getNextEmptyTray(gui);
            if (emptyTray == null) {
                // TakeItems2 gets confused by filled trays, so we need custom logic
                gui.msg("No empty tray in inventory, fetching from storage containers...");
                boolean success = fetchEmptyTrayFromStorage(gui, context);
                if (!success) {
                    gui.error("No empty Cheese Trays available in storage");
                    break;
                }
                emptyTray = getNextEmptyTray(gui);
                if (emptyTray == null) {
                    gui.error("Still no empty Cheese Tray after fetching from storage!");
                    break;
                }
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

            // 4. Transfer filled tray to correct area
            new CheeseAreaMatcher.TransferCheeseTraysToCorrectAreas().run(gui);

            traysCreated++;
        }

        return Results.SUCCESS(traysCreated);
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
     * Try to take an empty cheese tray from a specific container
     */
    private boolean tryTakeEmptyTrayFromContainer(NGameUI gui, Container container) throws InterruptedException {
        try {
            // Use TakeItemsFromContainer to take any cheese tray
            java.util.HashSet<String> trayNames = new java.util.HashSet<>();
            trayNames.add("Cheese Tray");

            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);
            Results result = new TakeItemsFromContainer(container, trayNames, cheeseTrayAlias).run(gui);
            new CloseTargetContainer(container).run(gui);
            if (result.IsSuccess()) {
                // Check if the tray we got is actually empty
                WItem takenTray = getNextEmptyTray(gui);
                if (takenTray != null) {
                    gui.msg("Successfully took empty cheese tray from storage");
                    return true;
                } else {
                    gui.msg("Took filled tray, looking in next container...");
                    return false;
                }
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
