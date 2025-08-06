package nurgling.actions.bots;

import haven.Gob;
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
 * 1. Move hatched silkworms from herbalist tables to feeding cabinets
 * 2. Ensure feeding cabinets have mulberry leaves (32 per cabinet)  
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

        // Step 2: Move hatched silkworms from herbalist tables to feeding cabinets
        while (true) {
            int wormsBefore = gui.getInventory().getItems(new NAlias(worms)).size();
            
            // Take silkworms from herbalist tables
            new TakeItems2(context, worms, gui.getInventory().getFreeSpace(), Specialisation.SpecName.htable).run(gui);
            
            int wormsAfter = gui.getInventory().getItems(new NAlias(worms)).size();
            int wormsCollected = wormsAfter - wormsBefore;
            
            if (wormsCollected > 0) {
                // Move worms to feeding cabinets using existing transfer logic
                HashSet<String> wormsSet = new HashSet<>(Collections.singleton(worms));
                new TransferItems2(context, wormsSet, Specialisation.SpecName.silkwormFeeding).run(gui);
            } else {
                break;
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
