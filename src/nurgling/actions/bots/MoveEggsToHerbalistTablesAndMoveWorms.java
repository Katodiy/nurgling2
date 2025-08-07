package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static nurgling.areas.NContext.contcaps;

/**
 * Multi-step silk processing bot:
 * 1. Ensure feeding cabinets have mulberry leaves (32 per cabinet)
 * 2. Move hatched silkworms from herbalist tables to feeding cabinets
 * 3. Move eggs from storage to now-empty herbalist tables
 */
public class MoveEggsToHerbalistTablesAndMoveWorms implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String moth = "Silkmoth";
        String eggs = "Silkworm Egg";
        String worms = "Silkworm";
        String leaves = "Mulberry Leaf";
        String cacoons = "Silkworm Cocoon";

        DepositItemsToSpecArea depositItemsActionCacoons = new DepositItemsToSpecArea(context, new NAlias(cacoons, moth), Specialisation.SpecName.silkmothBreeding, Specialisation.SpecName.silkwormFeeding, 16);
        depositItemsActionCacoons.run(gui);

        // Step 1: Ensure feeding cabinets have sufficient mulberry leaves (32 per cabinet)
        DepositItemsToSpecArea depositItemsActionLeafs = new DepositItemsToSpecArea(context, new NAlias(leaves), Specialisation.SpecName.silkwormFeeding, 32);
        depositItemsActionLeafs.run(gui);

        // Step 1.5: Calculate how many silkworms we can fit in feeding containers
        int totalSilkwormsNeeded = depositItemsActionLeafs.getContainerFreeSpaceMap().values().stream()
                .mapToInt(freeSpace -> Math.min(freeSpace, 52)) // Cap each container at 52 silkworms max
                .sum();

        // Step 2: Move hatched silkworms from herbalist tables to feeding cabinets
        // Also record herbalist table capacity for eggs during this pass
        int totalEggsNeeded = 0;
        ArrayList<Container> htableContainers = new ArrayList<>();
        ArrayList<Container> feedingContainers = new ArrayList<>();

        // Pre-populate feeding containers for efficiency
        NArea feedingArea = context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
        if (feedingArea != null) {
            ArrayList<Gob> feedingGobs = Finder.findGobs(feedingArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
            for (Gob gob : feedingGobs) {
                Container cand = new Container(gob, contcaps.get(gob.ngob.name));
                cand.initattr(Container.Space.class);
                feedingContainers.add(cand);
            }
        }

        if (totalSilkwormsNeeded > 0) {
            int wormsTransferredTotal = 0;

            // Take silkworms from herbalist tables - use container-by-container approach
            NArea htablesArea = context.getSpecArea(Specialisation.SpecName.htable, "Silkworm Eggs");
            if (htablesArea != null) {
                ArrayList<Gob> htableGobs = Finder.findGobs(htablesArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
                for (Gob gob : htableGobs) {
                    Container cand = new Container(gob, contcaps.get(gob.ngob.name));
                    cand.initattr(Container.Space.class);
                    htableContainers.add(cand);
                }

                // Process each herbalist table container individually
                for (Container htableContainer : htableContainers) {
                    if (wormsTransferredTotal >= totalSilkwormsNeeded) {
                        // Still need to check remaining containers for egg capacity only
                        new PathFinder(Finder.findGob(htableContainer.gobid)).run(gui);
                        new OpenTargetContainer(htableContainer).run(gui);

                        // Record free space for eggs
                        int freeSpace = gui.getInventory(htableContainer.cap).getFreeSpace();
                        totalEggsNeeded += freeSpace;

                        new CloseTargetContainer(htableContainer).run(gui);
                        continue;
                    }

                    new PathFinder(Finder.findGob(htableContainer.gobid)).run(gui);
                    new OpenTargetContainer(htableContainer).run(gui);

                    // Get all silkworm WItems from this container, excluding anything with "egg" in the name
                    ArrayList<String> silkwormKeys = new ArrayList<>();
                    silkwormKeys.add(worms);
                    ArrayList<String> exceptions = new ArrayList<>();
                    exceptions.add("egg");
                    ArrayList<WItem> silkwormItems = gui.getInventory(htableContainer.cap).getItems(new NAlias(silkwormKeys, exceptions));

                    // Transfer silkworms from this container in batches based on inventory space
                    int wormsFromThisContainer = 0;
                    while (!silkwormItems.isEmpty() &&
                           wormsTransferredTotal + wormsFromThisContainer < totalSilkwormsNeeded) {

                        // Take what fits in inventory
                        int inventorySpace = gui.getInventory().getFreeSpace();
                        int wormsToTake = Math.min(silkwormItems.size(), inventorySpace);

                        if (wormsToTake == 0) {
                            break; // No inventory space
                        }

                        ArrayList<WItem> wormsToTakeBatch = new ArrayList<>();
                        for (int i = 0; i < wormsToTake; i++) {
                            wormsToTakeBatch.add(silkwormItems.get(i));
                        }

                        new TakeWItemsFromContainer(htableContainer, wormsToTakeBatch).run(gui);
                        wormsFromThisContainer += wormsToTakeBatch.size();

                        // Remove taken items from our tracking list
                        for (int i = 0; i < wormsToTake; i++) {
                            silkwormItems.remove(0);
                        }

                        // Immediately transfer to feeding containers to free up inventory
                        context.getSpecArea(Specialisation.SpecName.silkwormFeeding);

                        // Continue processing htables without dropping off when there is inventory room
                        if(gui.getInventory(htableContainer.cap).getFreeSpace() > 1) {
                            continue;
                        }

                        for (Container feedingContainer : feedingContainers) {
                            if (gui.getInventory().getItems(new NAlias(worms)).isEmpty()) {
                                break; // No more silkworms in inventory
                            }

                            new PathFinder(Finder.findGob(feedingContainer.gobid)).run(gui);
                            new OpenTargetContainer(feedingContainer).run(gui);

                            // Check how many silkworms this container currently has
                            int currentWorms = gui.getInventory(feedingContainer.cap).getItems(new NAlias(worms)).size();
                            int spaceAvailable = Math.max(0, 56 - currentWorms);

                            if (spaceAvailable > 0) {
                                new TransferToContainer(feedingContainer, new NAlias(worms)).run(gui);
                            }

                            new CloseTargetContainer(feedingContainer).run(gui);
                        }
                    }

                    wormsTransferredTotal += wormsFromThisContainer;

                    // Record free space for eggs (done once per container)
                    new PathFinder(Finder.findGob(htableContainer.gobid)).run(gui);
                    new OpenTargetContainer(htableContainer).run(gui);
                    int freeSpace = gui.getInventory(htableContainer.cap).getFreeSpace();
                    totalEggsNeeded += freeSpace;

                    System.out.println(freeSpace);

                    new CloseTargetContainer(htableContainer).run(gui);
                }
            }

            System.out.println(totalEggsNeeded);
        }

        // Step 3: Move eggs from storage to now-empty herbalist tables (only fetch what's needed)
        if (totalEggsNeeded > 0) {
            context.addInItem(eggs, null);
            // Take only what we need (or what fits in inventory, whichever is smaller)
            int eggsToFetch = Math.min(totalEggsNeeded, gui.getInventory().getFreeSpace());
            new TakeItems2(context, eggs, eggsToFetch).run(gui);

            int eggsCollected = gui.getInventory().getItems(new NAlias(eggs)).size();

            if (eggsCollected > 0) {
                // Move eggs to herbalist tables using existing transfer logic
                new FillContainers2(htableContainers, eggs, context).run(gui);
            }
        }

        NContext freshContext = new NContext(gui);
        // Step 4: Clean up any remaining items in inventory
        new FreeInventory2(freshContext).run(gui);

        return Results.SUCCESS();
    }
}

