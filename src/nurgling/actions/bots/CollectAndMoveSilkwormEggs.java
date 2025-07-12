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

public class CollectAndMoveSilkwormEggs implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);

        NAlias eggAlias = new NAlias("Silkworm Egg");

        NArea.Specialisation breedingArea = new NArea.Specialisation(Specialisation.SpecName.silkmothBreeding.toString());
        NArea eggPutArea = NContext.findOut(eggAlias, 1);

        if(eggPutArea == null) {
            return Results.ERROR("PUT Area for Silkworm Egg required, but not found!");
        }

        ArrayList<Container> breedingContainers = new ArrayList<>();
        ArrayList<Container> eggContainers = new ArrayList<>();

        for (Gob sm : Finder.findGobs(NContext.findSpec(breedingArea).getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            breedingContainers.add(cand);
        }

        // Find all egg-put cabinets (assumed "egg_put" NArea)
        for (Gob sm : Finder.findGobs(eggPutArea.getRCArea(), new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
            Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
            cand.initattr(Container.Space.class);
            eggContainers.add(cand);
        }

        // Sanity check
        if (breedingContainers.isEmpty() || eggContainers.isEmpty()) {
            return Results.ERROR("No cabinets found in required NAareas.");
        }

        // Go through breeding cabinets and collect silkworm eggs
        for (Container breedingCabinet : breedingContainers) {
            new PathFinder(Finder.findGob(breedingCabinet.gobid)).run(gui);

            new OpenTargetContainer(breedingCabinet).run(gui);
            ArrayList<WItem> eggs = gui.getInventory(breedingCabinet.cap).getItems(eggAlias);

            if (eggs.isEmpty()) {
                new CloseTargetContainer(breedingCabinet).run(gui);
                continue;
            }

            while (!eggs.isEmpty()) {
                new PathFinder(Finder.findGob(breedingCabinet.gobid)).run(gui);

                new OpenTargetContainer(breedingCabinet).run(gui);

                eggs = gui.getInventory(breedingCabinet.cap).getItems(eggAlias);

                if (eggs.isEmpty()) {
                    new CloseTargetContainer(breedingCabinet).run(gui);
                    continue;
                }

                int fetchCount = Math.min(eggs.size(), gui.getInventory().getFreeSpace());
                // 4. Fetch all eggs from container
                new TakeAvailableItemsFromContainer(breedingCabinet, eggAlias, fetchCount).run(gui);

                new CloseTargetContainer(breedingCabinet).run(gui);

                new FreeInventory2(context).run(gui);
            }
        }

        return Results.SUCCESS();
    }
}
