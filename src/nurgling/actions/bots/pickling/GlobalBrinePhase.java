package nurgling.actions.bots.pickling;

import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NInventory;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.NTask;
import nurgling.tasks.ISRemoved;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class GlobalBrinePhase implements Action {

    private final PicklingBot.VegetableConfig vegetableConfig;

    public GlobalBrinePhase(PicklingBot.VegetableConfig vegetableConfig) {
        this.vegetableConfig = vegetableConfig;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean workDone = false;

        while (true) {
            NContext context = new NContext(gui);
            nurgling.areas.NArea jarArea = context.getSpecArea(Specialisation.SpecName.picklingJars, vegetableConfig.subSpec);
            if (jarArea == null) return Results.FAIL();

            if (!collectJarsNeedingBrine(gui, jarArea)) break;

            nurgling.areas.NArea barrelArea = context.getSpecArea(Specialisation.SpecName.barrel, "Pickling Brine");
            if (barrelArea == null) return Results.FAIL();

            ArrayList<Gob> barrels = Finder.findGobs(barrelArea, new NAlias("barrel"));
            if (barrels.isEmpty()) return Results.FAIL();

            // Find a barrel with pickling brine (FillWaterskins pattern)
            Gob validBarrel = null;
            for (Gob barrel : barrels) {
                if (hasPicklingBrine(barrel)) {
                    validBarrel = barrel;
                    break;
                }
            }

            // If no valid barrel found, skip filling but continue (graceful degradation)
            if (validBarrel != null) {
                if (fillJarsAtBarrel(gui, barrels, validBarrel)) {
                    workDone = true;
                }
            }

            context.getSpecArea(Specialisation.SpecName.picklingJars, vegetableConfig.subSpec);
            returnJarsToContainers(gui, jarArea);
        }
        return workDone ? Results.SUCCESS() : Results.FAIL();
    }

    private boolean collectJarsNeedingBrine(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        NInventory playerInventory = gui.getInventory();
        int maxJars = playerInventory.getNumberFreeCoord(haven.Coord.of(1, 1));
        int jarsCollected = 0;

        for (Container container : findAllContainers(jarArea)) {
            if (jarsCollected >= maxJars) break;

            new PathFinder(Finder.findGob(container.gobid)).run(gui);
            new OpenTargetContainer(container).run(gui);
            NInventory inventory = gui.getInventory(container.cap);
            if (inventory == null) continue;

            for (WItem jar : inventory.getItems(new NAlias("Pickling Jar"))) {
                if (jarsCollected >= maxJars) break;
                if (getBrineLevel(jar) < 1.0 && hasVegetables(jar)) {
                    jar.item.wdgmsg("transfer", haven.Coord.z);
                    NUtils.addTask(new ISRemoved(jar.item.wdgid()));
                    jarsCollected++;
                }
            }

            new CloseTargetContainer(container).run(gui);
        }

        return jarsCollected > 0;
    }

    private boolean fillJarsAtBarrel(NGameUI gui, ArrayList<Gob> barrels, Gob currentBarrel) throws InterruptedException {
        new nurgling.actions.PathFinder(currentBarrel).run(gui);

        NInventory playerInventory = gui.getInventory();
        ArrayList<WItem> jars = playerInventory.getItems(new NAlias("Pickling Jar"));

        boolean anyJarsFilled = false;
        Gob activeBarrel = currentBarrel;
        for (WItem jar : jars) {
            if (getBrineLevel(jar) >= 1.0) continue;

            // Check if current barrel still has brine before each jar (FillWaterskins pattern)
            if (!hasPicklingBrine(activeBarrel)) {
                // Try to find another barrel with brine
                Gob nextBarrel = null;
                for (Gob barrel : barrels) {
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
            if (fillSingleJar(gui, activeBarrel, originalBrineLevel)) {
                anyJarsFilled = true;
            } else {
                // Barrel ran out during this jar, mark it as empty for next iteration
                activeBarrel = null;
            }
        }

        return anyJarsFilled;
    }

    private boolean fillSingleJar(NGameUI gui, Gob barrel, double originalBrineLevel) throws InterruptedException {
        System.out.println("[PICKLING] Starting fillSingleJar - Original brine level: " + originalBrineLevel);

        // Check barrel has brine before attempting fill (FillWaterskins pattern)
        if (!hasPicklingBrine(barrel)) {
            System.out.println("[PICKLING] Barrel has no pickling brine, aborting");
            return false; // Barrel empty, graceful failure
        }

        new PathFinder(barrel).run(gui);
        NUtils.takeItemToHand(gui.getInventory().getItems(new NAlias("Pickling Jar")).get(0));
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return gui.vhand != null;
            }
        });

        System.out.println("[PICKLING] Clicking barrel to interact with jar...");
        NUtils.activateItem(barrel);

        // Adaptive approach: Wait to see what happens after first click
        String[] interactionResult = {""};
        boolean[] needSecondClick = {false};

        NUtils.addTask(new NTask() {
            int attempts = 0;
            @Override
            public boolean check() {
                attempts++;
                if (attempts > 100) {
                    System.out.println("[PICKLING] First interaction timeout after 100 attempts");
                    return true;
                }

                WItem jarInHand = gui.vhand;
                if (jarInHand == null) return false;

                double currentLevel = getBrineLevel(jarInHand);
                System.out.println("[PICKLING] First click attempt " + attempts + " - Level: " + currentLevel + " (was " + originalBrineLevel + ")");

                // Check if jar was filled (level increased)
                if (currentLevel > originalBrineLevel) {
                    interactionResult[0] = "FILLED";
                    System.out.println("[PICKLING] ✅ Jar was filled on first click! " + originalBrineLevel + "L → " + currentLevel + "L (barrel was full)");
                    return true;
                }

                // Check if jar was emptied (level decreased to 0)
                if (currentLevel == 0.0 && originalBrineLevel > 0.0) {
                    interactionResult[0] = "EMPTIED";
                    needSecondClick[0] = true;
                    System.out.println("[PICKLING] ✅ Jar was emptied on first click! " + originalBrineLevel + "L → 0.0L (barrel had room)");
                    return true;
                }

                // For empty jars, wait for fill
                if (originalBrineLevel == 0.0 && currentLevel >= 1.0) {
                    interactionResult[0] = "FILLED";
                    System.out.println("[PICKLING] ✅ Empty jar was filled! 0.0L → " + currentLevel + "L");
                    return true;
                }

                return false;
            }
        });

        // If jar was emptied, we need a second click to fill it
        if (needSecondClick[0]) {
            System.out.println("[PICKLING] Second click needed - filling emptied jar...");
            NUtils.activateItem(barrel);

            NUtils.addTask(new NTask() {
                int attempts = 0;
                @Override
                public boolean check() {
                    attempts++;
                    if (attempts > 100) {
                        System.out.println("[PICKLING] Second interaction timeout after 100 attempts");
                        return true;
                    }

                    WItem jarInHand = gui.vhand;
                    if (jarInHand == null) return false;

                    double currentLevel = getBrineLevel(jarInHand);
                    System.out.println("[PICKLING] Second click attempt " + attempts + " - Level: " + currentLevel);

                    // Wait for jar to be filled to 1.0L
                    if (currentLevel >= 1.0) {
                        System.out.println("[PICKLING] ✅ Jar filled on second click! 0.0L → " + currentLevel + "L");
                        return true;
                    }

                    return false;
                }
            });
        }

        // Put jar back to inventory and determine success
        WItem jarInHand = gui.vhand;
        if (jarInHand != null) {
            double finalLevel = getBrineLevel(jarInHand);
            System.out.println("[PICKLING] Final result: " + originalBrineLevel + "L → " + finalLevel + "L (" + interactionResult[0] + (needSecondClick[0] ? " + REFILLED)" : ")"));

            jarInHand.item.wdgmsg("transfer", haven.Coord.z);
            NUtils.addTask(new ISRemoved(jarInHand.item.wdgid()));

            boolean success = finalLevel >= 1.0;
            System.out.println("[PICKLING] fillSingleJar result: " + (success ? "SUCCESS" : "FAILED") + " - final level " + finalLevel + "L");
            return success;
        }
        return false;
    }

    private void returnJarsToContainers(NGameUI gui, nurgling.areas.NArea jarArea) throws InterruptedException {
        NInventory playerInventory = gui.getInventory();
        ArrayList<WItem> jars = playerInventory.getItems(new NAlias("Pickling Jar"));

        for (Container container : findAllContainers(jarArea)) {
            if (jars.isEmpty()) break;

            new PathFinder(Finder.findGob(container.gobid)).run(gui);
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
            for (Gob gob : Finder.findGobs(jarArea, new NAlias(resource))) {
                containers.add(new Container(gob, type));
            }
        }
        return containers;
    }

    /**
     * Check if barrel contains pickling brine (FillWaterskins pattern)
     */
    private boolean hasPicklingBrine(Gob barrel) {
        if (!NUtils.barrelHasContent(barrel)) {
            return false;
        }
        String contents = NUtils.getContentsOfBarrel(barrel);
        return contents != null && contents.toLowerCase().contains("picklebrine");
    }

    /**
     * Check if jar contains any vegetables
     */
    private boolean hasVegetables(WItem jar) throws InterruptedException {
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return jar.item.contents != null;
            }
        });

        if (jar.item.contents == null) return false;

        NInventory jarInventory = (NInventory) jar.item.contents;
        return !jarInventory.getItems(new NAlias(vegetableConfig.freshAlias)).isEmpty();
    }
}