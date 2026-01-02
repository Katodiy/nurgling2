package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.NWItem;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Container;
import nurgling.tools.Context;
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
    boolean isQualityGrid = false;

    public LettuceAndPumpkinCollector(NArea input, NArea seedOutput, NArea itemOutput, NAlias items, NArea troughArea) {
        this.input = input;
        this.seedOutput = seedOutput;
        this.itemOutput = itemOutput;
        this.items = items;
        this.troughArea = troughArea;
        this.secondaryItemAlias = items.keys.contains("Head of Lettuce") ? "Lettuce Leaf" : "Pumpkin Flesh";
    }

    public LettuceAndPumpkinCollector(NArea input, NArea seedOutput, NArea itemOutput, NAlias items, NArea troughArea, boolean isQualityGrid) {
        this(input, seedOutput, itemOutput, items, troughArea);
        this.isQualityGrid = isQualityGrid;
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

                    if (!(testItems = gui.getInventory().getItems(new NAlias("Seed"))).isEmpty()) {
                        transferSeeds(gui);
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

        if (!(testItems = gui.getInventory().getItems(new NAlias("Seed"))).isEmpty()) {
            transferSeeds(gui);
        }

        if (!(testItems = gui.getInventory().getItems(new NAlias(this.secondaryItemAlias))).isEmpty()) {
            new TransferToPiles(itemOutput.getRCArea(), new NAlias(this.secondaryItemAlias)).run(gui);
        }

        return Results.SUCCESS();
    }

    private void transferSeeds(NGameUI gui) throws InterruptedException {
        if (isQualityGrid) {
            // Quality mode: transfer seeds to containers
            ArrayList<Container> containers = new ArrayList<>();
            for (Gob sm : Finder.findGobs(seedOutput.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name), null);
                cand.initattr(Container.Space.class);
                containers.add(cand);
            }

            if (containers.isEmpty())
                throw new RuntimeException("No container found in seed area!");

            Container container = containers.get(0);
            new TransferToContainer(container, new NAlias("Seed")).run(gui);
            new CloseTargetContainer(container).run(gui);
        } else {
            // Regular mode: transfer seeds to barrels, then trough, then piles
            ArrayList<Gob> barrels = Finder.findGobs(seedOutput, new NAlias("barrel"));

            if (!barrels.isEmpty()) {
                for (Gob barrel : barrels) {
                    TransferToBarrel tb = new TransferToBarrel(barrel, new NAlias("Seed"));
                    tb.run(gui);
                    if (!tb.isFull()) break;
                }

                if (troughArea != null && !gui.getInventory().getItems(new NAlias("Seed")).isEmpty()) {
                    Gob trough = Finder.findGob(troughArea, new NAlias("gfx/terobjs/trough"));
                    if (trough != null) {
                        new TransferToTrough(trough, new NAlias("Seed")).run(gui);
                    }
                }
            }

            if (!gui.getInventory().getItems(new NAlias("Seed")).isEmpty()) {
                new TransferToPiles(seedOutput.getRCArea(), new NAlias("Seed")).run(gui);
            }
        }
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
