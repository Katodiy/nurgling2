package nurgling.actions.bots.pickling;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.NTask;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GlobalExtractionPhase implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (true) {
            NContext context = new NContext(gui);
            nurgling.areas.NArea jarArea = context.getSpecArea(Specialisation.SpecName.picklingJars);
            if (jarArea == null) return Results.FAIL();

            int extractableItems = countExtractableItems(gui, jarArea);
            if (extractableItems == 0) break;

            extractFromJars(gui, jarArea);

            if (isInventoryFull(gui)) {
                new FreeInventory2(new NContext(gui)).run(gui);
            }
        }

        new FreeInventory2(new NContext(gui)).run(gui);
        return Results.SUCCESS();
    }

    private void extractFromJars(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        for (Container container : findAllContainers(jarArea)) {
            new OpenTargetContainer(container).run(gui);

            boolean containerNeedsReprocessing;
            do {
                containerNeedsReprocessing = false;
                NInventory inventory = gui.getInventory(container.cap);
                if (inventory == null) break;

                ArrayList<WItem> jars = inventory.getItems(new NAlias("Pickling Jar"));
                for (WItem jar : jars) {
                    extractFromSingleJar(gui, jar);
                    if (isInventoryFull(gui)) {
                        new CloseTargetContainer(container).run(gui);
                        new FreeInventory2(new NContext(gui)).run(gui);
                        // Navigate back to jar area after dropping off items
                        NContext context = new NContext(gui);
                        context.getSpecArea(Specialisation.SpecName.picklingJars);
                        new OpenTargetContainer(container).run(gui);

                        containerNeedsReprocessing = true;
                        break; // Break out of jar loop to refetch jar list
                    }
                }
            } while (containerNeedsReprocessing);

            new CloseTargetContainer(container).run(gui);
        }
    }

    private void extractFromSingleJar(NGameUI gui, WItem jar) throws InterruptedException {
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return jar.item.contents != null;
            }
        });

        if (jar.item.contents == null) return;

        NInventory jarInventory = (NInventory) jar.item.contents;
        ArrayList<WItem> beetroots = jarInventory.getItems(new NAlias("Beetroot"));

        for (WItem beetroot : beetroots) {
            if (isInventoryFull(gui)) break;

            beetroot.item.wdgmsg("transfer", haven.Coord.z);
            NUtils.addTask(new ISRemoved(beetroot.item.wdgid()));
        }
    }

    private int countExtractableItems(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        int totalItems = 0;
        for (Container container : findAllContainers(jarArea)) {
            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory != null) {
                for (WItem jar : inventory.getItems(new NAlias("Pickling Jar"))) {
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return jar.item.contents != null;
                        }
                    });
                    if (jar.item.contents != null) {
                        totalItems += ((NInventory) jar.item.contents).getItems(new NAlias("Beetroot")).size();
                    }
                }
            }
            new CloseTargetContainer(container).run(gui);
        }
        return totalItems;
    }


    private boolean isInventoryFull(NGameUI gui) throws InterruptedException {
        return gui.getInventory().getNumberFreeCoord(haven.Coord.of(1, 1)) <= 2;
    }

    private ArrayList<Container> findAllContainers(nurgling.areas.NArea jarArea) throws InterruptedException {
        ArrayList<Container> containers = new ArrayList<>();
        for (String resource : NContext.contcaps.keySet()) {
            String type = NContext.contcaps.get(resource);
            for (haven.Gob gob : Finder.findGobs(jarArea, new NAlias(resource))) {
                containers.add(new Container(gob, type));
            }
        }
        return containers;
    }
}