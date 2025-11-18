package nurgling.actions.bots.pickling;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.CloseTargetContainer;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.Results;
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

            // Find a barrel with pickling brine (FillWaterskins pattern)
            haven.Gob validBarrel = null;
            for (haven.Gob barrel : barrels) {
                if (hasPicklingBrine(barrel)) {
                    validBarrel = barrel;
                    break;
                }
            }

            // If no valid barrel found, skip filling but continue (graceful degradation)
            if (validBarrel != null) {
                fillJarsAtBarrel(gui, barrels, validBarrel);
            }

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

    private void fillJarsAtBarrel(NGameUI gui, ArrayList<haven.Gob> barrels, haven.Gob currentBarrel) throws InterruptedException {
        new nurgling.actions.PathFinder(currentBarrel).run(gui);

        NInventory playerInventory = gui.getInventory();
        ArrayList<WItem> jars = playerInventory.getItems(new NAlias("Pickling Jar"));

        haven.Gob activeBarrel = currentBarrel;
        for (WItem jar : jars) {
            if (getBrineLevel(jar) >= 1.0) continue;

            // Check if current barrel still has brine before each jar (FillWaterskins pattern)
            if (!hasPicklingBrine(activeBarrel)) {
                // Try to find another barrel with brine
                haven.Gob nextBarrel = null;
                for (haven.Gob barrel : barrels) {
                    if (barrel != activeBarrel && hasPicklingBrine(barrel)) {
                        nextBarrel = barrel;
                        break;
                    }
                }

                if (nextBarrel == null) {
                    // No more barrels with brine, stop filling (graceful degradation)
                    break;
                } else {
                    // Switch to new barrel
                    activeBarrel = nextBarrel;
                    new nurgling.actions.PathFinder(activeBarrel).run(gui);
                }
            }

            double originalBrineLevel = getBrineLevel(jar);
            if (!fillSingleJar(gui, activeBarrel, originalBrineLevel)) {
                // Barrel ran out during this jar, mark it as empty for next iteration
                activeBarrel = null;
            }
        }
    }

    private boolean fillSingleJar(NGameUI gui, haven.Gob barrel, double originalBrineLevel) throws InterruptedException {
        // Check barrel has brine before attempting fill (FillWaterskins pattern)
        if (!hasPicklingBrine(barrel)) {
            return false; // Barrel empty, graceful failure
        }

        NUtils.takeItemToHand(gui.getInventory().getItems(new NAlias("Pickling Jar")).get(0));
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.vhand != null;
            }
        });

        NUtils.activateItem(barrel);

        // Custom wait for brine level change with barrel depletion detection
        boolean[] barrelDepleted = {false};
        NUtils.addTask(new NTask() {
            int attempts = 0;
            @Override
            public boolean check() {
                attempts++;
                if (attempts > 100) return true; // Safety timeout

                WItem jarInHand = gui.vhand;
                if (jarInHand == null) return false;

                double currentBrineLevel = getBrineLevel(jarInHand);

                // If brine increased, success
                if (currentBrineLevel > originalBrineLevel) {
                    return true;
                }

                // If jar is now empty and barrel is also empty, barrel depleted
                if (currentBrineLevel == 0.0 && originalBrineLevel > 0.0) {
                    if (!hasPicklingBrine(barrel)) {
                        barrelDepleted[0] = true;
                        return true; // Stop trying, barrel is empty
                    }
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

        return !barrelDepleted[0]; // Return false if barrel depleted during filling
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

    /**
     * Check if barrel contains pickling brine (FillWaterskins pattern)
     */
    private boolean hasPicklingBrine(haven.Gob barrel) {
        if (!NUtils.barrelHasContent(barrel)) {
            return false;
        }
        String contents = NUtils.getContentsOfBarrel(barrel);
        return contents != null && contents.toLowerCase().contains("picklebrine");
    }
}