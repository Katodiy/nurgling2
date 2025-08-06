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
        String eggs = "Silkworm Egg";
        String worms = "Silkworm";
        String leaves = "Mulberry Leaf";

        // Step 1: Ensure feeding cabinets have sufficient mulberry leaves (32 per cabinet)
        new DepositItemsToSpecArea(context, leaves, Specialisation.SpecName.silkwormFeeding, 32).run(gui);

        // Step 1.5: Calculate how many silkworms we can fit in feeding containers
        int totalSilkwormsNeeded = 0;
        NArea feedingArea = context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
        if (feedingArea != null) {
            ArrayList<Container> feedingContainers = new ArrayList<>();
            ArrayList<Gob> feedingGobs = Finder.findGobs(feedingArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
            for (Gob gob : feedingGobs) {
                Container cand = new Container(gob, contcaps.get(gob.ngob.name));
                cand.initattr(Container.Space.class);
                feedingContainers.add(cand);
            }
            
            // Check each feeding container to see how many silkworms it can fit
            for (Container feedingContainer : feedingContainers) {
                new PathFinder(Finder.findGob(feedingContainer.gobid)).run(gui);
                new OpenTargetContainer(feedingContainer).run(gui);
                
                int currentWorms = gui.getInventory(feedingContainer.cap).getItems(new NAlias(worms)).size();
                int spaceAvailable = Math.max(0, 56 - currentWorms); // Max 56 silkworms per container
                totalSilkwormsNeeded += spaceAvailable;
                
                new CloseTargetContainer(feedingContainer).run(gui);
            }
        }

        System.out.println(totalSilkwormsNeeded);

        // Step 2: Move hatched silkworms from herbalist tables to feeding cabinets (only collect what we need)
        if (totalSilkwormsNeeded > 0) {
            int wormsCollectedSoFar = 0;
            
            // Take silkworms from herbalist tables - use manual approach like eggs
            NArea htablesArea = context.getSpecArea(Specialisation.SpecName.htable, "Silkworm Eggs");
            if (htablesArea != null) {
                ArrayList<Container> htableContainers = new ArrayList<>();
                ArrayList<Gob> htableGobs = Finder.findGobs(htablesArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
                for (Gob gob : htableGobs) {
                    Container cand = new Container(gob, contcaps.get(gob.ngob.name));
                    cand.initattr(Container.Space.class);
                    htableContainers.add(cand);
                }
                
                // Take silkworms from herbalist table containers - only take what we need
                for (Container htableContainer : htableContainers) {
                    if (wormsCollectedSoFar >= totalSilkwormsNeeded) {
                        break; // We have enough silkworms
                    }
                    
                    new PathFinder(Finder.findGob(htableContainer.gobid)).run(gui);
                    new OpenTargetContainer(htableContainer).run(gui);
                    
                    // Get silkworm WItems from this container, excluding anything with "egg" in the name
                    ArrayList<String> silkwormKeys = new ArrayList<>();
                    silkwormKeys.add(worms);
                    ArrayList<String> exceptions = new ArrayList<>();
                    exceptions.add("egg");
                    ArrayList<WItem> silkwormItems = gui.getInventory(htableContainer.cap).getItems(new NAlias(silkwormKeys, exceptions));
                    
                    if (!silkwormItems.isEmpty()) {
                        // Only take what we still need
                        int wormsStillNeeded = totalSilkwormsNeeded - wormsCollectedSoFar;
                        ArrayList<WItem> wormsToTake = new ArrayList<>();
                        
                        for (int i = 0; i < Math.min(silkwormItems.size(), wormsStillNeeded); i++) {
                            wormsToTake.add(silkwormItems.get(i));
                        }
                        
                        if (!wormsToTake.isEmpty()) {
                            new TakeWItemsFromContainer(htableContainer, wormsToTake).run(gui);
                            wormsCollectedSoFar += wormsToTake.size();
                        }
                    }
                    
                    new CloseTargetContainer(htableContainer).run(gui);
                }
            }
            
            // Move worms to feeding cabinets if we collected any
            if (wormsCollectedSoFar > 0) {
                // Move worms to feeding cabinets - use manual approach since silkworms don't have a source area
                feedingArea = context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
                if (feedingArea != null) {
                    ArrayList<Container> feedingContainers = new ArrayList<>();
                    ArrayList<Gob> feedingGobs = Finder.findGobs(feedingArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
                    for (Gob gob : feedingGobs) {
                        Container cand = new Container(gob, contcaps.get(gob.ngob.name));
                        cand.initattr(Container.Space.class);
                        feedingContainers.add(cand);
                    }
                    
                    // Transfer silkworms to feeding containers
                    for (Container feedingContainer : feedingContainers) {
                        if (gui.getInventory().getItems(new NAlias(worms)).isEmpty()) {
                            break; // No more silkworms to transfer
                        }
                        
                        new PathFinder(Finder.findGob(feedingContainer.gobid)).run(gui);
                        new OpenTargetContainer(feedingContainer).run(gui);
                        
                        // Check how many silkworms this container currently has
                        int currentWorms = gui.getInventory(feedingContainer.cap).getItems(new NAlias(worms)).size();
                        int spaceNeeded = Math.max(0, 56 - currentWorms); // Max 56 silkworms per container
                        
                        if (spaceNeeded > 0) {
                            new TransferToContainer(feedingContainer, new NAlias(worms)).run(gui);
                        }
                        
                        new CloseTargetContainer(feedingContainer).run(gui);
                    }
                }
            }
        }

        // Step 3: Move eggs from storage to now-empty herbalist tables
        while (true) {
            int eggsBefore = gui.getInventory().getItems(new NAlias(eggs)).size();

            context.addInItem(eggs, null);
            // Take eggs from egg storage
            new TakeItems2(context, eggs, gui.getInventory().getFreeSpace()).run(gui);

            int eggsAfter = gui.getInventory().getItems(new NAlias(eggs)).size();
            int eggsCollected = eggsAfter - eggsBefore;

            if (eggsCollected > 0) {
                // Move eggs to herbalist tables using existing transfer logic
                NArea htablesArea = context.getSpecArea(Specialisation.SpecName.htable, eggs);

                ArrayList<Container> containers = new ArrayList<>();
                ArrayList<Gob> gobs = Finder.findGobs(htablesArea, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
                for (Gob gob : gobs) {
                    Container cand = new Container(gob, contcaps.get(gob.ngob.name));
                    cand.initattr(Container.Space.class);
                    containers.add(cand);
                }

                new FillContainers2(containers, eggs, context).run(gui);

                if(!gui.getInventory().getItems(eggs).isEmpty()) {
                    break;
                }
            } else {
                break;
            }
        }

        NContext freshContext = new NContext(gui);
        // Step 4: Clean up any remaining items in inventory
        new FreeInventory2(freshContext).run(gui);

        return Results.SUCCESS();
    }
}
