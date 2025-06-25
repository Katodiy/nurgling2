package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NWItem;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LettuceAndPumpkinCollector implements Action {
    NArea input;
    NArea seedOutput;
    NArea itemOutput;
    NArea troughArea;
    NAlias items;
    String secondaryItemAlias;

    public LettuceAndPumpkinCollector(NArea input, NArea seedOutput, NArea itemOutput, NAlias items, NArea troughArea) {
        this.input = input;
        this.seedOutput = seedOutput;
        this.itemOutput = itemOutput;
        this.items = items;
        this.troughArea = troughArea;
        this.secondaryItemAlias = items.keys.contains("Head of Lettuce") ? "Lettuce Leaf" : "Pumpkin Flesh";
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NAlias collected_items = new NAlias(items.keys, new ArrayList<>(Arrays.asList("stockpile", "barrel")));
        ArrayList<WItem> testItems;

        int totalItemsThatCanFit = 0;
        int currentQuantity = 0;

        while (!Finder.findGobs(input, collected_items).isEmpty()) {
            if (!(testItems = gui.getInventory().getItems(items)).isEmpty()) {
                totalItemsThatCanFit = Math.max(gui.getInventory().getNumberFreeCoord(testItems.get(0)) + 1, totalItemsThatCanFit);
                currentQuantity = gui.getInventory().getItems(items).size();

                if ((this.items.keys.contains("Head of Lettuce") && gui.getInventory().getNumberFreeCoord(testItems.get(0)) <= Math.floor(totalItemsThatCanFit/2))
                        || (this.items.keys.contains("Pumpkin") && gui.getInventory().getNumberFreeCoord(testItems.get(0)) == 0)) {
                    splitItems(gui);

                    ArrayList<Gob> barrels = Finder.findGobs(seedOutput, new NAlias("barrel"));
                    HashMap<Gob, AtomicBoolean> barrelInfo = new HashMap();

                    if (!(testItems = gui.getInventory().getItems(new NAlias("Seed"))).isEmpty()) {

                        if (!barrels.isEmpty()) {
                            for (Gob barrel : barrels) {
                                TransferToBarrel tb;
                                (tb = new TransferToBarrel(barrel, new NAlias("Seed"))).run(gui);
                                barrelInfo.put(barrel, new AtomicBoolean(tb.isFull()));
                            }

                            Gob trough = Finder.findGob(troughArea, new NAlias("gfx/terobjs/trough"));

                            if (!gui.getInventory().getItems(new NAlias("Seed")).isEmpty()) {
                                new TransferToTrough(trough, new NAlias("Seed")).run(gui);
                            }
                        }

                        if (gui.getInventory().getNumberFreeCoord(testItems.get(0)) == 0) {
                            new TransferToPiles(seedOutput.getRCArea(), new NAlias("Seed")).run(gui);
                        }
                    }

                    if (!(testItems = gui.getInventory().getItems(new NAlias(this.secondaryItemAlias))).isEmpty()) {
                        new TransferToPiles(itemOutput.getRCArea(), new NAlias(this.secondaryItemAlias)).run(gui);

                    }

                    currentQuantity = 0;
                }
            }

            Gob item = Finder.findGob(collected_items);
            if (item == null)
                break;
            if (item.rc.dist(gui.map.player().rc) > MCache.tilesz2.x) {
                PathFinder pf = new PathFinder(item);
                pf.run(gui);
            }
            NUtils.takeFromEarth(item);
            NUtils.getUI().core.addTask(new WaitMoreItems(NUtils.getGameUI().getInventory(), items, currentQuantity+1));
        }

        splitItems(gui);

        ArrayList<Gob> barrels = Finder.findGobs(seedOutput, new NAlias("barrel"));
        HashMap<Gob, AtomicBoolean> barrelInfo = new HashMap();

        if (!(testItems = gui.getInventory().getItems(new NAlias("Seed"))).isEmpty()) {

            if (!barrels.isEmpty()) {
                for (Gob barrel : barrels) {
                    TransferToBarrel tb;
                    (tb = new TransferToBarrel(barrel, new NAlias("Seed"))).run(gui);
                    barrelInfo.put(barrel, new AtomicBoolean(tb.isFull()));
                }

                Gob trough = Finder.findGob(troughArea, new NAlias("gfx/terobjs/trough"));

                if (!gui.getInventory().getItems(new NAlias("Seed")).isEmpty()) {
                    new TransferToTrough(trough, new NAlias("Seed")).run(gui);
                }
            }

            if (gui.getInventory().getNumberFreeCoord(testItems.get(0)) == 0) {
                new TransferToPiles(seedOutput.getRCArea(), new NAlias("Seed")).run(gui);
            }
        }

        if (!(testItems = gui.getInventory().getItems(new NAlias(this.secondaryItemAlias))).isEmpty()) {
            new TransferToPiles(itemOutput.getRCArea(), new NAlias(this.secondaryItemAlias)).run(gui);
        }

        return Results.SUCCESS();
    }

    private void splitItems(NGameUI gui) throws InterruptedException {
        NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
        ArrayList<WItem> items = NUtils.getGameUI().getInventory().getItems(this.items);
        for (WItem item : items) {
            if(this.items.keys.contains("Head of Lettuce")) {
                new SelectFlowerAction("Split", (NWItem) item).run(gui);
            } else if(this.items.keys.contains("Pumpkin")) {
                new SelectFlowerAction("Slice", (NWItem) item).run(gui);
            }

        }
    }
}
