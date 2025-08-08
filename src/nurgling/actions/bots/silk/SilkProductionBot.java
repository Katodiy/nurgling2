package nurgling.actions.bots.silk;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.actions.bots.CollectRemainingCocoons;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static nurgling.areas.NContext.contcaps;

/**
 * Multistep silk processing bot:
 * 1. Ensure feeding cabinets have mulberry leaves (32 per cabinet)
 * 2. Move hatched silkworms from herbalist tables to feeding cabinets
 * 3. Move eggs from storage to now-empty herbalist tables
 */
public class SilkProductionBot implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        String moth = "Silkmoth";
        String eggs = "Silkworm Egg";
        String leaves = "Mulberry Leaf";
        String cacoons = "Silkworm Cocoon";
        

        boolean areasValid = validateRequiredAreas(gui);

        if(!areasValid) {
            return Results.ERROR("Not all required areas are defined");
        }

        // Step 1: Collect all ready silkworm eggs and drop them off at storage area.
        gui.msg("Collecting and storing silkworm eggs.");
        new CollectAndMoveSilkwormEggs().run(gui);

        // Step 2: Collect all ready silkworm cocoons and drop them off at silkmoth breeding area.
        gui.msg("Collecting ready silkworm cocoons to breeding area.");
        DepositItemsToSpecArea depositItemsActionCacoons = new DepositItemsToSpecArea(context, new NAlias(cacoons, moth), Specialisation.SpecName.silkmothBreeding, Specialisation.SpecName.silkwormFeeding, 16);
        depositItemsActionCacoons.run(gui);

        gui.msg("Dropping off remaining silkworm cocoons to storage area.");
        new CollectRemainingCocoons().run(gui);

        // Step 3: Ensure feeding cupboards have sufficient mulberry leaves (32 per cabinet).
        gui.msg("Refilling mulberry leafs.");
        DepositItemsToSpecArea depositItemsActionLeafs = new DepositItemsToSpecArea(context, new NAlias(leaves), Specialisation.SpecName.silkwormFeeding, 32);
        depositItemsActionLeafs.run(gui);

        // Step 4: Calculate how many silkworms we can fit in feeding containers
        int totalSilkwormsNeeded = depositItemsActionLeafs.getContainerFreeSpaceMap().values().stream()
                .mapToInt(freeSpace -> Math.min(freeSpace, 52)) // Cap each container at 52 silkworms max
                .sum();

        // Step 5: Move hatched silkworms from herbalist tables to feeding cupboards
        // Also record herbalist table capacity for eggs during this pass
        gui.msg("Moving hatches silkworms from herbalist table to feeding cupboards.");
        TransferSilkwormsFromHTablesToFeeding transferAction = new TransferSilkwormsFromHTablesToFeeding(totalSilkwormsNeeded);

        transferAction.run(gui);
        int totalEggsNeeded = transferAction.getTotalEggsNeeded();
        
        // Get htable containers for egg filling step
        ArrayList<Container> htableContainers = new ArrayList<>();
        NArea htablesArea = context.getSpecArea(Specialisation.SpecName.htable, "Silkworm Egg");
        if (htablesArea != null) {
            htableContainers = createContainersFromArea(htablesArea);
        }

        // Step 6: Move eggs from storage to now-empty herbalist tables (only fetch what's needed)
        gui.msg("Filling herbalist tables with eggs.");
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
        // Step 7: Clean up any remaining items in inventory
        gui.msg("Free up inventory.");
        new FreeInventory2(freshContext).run(gui);

        // Step 8: Rearrange silkmoths in the breeding containers to maximize pairs.
        gui.msg("Rearranging silkmoths to maximize pairs.");
        new ArrangeSilkmothPairs().run(gui);

        return Results.SUCCESS();
    }

    private boolean validateRequiredAreas(NGameUI gui) {
        NArea feedingArea = NContext.findSpecGlobal("silkwormFeeding");
        if (feedingArea == null) {
            gui.error("Silkworm Feeding spec area is required, but not found.");
            return false;
        }

        NArea breedingArea = NContext.findSpecGlobal("silkmothBreeding");
        if (breedingArea == null) {
            gui.error("Silkmoth Breeding spec area is required, but not found.");
            return false;
        }

        NArea htablesSilkwormEgg = NContext.findSpecGlobal("htable", "Silkworm Egg");
        if (htablesSilkwormEgg == null) {
            gui.error("Herbalist Table spec with Silkworm Egg sub spec area is required, but not found.");
            return false;
        }

        TreeMap<Integer,NArea> putSilkwormEgg = NContext.findOutsGlobal("Silkworm Egg");
        NArea takeSilkwormEgg = NContext.findInGlobal("Silkworm Egg");
        if (putSilkwormEgg.isEmpty() || takeSilkwormEgg == null) {
            gui.error("PUT and TAKE Silkworm Egg area is required, but not found.");
            return false;
        }

        TreeMap<Integer,NArea> putCocoon = NContext.findOutsGlobal("Silkworm Cocoon");
        NArea takeCocoon = NContext.findInGlobal("Silkworm Cocoon");
        if (putCocoon.isEmpty() || takeCocoon == null) {
            gui.error("PUT and TAKE Silkworm Cocoon area is required, but not found.");
            return false;
        }

        NArea takeMulberryLeaf = NContext.findInGlobal("Mulberry Leaf");
        if (takeMulberryLeaf == null) {
            gui.error("TAKE Mulberry Leaf area is required, but not found.");
            return false;
        }

        return true;
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
    
}

