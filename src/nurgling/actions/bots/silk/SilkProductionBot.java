package nurgling.actions.bots.silk;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.actions.bots.CollectAndKillRemainingCocoons;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import nurgling.NInventory.QualityType;

import java.util.ArrayList;
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
        // Known problems:
        // Silkmoth Breeding containers will ALL be opened at least 3 times when no changes need to happen

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
        // 1 OPEN ALL SILKMOTH BREEDING CONTAINERS
        new CollectAndMoveSilkwormEggs().run(gui);

        // Step 2: Collect all ready silkworm cocoons and drop them off at silkmoth breeding area.
        gui.msg("Collecting ready silkworm cocoons to breeding area.");
        // 1 OPEN ALL SILKMOTH BREEDING CONTAINERS
        DepositItemsToSpecArea depositItemsActionCacoons = new DepositItemsToSpecArea(context, new NAlias(moth, cacoons), Specialisation.SpecName.silkmothBreeding, Specialisation.SpecName.silkwormFeeding, 16);
        depositItemsActionCacoons.run(gui);

        gui.msg("Killing and dropping off remaining silkworm cocoons to storage area.");
        new CollectAndKillRemainingCocoons().run(gui);

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

        // Step 6: Move eggs from storage to now-empty herbalist tables
        gui.msg("Filling herbalist tables with eggs.");
        if (totalEggsNeeded > 0) {
            context.addInItem(eggs, null);
            new FillContainers2(htableContainers, eggs, context, QualityType.High).run(gui);
        }

        NContext freshContext = new NContext(gui);
        // Step 7: Clean up any remaining items in inventory
        gui.msg("Free up inventory.");
        new FreeInventory2(freshContext).run(gui);

        // Step 8: Rearrange silkmoths in the breeding containers to maximize pairs.
        // 1 OPEN ALL SILKMOTH BREEDING CONTAINERS
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
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet())));
        for (Gob gob : gobs) {
            Container cand = new Container(gob, contcaps.get(gob.ngob.name));
            cand.initattr(Container.Space.class);

            // Set known space values for herbalist tables (4x4 = 16 slots)
            // Assuming they start empty for egg filling - could be refined later
            Container.Space space = cand.getattr(Container.Space.class);
            space.getRes().put(Container.Space.MAXSPACE, 16);
            space.getRes().put(Container.Space.FREESPACE, 16);

            containers.add(cand);
        }
        return containers;
    }
    
}

