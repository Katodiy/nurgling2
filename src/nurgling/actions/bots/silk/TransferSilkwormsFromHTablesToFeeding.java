package nurgling.actions.bots.silk;

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
import java.util.List;
import java.util.Map;

import static nurgling.areas.NContext.contcaps;

/**
 * Transfers silkworms from herbalist tables to feeding containers
 * Records available space in herbalist tables for future egg placement
 */
public class TransferSilkwormsFromHTablesToFeeding implements Action {
    private final int totalSilkwormsNeeded;
    private int totalEggsNeeded = 0;
    
    public TransferSilkwormsFromHTablesToFeeding(int totalSilkwormsNeeded) {
        this.totalSilkwormsNeeded = totalSilkwormsNeeded;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String worms = "Silkworm";
        NAlias wormsAlias = new NAlias(new ArrayList<>(List.of(worms)), new ArrayList<>(List.of("egg")));
        
        totalEggsNeeded = 0;
        ArrayList<Container> htableContainers = new ArrayList<>();
        ArrayList<Container> feedingContainers = new ArrayList<>();
        
        // Pre-populate feeding containers for efficiency
        NArea feedingArea = context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
        if (feedingArea != null) {
            feedingContainers = createContainersFromArea(feedingArea);
        }
        
        if (totalSilkwormsNeeded > 0) {
            int wormsTransferredTotal = 0;
            
            // Take silkworms from herbalist tables - use container-by-container approach
            NArea htablesArea = context.getSpecArea(Specialisation.SpecName.htable, "Silkworm Egg");
            if (htablesArea != null) {
                htableContainers = createContainersFromArea(htablesArea);
                
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
                    ArrayList<WItem> silkwormItems = gui.getInventory(htableContainer.cap).getItems(wormsAlias);
                    
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
                        
                        context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
                        
                        // Continue processing htables without dropping off when there is inventory room
                        if(gui.getInventory().getFreeSpace() > 1) {
                            continue;
                        }
                        
                        dropOffWormsToFeedingContainers(gui, feedingContainers, wormsAlias, context);
                        context.getSpecArea(Specialisation.SpecName.htable, "Silkworm Egg");
                    }
                    
                    // Drop off any remaining silkworms in inventory after finishing this container
                    if (!gui.getInventory().getItems(wormsAlias).isEmpty()) {
                        dropOffWormsToFeedingContainers(gui, feedingContainers, wormsAlias, context);
                        context.getSpecArea(Specialisation.SpecName.htable, "Silkworm Egg");
                    }
                    
                    wormsTransferredTotal += wormsFromThisContainer;
                    
                    // Record free space for eggs (done once per container)
                    new PathFinder(Finder.findGob(htableContainer.gobid)).run(gui);
                    new OpenTargetContainer(htableContainer).run(gui);
                    int freeSpace = gui.getInventory(htableContainer.cap).getFreeSpace();
                    totalEggsNeeded += freeSpace;
                    
                    new CloseTargetContainer(htableContainer).run(gui);
                }
            }
        }
        
        return Results.SUCCESS();
    }
    
    public int getTotalEggsNeeded() {
        return totalEggsNeeded;
    }
    
    private ArrayList<Container> createContainersFromArea(NArea area) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        for (Gob gob : gobs) {
            Container cand = new Container(gob, contcaps.get(gob.ngob.name));
            cand.initattr(Container.Space.class);
            containers.add(cand);
        }
        return containers;
    }
    
    private void dropOffWormsToFeedingContainers(NGameUI gui, ArrayList<Container> feedingContainers, NAlias wormsAlias, NContext context) throws InterruptedException {
        context.getSpecArea(Specialisation.SpecName.silkwormFeeding);
        
        for (Container feedingContainer : feedingContainers) {
            if (gui.getInventory().getItems(wormsAlias).isEmpty()) {
                break; // No more silkworms in inventory
            }
            
            new PathFinder(Finder.findGob(feedingContainer.gobid)).run(gui);
            new OpenTargetContainer(feedingContainer).run(gui);
            
            int currentWorms = gui.getInventory(feedingContainer.cap).getItems(wormsAlias).size();
            int spaceAvailable = Math.max(0, 56 - currentWorms);
            
            if (spaceAvailable > 0) {
                new TransferToContainer(feedingContainer, wormsAlias).run(gui);
            }
            
            new CloseTargetContainer(feedingContainer).run(gui);
        }
    }
}