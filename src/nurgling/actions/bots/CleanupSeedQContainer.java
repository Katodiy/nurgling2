package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.ItemInfo;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitFreeHand;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class CleanupSeedQContainer implements Action {
    NArea seed;
    NAlias iseed;
    NArea trougha;

    public CleanupSeedQContainer(NArea seed, NAlias iseed, NArea trougha) {
        this.seed = seed;
        this.iseed = iseed;
        this.trougha = trougha;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob sm : Finder.findGobs(seed.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }

        if (containers.isEmpty())
            throw new RuntimeException("No container found in seed area!");
        Container container = containers.get(0);

        Gob containerGob = Finder.findGob(container.gobid);

        new PathFinder(containerGob).run(gui);

        new OpenTargetContainer(container).run(gui);

        // Get container capacity
        Coord containerSize = gui.getInventory(container.cap).isz;
        int containerCapacity = containerSize.x * containerSize.y;

        combineSmallSeedStacks(gui, container.cap, iseed);

        // Get all seeds in the container
        ArrayList<WItem> seeds = gui.getInventory(container.cap).getItems(iseed);

        if (seeds.size() <= containerCapacity / 2) {
            new CloseTargetContainer(container).run(gui);
            return Results.SUCCESS();
        }

        while (seeds.size() > containerCapacity / 2) {
            int fetchCount = Math.min(seeds.size() - (containerCapacity / 2), gui.getInventory().getFreeSpace());

            // Fetch low seeds to inventory
            new TakeAvailableItemsFromContainer(container, iseed, fetchCount, NInventory.QualityType.Low).run(gui);

            Gob trough = Finder.findGob(trougha, new NAlias("gfx/terobjs/trough"));

            if (!gui.getInventory().getItems(iseed).isEmpty() && trough != null) {
                new TransferToTrough(trough, iseed).run(gui);
            } else {
                throw new RuntimeException("Failed to transfer to trough!");
            }

            new PathFinder(containerGob).run(gui);

            new OpenTargetContainer(container).run(gui);

            seeds = gui.getInventory(container.cap).getItems(iseed);
        }

        new CloseTargetContainer(container).run(gui);

        return Results.SUCCESS();
    }

    private void combineSmallSeedStacks(NGameUI gui, String containerCap, NAlias iseed) throws InterruptedException {
        while (true) {
            ArrayList<WItem> seeds = gui.getInventory(containerCap).getItems(iseed);
            ArrayList<WItem> smallSeeds = new ArrayList<>();
            ArrayList<Integer> amounts = new ArrayList<>();
            for (WItem seed : seeds) {
                int amount = -1;
                if (seed.item.info != null) {
                    for (haven.ItemInfo info : seed.item.info) {
                        if (info instanceof haven.GItem.Amount) {
                            amount = ((haven.GItem.Amount) info).itemnum();
                            break;
                        }
                    }
                }
                if (amount > 0 && amount < 5) {
                    smallSeeds.add(seed);
                    amounts.add(amount);
                }
            }
            if (smallSeeds.size() < 2) break;

            int total = 0;
            for (int amt : amounts) total += amt;
            if (total < 5) break;

            int combined = amounts.get(0);
            WItem target = smallSeeds.get(0);
            for (int i = 1; i < smallSeeds.size(); i++) {
                int nextAmt = amounts.get(i);
                if (combined + nextAmt <= 50) {
                    NUtils.takeItemToHand(smallSeeds.get(i));
                    NUtils.itemact(target);
                    NUtils.addTask(new WaitFreeHand());
                    combined += nextAmt;
                }
                if (combined >= 5) break;
            }
        }
    }

}
