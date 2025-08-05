package nurgling.actions;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.List;

public class DepositItemsToSpecArea implements Action {
    private final NContext context;
    private final String item; // item name (e.g. "Mulberry Leaf")
    private final Specialisation.SpecName specArea;
    private final int maxPerContainer; // e.g. 32

    public DepositItemsToSpecArea(NContext context, String item, Specialisation.SpecName specArea, int maxPerContainer) {
        this.context = context;
        this.item = item;
        this.specArea = specArea;
        this.maxPerContainer = maxPerContainer;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NAlias itemAlias = new NAlias(item);

        // Get the destination area
        NArea area = context.getSpecArea(specArea);
        if (area == null) return Results.ERROR("Destination spec area not found!");

        // Get all containers in this area (cupboards, troughs, etc)
        ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias(new ArrayList<>(Context.contcaps.keySet())));
        ArrayList<Container> containers = new ArrayList<>();
        for (Gob gob : gobs) {
            Container c = new Container(gob, Context.contcaps.get(gob.ngob.name));
            c.initattr(Container.Space.class);
            containers.add(c);
        }
        if (containers.isEmpty()) return Results.ERROR("No containers in target area!");

        // Loop until all containers are at max or we run out of source items
        while (true) {
            // Step 1: Calculate how many more we need in total
            int totalNeeded = 0;
            List<Integer> needs = new ArrayList<>(containers.size());
            for (Container cont : containers) {
                new PathFinder(Finder.findGob(cont.gobid)).run(gui);
                new OpenTargetContainer(cont).run(gui);
                int current = gui.getInventory(cont.cap).getItems(itemAlias).size();
                new CloseTargetContainer(cont).run(gui);
                int need = Math.max(0, maxPerContainer - current);
                needs.add(need);
                totalNeeded += need;
            }
            if (totalNeeded == 0) break; // All full

            // Step 2: Fetch as much as possible from source
            int invSpace = gui.getInventory().getFreeSpace();
            int fetch = Math.min(invSpace, totalNeeded);
            context.addInItem(item, null);
            new TakeItems2(context, item, fetch).run(gui);
            int inInv = gui.getInventory().getItems(itemAlias).size();
            if (inInv == 0) break; // No more source

            // Step 3: Deposit into containers as needed
            context.getSpecArea(specArea);

            for (int i = 0; i < containers.size(); i++) {
                Container cont = containers.get(i);
                int need = needs.get(i); // maxPerContainer - current, already calculated above
                int itemsInInventory = gui.getInventory().getItems(itemAlias).size();

                if (need > 0 && itemsInInventory > 0) {
                    int toDeposit = Math.min(need, itemsInInventory);

                    System.out.printf("[Deposit] Checking container %d (GobID: %d)\n", i, cont.gobid);
                    System.out.printf("[Deposit] This container needs %d more items.\n", need);
                    System.out.printf("[Deposit] Items available in inventory: %d\n", itemsInInventory);

                    new PathFinder(Finder.findGob(cont.gobid)).run(gui);
                    new OpenTargetContainer(cont).run(gui);

                    int currentCount = gui.getInventory(cont.cap).getItems(itemAlias).size();
                    System.out.printf("[Deposit] Current item count in container: %d\n", currentCount);
                    System.out.printf("[Deposit] Will deposit up to: %d items\n", toDeposit);

                    new TransferToContainer(cont, itemAlias).run(gui);

                    System.out.println("[Deposit] Done with this container, closing...");
                    new CloseTargetContainer(cont).run(gui);
                } else {
                    System.out.printf("[Deposit] Skipping container (need: %d, in inv: %d)\n", need, itemsInInventory);
                }
            }

            // Safety: If we couldn't deposit anything, prevent infinite loop
            if (gui.getInventory().getItems(itemAlias).size() == inInv)
                break;
        }

        return Results.SUCCESS();
    }
}
