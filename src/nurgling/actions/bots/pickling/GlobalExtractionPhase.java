package nurgling.actions.bots.pickling;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.NTask;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GlobalExtractionPhase implements Action {

    private final NArea jarArea;
    private final PicklingBot.VegetableConfig vegetableConfig;

    public GlobalExtractionPhase(NArea jarArea, PicklingBot.VegetableConfig vegetableConfig) {
        this.jarArea = jarArea;
        this.vegetableConfig = vegetableConfig;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean workDone = false;

        while (true) {
            NContext context = new NContext(gui);
            nurgling.areas.NArea jarArea = context.getSpecArea(Specialisation.SpecName.picklingJars, vegetableConfig.subSpec);
            if (jarArea == null) return Results.FAIL();

            int extractableItems = countExtractableItems(gui, jarArea);
            if (extractableItems == 0) break;

            if (extractFromJars(gui, jarArea)) {
                workDone = true;
            }

            if (isInventoryFull(gui)) {
                new FreeInventory2(new NContext(gui)).run(gui);
            }
        }

        new FreeInventory2(new NContext(gui)).run(gui);
        return workDone ? Results.SUCCESS() : Results.FAIL();
    }

    private boolean extractFromJars(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        boolean anyItemsExtracted = false;

        for (Container container : findAllContainers(jarArea)) {
            new OpenTargetContainer(container).run(gui);

            boolean containerNeedsReprocessing;
            do {
                containerNeedsReprocessing = false;
                NInventory inventory = gui.getInventory(container.cap);
                if (inventory == null) break;

                ArrayList<WItem> jars = inventory.getItems(new NAlias("Pickling Jar"));
                for (WItem jar : jars) {
                    if (extractFromSingleJar(gui, jar)) {
                        anyItemsExtracted = true;
                    }
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

        return anyItemsExtracted;
    }

    private boolean extractFromSingleJar(NGameUI gui, WItem jar) throws InterruptedException {
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return jar.item.contents != null;
            }
        });

        if (jar.item.contents == null) return false;

        NInventory jarInventory = (NInventory) jar.item.contents;
        ArrayList<WItem> pickledItems = jarInventory.getItems(new NAlias(vegetableConfig.pickledAlias));

        boolean itemsExtracted = false;
        for (WItem pickledItem : pickledItems) {
            if (isInventoryFull(gui)) break;

            pickledItem.item.wdgmsg("transfer", haven.Coord.z);
            NUtils.addTask(new ISRemoved(pickledItem.item.wdgid()));
            itemsExtracted = true;
        }

        return itemsExtracted;
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
                        totalItems += ((NInventory) jar.item.contents).getItems(new NAlias(vegetableConfig.pickledAlias)).size();
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