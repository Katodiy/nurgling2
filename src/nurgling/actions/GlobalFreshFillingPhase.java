package nurgling.actions;

import haven.Widget;
import haven.WItem;
import haven.res.ui.stackinv.ItemStack;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tasks.ISRemoved;
import nurgling.tasks.NTask;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GlobalFreshFillingPhase implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (true) {
            NContext context = new NContext(gui);
            context.getSpecArea(Specialisation.SpecName.picklingJars);

            int availableSpace = countAvailableJarSpace(gui);
            if (availableSpace == 0) break;

            if (!collectBeetroots(gui)) break;

            fillJars(gui);
        }

        new FreeInventory2(new NContext(gui)).run(gui);
        return Results.SUCCESS();
    }

    private boolean collectBeetroots(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        context.addInItem("Beetroot", null);

        int maxItems = gui.getInventory().getNumberFreeCoord(haven.Coord.of(1, 1));
        if (maxItems <= 0) return false;

        Results result = new TakeItems2(context, "Beetroot", maxItems).run(gui);
        if (!result.isSuccess) return false;

        context.getSpecArea(Specialisation.SpecName.picklingJars);
        return gui.getInventory().getItems(new NAlias("Beetroot")).size() > 0;
    }

    private void fillJars(NGameUI gui) throws InterruptedException {
        for (Container container : findAllContainers()) {
            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory == null) continue;

            ArrayList<WItem> jars = inventory.getItems(new NAlias("Pickling Jar"));
            for (WItem jar : jars) {
                if (getBrineLevel(jar) > 1.5) continue;
                fillSingleJar(gui, jar);
                if (countBeetroots(gui) == 0) break;
            }

            new CloseTargetContainer(container).run(gui);
        }
    }

    private void fillSingleJar(NGameUI gui, WItem jar) throws InterruptedException {
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return jar.item.contents != null;
            }
        });

        if (jar.item.contents == null) return;

        NInventory jarInventory = (NInventory) jar.item.contents;
        int availableSpace = jarInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
        if (availableSpace <= 0) return;

        for (int i = 0; i < availableSpace; i++) {
            ArrayList<WItem> beetroots = findBeetroots(gui.getInventory());
            if (beetroots.isEmpty()) break;

            WItem beetroot = beetroots.get(0);
            if (beetroot.parent instanceof ItemStack) {
                ((ItemStack) beetroot.parent).wdgmsg("invxf", jarInventory.wdgid(), 1);
            } else {
                beetroot.item.wdgmsg("invxf", jarInventory.wdgid(), 1);
            }

            NUtils.addTask(new ISRemoved(beetroot.item.wdgid()));
        }
    }

    private int countAvailableJarSpace(NGameUI gui) throws InterruptedException {
        int totalSpace = 0;
        for (Container container : findAllContainers()) {
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
                            totalSpace += ((NInventory) jar.item.contents).getNumberFreeCoord(haven.Coord.of(1, 1));
                        }
                    }
                }
            }
            new CloseTargetContainer(container).run(gui);
        }
        return totalSpace;
    }

    private ArrayList<WItem> findBeetroots(NInventory inventory) {
        ArrayList<WItem> beetroots = new ArrayList<>();
        Widget widget = inventory.child;
        while (widget != null) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                if (isBeetroot(item)) {
                    beetroots.add(item);
                }
            }
            widget = widget.next;
        }
        return beetroots;
    }

    private boolean isBeetroot(WItem item) {
        if (item == null || item.item == null) return false;
        NGItem ngItem = (NGItem) item.item;
        String name = ngItem.name();
        String resource = ngItem.res != null ? ngItem.res.get().name : null;
        return (name != null && name.toLowerCase().contains("beetroot")) ||
               (resource != null && resource.contains("gfx/invobjs/beet"));
    }

    private int countBeetroots(NGameUI gui) {
        return findBeetroots(gui.getInventory()).size();
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

    private ArrayList<Container> findAllContainers() {
        ArrayList<Container> containers = new ArrayList<>();
        for (String resource : NContext.contcaps.keySet()) {
            String type = NContext.contcaps.get(resource);
            for (haven.Gob gob : Finder.findGobs(new NAlias(resource))) {
                containers.add(new Container(gob, type));
            }
        }
        return containers;
    }
}