package nurgling.actions.bots;

import haven.Coord;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NISBox;
import nurgling.NUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.WaitItemFromPile;
import nurgling.tasks.WaitItems;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class SoilStockpileDropper implements Action {

    private static final Coord SOIL_SIZE = new Coord(1, 1);
    private static final int MIN_KEEP = 10;
    private static final NAlias SOIL = new NAlias("Soil", "Earthworm");

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NISBox pile = gui.getStockpile();
        if (pile == null) {
            return Results.ERROR("Stockpile not open");
        }

        // Initial cleanup: drop any existing soil in inventory
        dropAllSoil(gui);

        // Main loop
        while (true) {
            int count = pile.calcCount();
            if (count <= MIN_KEEP) {
                continue;
            }

            int toTake = count - MIN_KEEP;
            int freeSpace = gui.getInventory().getNumberFreeCoord(SOIL_SIZE);
            toTake = Math.min(toTake, freeSpace);

            if (toTake <= 0) {
                continue;
            }

            // Take from stockpile
            ((NUI) gui.ui).enableMonitor(gui.maininv);
            pile.transfer(toTake);
            WaitItemFromPile wifp = new WaitItemFromPile(toTake);
            NUtils.getUI().core.addTask(wifp);
            wifp.getItems();
            ((NUI) gui.ui).disableMonitor();

            // Drop all soil from inventory
            dropAllSoil(gui);
        }
    }

    private void dropAllSoil(NGameUI gui) throws InterruptedException {
        ArrayList<WItem> soilItems = gui.getInventory().getItems(SOIL);
        for (WItem item : soilItems) {
            NUtils.drop(item);
        }
        if (!soilItems.isEmpty()) {
            NUtils.addTask(new WaitItems(gui.getInventory(), SOIL, 0));
        }
    }
}
