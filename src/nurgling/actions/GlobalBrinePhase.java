package nurgling.actions;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tasks.NTask;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GlobalBrinePhase implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        while (true) {
            NContext context = new NContext(gui);
            nurgling.areas.NArea jarArea = context.getSpecArea(Specialisation.SpecName.picklingJars);
            if (jarArea == null) return Results.FAIL();

            if (!collectJarsNeedingBrine(gui, jarArea)) break;

            nurgling.areas.NArea barrelArea = context.getSpecArea(Specialisation.SpecName.barrel, "Pickling Brine");
            if (barrelArea == null) return Results.FAIL();

            ArrayList<haven.Gob> barrels = Finder.findGobs(barrelArea, new NAlias("barrel"));
            if (barrels.isEmpty()) return Results.FAIL();

            fillJarsAtBarrel(gui, barrels.get(0));

            context.getSpecArea(Specialisation.SpecName.picklingJars);
            returnJarsToContainers(gui, jarArea);
        }
        return Results.SUCCESS();
    }

    private boolean collectJarsNeedingBrine(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        NInventory playerInventory = gui.getInventory();
        int maxJars = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
        int jarsCollected = 0;

        for (Container container : findAllContainers(jarArea)) {
            if (jarsCollected >= maxJars) break;

            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory == null) continue;

            for (WItem jar : inventory.getItems(new NAlias("Pickling Jar"))) {
                if (jarsCollected >= maxJars) break;
                if (getBrineLevel(jar) < 1.0) {
                    jar.item.wdgmsg("transfer", haven.Coord.z);
                    NUtils.addTask(new ISRemoved(jar.item.wdgid()));
                    jarsCollected++;
                }
            }

            new CloseTargetContainer(container).run(gui);
        }

        return jarsCollected > 0;
    }

    private void fillJarsAtBarrel(NGameUI gui, haven.Gob barrel) throws InterruptedException {
        new nurgling.actions.PathFinder(barrel).run(gui);

        NInventory playerInventory = gui.getInventory();
        ArrayList<WItem> jars = playerInventory.getItems(new NAlias("Pickling Jar"));

        for (WItem jar : jars) {
            if (getBrineLevel(jar) >= 1.0) continue;

            double originalBrineLevel = getBrineLevel(jar);
            fillSingleJar(gui, barrel, originalBrineLevel);
        }
    }

    private void fillSingleJar(NGameUI gui, haven.Gob barrel, double originalBrineLevel) throws InterruptedException {
        NUtils.takeItemToHand(gui.getInventory().getItems(new NAlias("Pickling Jar")).get(0));
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.vhand != null;
            }
        });

        NUtils.activateItem(barrel);

        // Custom wait for brine level change
        NUtils.addTask(new NTask() {
            int attempts = 0;
            @Override
            public boolean check() {
                attempts++;
                if (attempts > 100) return true; // Safety timeout

                WItem jarInHand = gui.vhand;
                if (jarInHand == null) return false;

                double currentBrineLevel = getBrineLevel(jarInHand);

                // If brine increased, we're done
                if (currentBrineLevel > originalBrineLevel) {
                    return true;
                }

                // If jar is now empty, try filling again
                if (currentBrineLevel == 0.0 && originalBrineLevel > 0.0) {
                    NUtils.activateItem(barrel);
                    return false; // Continue waiting
                }

                return false;
            }
        });

        // Put jar back to inventory
        WItem jarInHand = gui.vhand;
        if (jarInHand != null) {
            jarInHand.item.wdgmsg("transfer", haven.Coord.z);
            NUtils.addTask(new ISRemoved(jarInHand.item.wdgid()));
        }
    }

    private void returnJarsToContainers(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        NInventory playerInventory = gui.getInventory();
        ArrayList<WItem> jars = playerInventory.getItems(new NAlias("Pickling Jar"));

        for (Container container : findAllContainers(jarArea)) {
            if (jars.isEmpty()) break;

            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory == null) continue;

            int freeSpace = inventory.getNumberFreeCoord(haven.Coord.of(1, 1));
            for (int i = 0; i < Math.min(freeSpace, jars.size()); i++) {
                WItem jar = jars.get(i);
                jar.item.wdgmsg("transfer", haven.Coord.z);
                NUtils.addTask(new ISRemoved(jar.item.wdgid()));
            }

            new CloseTargetContainer(container).run(gui);
            jars = playerInventory.getItems(new NAlias("Pickling Jar"));
        }
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