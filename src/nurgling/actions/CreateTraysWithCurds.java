package nurgling.actions;

import haven.WItem;
import nurgling.*;
import nurgling.areas.NContext;
import nurgling.tasks.WaitItems;
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
                Results traysRes = new TakeItems2(context, cheeseTrayType, 1).run(gui);
                if (!traysRes.isSuccess) {
                    gui.error("No Cheese Trays available");
                    break;
                }
                emptyTray = getNextEmptyTray(gui);
                if (emptyTray == null) {
                    gui.error("Still no empty Cheese Tray after taking!");
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
