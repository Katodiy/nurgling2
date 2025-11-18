package nurgling.actions.bots.pickling;

import haven.WItem;
import haven.res.ui.stackinv.ItemStack;
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

public class GlobalFreshFillingPhase implements Action {

    private final PicklingBot.VegetableConfig vegetableConfig;

    public GlobalFreshFillingPhase(PicklingBot.VegetableConfig vegetableConfig) {
        this.vegetableConfig = vegetableConfig;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean workDone = false;

        while (true) {
            NContext context = new NContext(gui);
            nurgling.areas.NArea jarArea = context.getSpecArea(Specialisation.SpecName.picklingJars, vegetableConfig.subSpec);
            if (jarArea == null) return Results.FAIL();

            int availableSpace = countAvailableJarSpace(gui, jarArea);
            if (availableSpace == 0) break;

            if (!collectVegetables(gui)) break;

            if (fillJars(gui, jarArea)) {
                workDone = true;
            }
        }

        new FreeInventory2(new NContext(gui)).run(gui);
        return workDone ? Results.SUCCESS() : Results.FAIL();
    }

    private boolean collectVegetables(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        context.addInItem(vegetableConfig.freshAlias, null);

        int maxItems = gui.getInventory().getNumberFreeCoord(vegetableConfig.itemSize);
        if (maxItems <= 0) return false;

        Results result = new TakeItems2(context, vegetableConfig.freshAlias, maxItems).run(gui);
        if (!result.isSuccess) return false;

        context.getSpecArea(Specialisation.SpecName.picklingJars, vegetableConfig.subSpec);
        return !gui.getInventory().getItems(new NAlias(vegetableConfig.freshAlias)).isEmpty();
    }

    private boolean fillJars(NGameUI gui, NArea jarArea) throws InterruptedException {
        boolean anyJarsFilled = false;

        for (Container container : findAllContainers(jarArea)) {
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory == null) continue;

            ArrayList<WItem> jars = inventory.getItems(new NAlias("Pickling Jar"));
            for (WItem jar : jars) {
                if (getBrineLevel(jar) > 1.5) continue;
                if (fillSingleJar(gui, jar)) {
                    anyJarsFilled = true;
                }
                if (countVegetables(gui) == 0) break;
            }

            new CloseTargetContainer(container).run(gui);
        }

        return anyJarsFilled;
    }

    private boolean fillSingleJar(NGameUI gui, WItem jar) throws InterruptedException {
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return jar.item.contents != null;
            }
        });

        if (jar.item.contents == null) return false;

        NInventory jarInventory = (NInventory) jar.item.contents;
        int availableSpace = jarInventory.getNumberFreeCoord(vegetableConfig.itemSize);
        if (availableSpace <= 0) return false;

        boolean itemsAdded = false;
        for (int i = 0; i < availableSpace; i++) {
            ArrayList<WItem> vegetables = findVegetables(gui.getInventory());
            if (vegetables.isEmpty()) break;

            WItem vegetable = vegetables.get(0);
            if (vegetable.parent instanceof ItemStack) {
                ((ItemStack) vegetable.parent).wdgmsg("invxf", jarInventory.wdgid(), 1);
            } else {
                vegetable.item.wdgmsg("invxf", jarInventory.wdgid(), 1);
            }

            NUtils.addTask(new ISRemoved(vegetable.item.wdgid()));
            itemsAdded = true;
        }

        return itemsAdded;
    }

    private int countAvailableJarSpace(NGameUI gui, NArea jarArea) throws InterruptedException {
        int totalSpace = 0;
        for (Container container : findAllContainers(jarArea)) {
            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory != null) {
                for (WItem jar : inventory.getItems(new NAlias("Pickling Jar"))) {
                    if (getBrineLevel(jar) <= 1.5) {
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return jar.item.contents != null;
                            }
                        });
                        if (jar.item.contents != null) {
                            totalSpace += ((NInventory) jar.item.contents).getNumberFreeCoord(vegetableConfig.itemSize);
                        }
                    }
                }
            }
            new CloseTargetContainer(container).run(gui);
        }
        return totalSpace;
    }

    private ArrayList<WItem> findVegetables(NInventory inventory) throws InterruptedException {
        return inventory.getItems(new NAlias(vegetableConfig.freshAlias));
    }

    private int countVegetables(NGameUI gui) throws InterruptedException {
        return findVegetables(gui.getInventory()).size();
    }

    private double getBrineLevel(WItem jarItem) {
        NGItem ngItem = (NGItem) jarItem.item;
        for (NGItem.NContent content : ngItem.content()) {
            String contentName = content.name();
            if (contentName.contains("l of Pickling Brine")) {
                String[] parts = contentName.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("l") && i > 0) {
                        return Double.parseDouble(parts[i - 1]);
                    }
                }
            }
        }
        return 0.0;
    }

    private ArrayList<Container> findAllContainers(NArea jarArea) throws InterruptedException {
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