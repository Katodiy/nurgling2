package nurgling.actions.bots;

import haven.Gob;
import haven.ResDrawable;
import haven.UI;
import nurgling.*;
import nurgling.actions.*;
import nurgling.conf.NAreaRad;
import nurgling.conf.NBoughBeeProp;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NAlarmWdg;
import nurgling.widgets.bots.Checkable;

import java.util.ArrayList;

public class BoughBee implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.BoughBee w = null;
        NBoughBeeProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable(NUtils.getGameUI().add((w = new nurgling.widgets.bots.BoughBee()), UI.scale(200, 200))));
            prop = w.prop;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            if (w != null)
                w.destroy();
        }
        if (prop == null) {
            return Results.ERROR("No config");
        }

        // Get dangerous animal patterns from NConfig
        @SuppressWarnings("unchecked")
        ArrayList<NAreaRad> animalRads = (ArrayList<NAreaRad>) NConfig.get(NConfig.Key.animalrad);
        ArrayList<String> dangerousAnimals = new ArrayList<>();
        if (animalRads != null) {
            for (NAreaRad rad : animalRads) {
                dangerousAnimals.add(rad.name);
            }
        }

        final NGameUI finalGui = gui;
        final NBoughBeeProp finalProp = prop;
        
        while (true) {
            // Find bpyre (bonfire/smoke)
            Gob bpyre = Finder.findGob(new NAlias("bpyre"));
            if (bpyre == null) {
                return Results.ERROR("No bpyre found");
            }

            // Find beehive near bpyre
            Gob beehive = Finder.findGob(bpyre.rc, new NAlias("wildbees/wildbeehive"), null, 50.0);
            if (beehive == null) {
                return Results.ERROR("No beehive found");
            }

            final Gob finalBpyre = bpyre;
            final Gob finalBeehive = beehive;
            
            // Wait until bpyre disappears and beehive marker becomes 0
            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    try {
                        // Check for dangerous players
                        if (!NAlarmWdg.borkas.isEmpty()) {
                            if (!finalProp.onPlayerAction.equals("nothing")) {
                                performSafetyAction(finalGui, finalProp.onPlayerAction);
                                return true;
                            }
                        }

                        // Check for dangerous animals in radius 200
                        for (String animalPattern : dangerousAnimals) {
                            Gob animal = Finder.findGob(NUtils.player().rc, new NAlias(animalPattern), null, 200.0);
                            if (animal != null) {
                                performSafetyAction(finalGui, finalProp.onAnimalAction);
                                return true;
                            }
                        }

                        // Check if bpyre disappeared
                        Gob currentBpyre = Finder.findGob(finalBpyre.id);
                        if (currentBpyre == null) {
                            // Check if beehive marker is 0
                            Gob currentBeehive = Finder.findGob(finalBeehive.id);
                            if (currentBeehive != null) {
                                ResDrawable rd = currentBeehive.getattr(ResDrawable.class);
                                if (rd != null) {
                                    long marker = rd.calcMarker();
                                    if (marker == 0) {
                                        return true;
                                    }
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        return true;
                    }
                    return false;
                }
            });

            // Check if we exited due to safety action
            if (!NAlarmWdg.borkas.isEmpty() && !finalProp.onPlayerAction.equals("nothing")) {
                return Results.SUCCESS();
            }
            for (String animalPattern : dangerousAnimals) {
                Gob animal = Finder.findGob(NUtils.player().rc, new NAlias(animalPattern), null, 200.0);
                if (animal != null) {
                    return Results.SUCCESS();
                }
            }

            // Check if beehive still exists
            Gob currentBeehive = Finder.findGob(finalBeehive.id);
            if (currentBeehive == null) {
                // Beehive disappeared (might have been collected), perform after harvest action
                performSafetyAction(finalGui, finalProp.afterHarvestAction);
                return Results.SUCCESS();
            }

            // Raid the beehive
            new PathFinder(currentBeehive).run(finalGui);
            new SelectFlowerAction("Raid!", currentBeehive).run(finalGui);

            // Wait for beehive to disappear
            NUtils.getUI().core.addTask(new WaitGobRemoval(finalBeehive.id));

            // Perform after harvest action
            performSafetyAction(finalGui, finalProp.afterHarvestAction);
            return Results.SUCCESS();
        }
    }

    private void performSafetyAction(NGameUI gui, String action) throws InterruptedException {
        switch (action) {
            case "logout":
                gui.act("lo");
                break;
            case "travel hearth":
                gui.act("travel", "hearth");
                break;
            case "nothing":
            default:
                // Do nothing
                break;
        }
    }
}
