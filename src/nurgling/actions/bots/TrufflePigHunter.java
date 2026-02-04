package nurgling.actions.bots;

import haven.*;
import haven.res.ui.tt.leashed.Leashed;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.NTrufflePigProp;
import nurgling.routes.ForagerPath;
import nurgling.routes.ForagerSection;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitPose;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Map;

import static nurgling.NUtils.getGameUI;

public class TrufflePigHunter implements Action {

    private static final NAlias PIG_ALIAS = new NAlias(new ArrayList<String>() {{
        add("gfx/kritter/pig/sow");
        add("gfx/kritter/pig/hog");
    }}, new ArrayList<>());

    private static final NAlias TRUFFLE_ALIAS = new NAlias("gfx/terobjs/items/truffle");
    private static final NAlias ROPE_ALIAS = new NAlias("Rope");
    private static final NAlias HITCHING_POST_ALIAS = new NAlias("gfx/terobjs/hitchingpost");

    private String presetName = null;

    public TrufflePigHunter() {
    }

    public TrufflePigHunter(Map<String, Object> settings) {
        if (settings != null && settings.containsKey("presetName")) {
            this.presetName = (String) settings.get("presetName");
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NTrufflePigProp prop = null;
        NTrufflePigProp.PresetData preset = null;

        if (presetName != null) {
            // Scenario mode: load preset directly without UI
            prop = NTrufflePigProp.get(NUtils.getUI().sessInfo);
            if (prop == null) {
                return Results.ERROR("Cannot load truffle pig properties");
            }

            preset = prop.presets.get(presetName);
            if (preset == null) {
                return Results.ERROR("Preset not found: " + presetName);
            }

            if (preset.foragerPath == null && !preset.pathFile.isEmpty()) {
                try {
                    preset.foragerPath = ForagerPath.load(preset.pathFile);
                } catch (Exception e) {
                    return Results.ERROR("Failed to load path: " + e.getMessage());
                }
            }
        } else {
            // Interactive mode: show UI
            nurgling.widgets.bots.TrufflePigHunter w = null;
            try {
                NUtils.getUI().core.addTask(new nurgling.tasks.WaitCheckable(
                    NUtils.getGameUI().add((w = new nurgling.widgets.bots.TrufflePigHunter()), UI.scale(200, 200))
                ));
                prop = w.prop;
            } catch (InterruptedException e) {
                throw e;
            } finally {
                if (w != null)
                    w.destroy();
            }

            if (prop == null) {
                return Results.ERROR("No configuration");
            }

            preset = prop.presets.get(prop.currentPreset);
        }

        if (preset == null || preset.foragerPath == null) {
            return Results.ERROR("No path configured");
        }

        ForagerPath path = preset.foragerPath;

        if (path.getSectionCount() == 0) {
            return Results.ERROR("Path has no sections");
        }

        // Step 1: Find truffle pig area and navigate to it
        NArea trufflePigArea = NContext.findSpec(Specialisation.SpecName.trufflePig.toString());
        if (trufflePigArea == null) {
            return Results.ERROR("No truffle pig area found. Create an area with 'Truffle Pig' specialization.");
        }

        gui.msg("Navigating to truffle pig area...");
        Results navResult = new PathFinder(trufflePigArea.getRCArea().a).run(gui);
        if (!navResult.IsSuccess()) {
            return Results.ERROR("Failed to navigate to truffle pig area");
        }

        // Step 2: Find a pig in the area
        Gob pig = Finder.findGob(trufflePigArea, PIG_ALIAS);
        if (pig == null) {
            return Results.ERROR("No pig found in truffle pig area");
        }

        gui.msg("Found pig, leashing...");

        // Step 3: Leash the pig with rope
        Results leashResult = leashPig(gui, pig);
        if (!leashResult.IsSuccess()) {
            return leashResult;
        }

        long pigId = pig.id;
        gui.msg("Pig leashed. Starting path...");

        // Step 4: Navigate to path start
        MiniMap.Location sessloc = gui.mmap.sessloc;
        if (sessloc == null) {
            return Results.ERROR("Cannot get session location");
        }

        Coord2d startPos = path.waypoints.get(0).toWorldCoord(sessloc);
        if (startPos == null) {
            return Results.ERROR("Cannot get start position - waypoint not in current segment");
        }

        PathFinder pf = new PathFinder(startPos);
        pf.run(gui);

        // Step 5: Walk the path, watching pig behavior for truffle detection
        for (int i = 0; i < path.getSectionCount(); i++) {
            ForagerSection section = path.getSection(i);
            if (section == null) continue;

            Coord2d sectionEnd = section.endPoint;

            // Check pig distance BEFORE moving to next waypoint (nurgling1 pattern)
            Gob currentPig = Finder.findGob(pigId);
            if (currentPig != null) {
                double pigDist = NUtils.player().rc.dist(currentPig.rc);
                if (pigDist >= 77) {
                    gui.msg("Waiting for pig to catch up (distance: " + (int)pigDist + ")...");
                    waitForPigDistance(gui, pigId, 77);
                }
            }

            // Use PathFinder to navigate to section endpoint
            PathFinder sectionPf = new PathFinder(sectionEnd);
            Results pathResult = sectionPf.run(gui);

            if (!pathResult.IsSuccess()) {
                gui.msg("Warning: Path navigation issue, checking for truffles anyway...");
            }

            // After reaching waypoint, watch pig behavior (nurgling1 approach)
            // Check if pig is walking (indicating it found a truffle)
            currentPig = Finder.findGob(pigId);
            if (currentPig != null) {
                String pose = currentPig.pose();
                if (pose != null && pose.contains("walking")) {
                    gui.msg("Pig is searching...");
                    // Wait for pig to go idle (indicating it reached the truffle)
                    NUtils.addTask(new WaitPose(currentPig, "idle"));
                }
            }

            // After pig behavior check, look for truffles
            ArrayList<Gob> truffles = Finder.findGobs(TRUFFLE_ALIAS);
            if (!truffles.isEmpty()) {
                gui.msg("Found " + truffles.size() + " truffle(s)!");

                for (Gob truffle : truffles) {
                    // Check if pig is too close to this truffle (nurgling1 pattern)
                    currentPig = Finder.findGob(pigId);
                    if (currentPig != null) {
                        double pigToTruffleDist = truffle.rc.dist(currentPig.rc);

                        // If pig is very close (< 11), skip this truffle - pig will eat it
                        if (pigToTruffleDist < 11) {
                            gui.msg("Pig too close to truffle, skipping...");
                            continue;
                        }

                        // If pig is moderately close (< 20), pull the rope first
                        if (pigToTruffleDist < 20) {
                            gui.msg("Pulling pig away from truffle...");
                            pullRope(gui);
                            // Wait for pig to move away
                            final Gob truffleRef = truffle;
                            NUtils.addTask(new NTask() {
                                { maxCounter = 50; }
                                @Override
                                public boolean check() {
                                    Gob p = Finder.findGob(pigId);
                                    if (p != null) {
                                        return truffleRef.rc.dist(p.rc) > 20;
                                    }
                                    return counter >= maxCounter;
                                }
                            });
                        }
                    }

                    // Navigate to truffle
                    PathFinder trufflePf = new PathFinder(truffle);
                    Results trufflePathResult = trufflePf.run(gui);

                    if (!trufflePathResult.IsSuccess()) {
                        gui.msg("Warning: Can't reach truffle, skipping...");
                        continue;
                    }

                    // Pick up truffle using takeFromEarth (nurgling1 pattern)
                    NUtils.takeFromEarth(truffle);
                }
            }
        }

        gui.msg("Path complete. Returning to area to tie up pig...");

        // Step 6: Return to truffle pig area
        navResult = new PathFinder(trufflePigArea.getRCArea().a).run(gui);
        if (!navResult.IsSuccess()) {
            return Results.ERROR("Failed to navigate back to truffle pig area");
        }

        // Step 7: Find hitching post and tie rope to it
        Gob hitchingPost = Finder.findGob(trufflePigArea, HITCHING_POST_ALIAS);
        if (hitchingPost != null) {
            gui.msg("Tying pig to hitching post...");
            Results tieResult = tieToHitchingPost(gui, hitchingPost);
            if (!tieResult.IsSuccess()) {
                gui.msg("Warning: Failed to tie to hitching post");
            }
        } else {
            gui.msg("No hitching post found in area");
        }

        gui.msg("Truffle hunting complete!");
        return Results.SUCCESS();
    }

    private Results leashPig(NGameUI gui, Gob pig) throws InterruptedException {
        WItem rope = gui.getInventory().getItem(ROPE_ALIAS);
        if (rope == null) {
            return Results.ERROR("No rope found in inventory");
        }

        // Navigate close to pig first
        while (NUtils.player().rc.dist(pig.rc) > 15) {
            new PathFinder(pig).run(gui);
        }

        // Take rope to hand
        NUtils.takeItemToHand(rope);

        // Wait for rope to be in hand
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return NUtils.getGameUI().vhand != null;
            }
        });

        // Use rope on pig
        NUtils.activateItem(pig, false);

        // Wait for leash to apply - check if rope gets Leashed info
        final boolean[] leashConfirmed = {false};
        NUtils.addTask(new NTask() {
            {
                maxCounter = 100; // ~10 seconds
            }
            @Override
            public boolean check() {
                NGameUI g = NUtils.getGameUI();
                if (g.vhand != null && g.vhand.item != null && g.vhand.item.info() != null) {
                    for (ItemInfo info : g.vhand.item.info()) {
                        if (info instanceof Leashed) {
                            leashConfirmed[0] = true;
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        // Transfer rope back to inventory
        if (gui.vhand != null) {
            NUtils.dropToInv();
        }

        // Wait for rope to be in inventory
        NUtils.addTask(new NTask() {
            {
                maxCounter = 50;
            }
            @Override
            public boolean check() {
                try {
                    return findLeashedRope(NUtils.getGameUI()) != null ||
                           NUtils.getGameUI().getInventory().getItem(ROPE_ALIAS) != null;
                } catch (InterruptedException e) {
                    return true;
                }
            }
        });

        // Final check for leashed rope
        WItem leashedRope = findLeashedRope(gui);
        if (leashedRope != null) {
            leashConfirmed[0] = true;
        }

        if (!leashConfirmed[0]) {
            gui.msg("Warning: Could not confirm leash. Continuing anyway...");
        } else {
            gui.msg("Leash confirmed!");
        }

        return Results.SUCCESS();
    }

    /**
     * Find a rope with Leashed info in inventory
     */
    private WItem findLeashedRope(NGameUI gui) throws InterruptedException {
        for (WItem item : gui.getInventory().getItems(ROPE_ALIAS)) {
            if (item.item != null && item.item.info() != null) {
                for (ItemInfo info : item.item.info()) {
                    if (info instanceof Leashed) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Wait for pig to be within specified distance of player.
     * Used BEFORE moving to next waypoint (nurgling1 pattern).
     */
    private void waitForPigDistance(NGameUI gui, long pigId, double maxDistance) throws InterruptedException {
        NUtils.addTask(new NTask() {
            {
                maxCounter = 1000; // ~100 seconds timeout
            }
            @Override
            public boolean check() {
                Gob pig = Finder.findGob(pigId);
                if (pig != null) {
                    Coord2d playerPos = NUtils.player().rc;
                    double pigDist = playerPos.dist(pig.rc);

                    if (pigDist < maxDistance) {
                        return true;
                    }
                }
                if (counter >= maxCounter) {
                    NUtils.getGameUI().msg("Pig distance timeout, continuing...");
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Pull the rope to bring the pig closer.
     * Used when pig is too close to truffle (nurgling1 pattern).
     */
    private void pullRope(NGameUI gui) throws InterruptedException {
        WItem rope = findLeashedRope(gui);
        if (rope == null) {
            rope = gui.getInventory().getItem(ROPE_ALIAS);
        }
        if (rope == null) {
            gui.msg("Warning: No rope found for pulling");
            return;
        }

        // Use flower menu "Pull" action on the rope
        new SelectFlowerAction("Pull", rope).run(gui);
    }

    private Results tieToHitchingPost(NGameUI gui, Gob hitchingPost) throws InterruptedException {
        // Find the leashed rope
        WItem rope = findLeashedRope(gui);
        if (rope == null) {
            rope = gui.getInventory().getItem(ROPE_ALIAS);
        }
        if (rope == null) {
            return Results.ERROR("No rope found in inventory");
        }

        // Navigate to hitching post
        new PathFinder(hitchingPost).run(gui);

        // Take rope to hand
        NUtils.takeItemToHand(rope);

        // Wait for rope to be in hand
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return NUtils.getGameUI().vhand != null;
            }
        });

        // Use rope on hitching post
        NUtils.activateItem(hitchingPost, false);

        // Wait for leash to be transferred to hitching post
        NUtils.addTask(new NTask() {
            {
                maxCounter = 100;
            }
            @Override
            public boolean check() {
                NGameUI g = NUtils.getGameUI();
                if (g.vhand == null) return true; // Rope was consumed or transferred

                GItem ropeItem = g.vhand.item;
                if (ropeItem != null && ropeItem.info() != null) {
                    for (ItemInfo info : ropeItem.info()) {
                        if (info instanceof Leashed) {
                            return false; // Still leashed, keep waiting
                        }
                    }
                    // No longer leashed
                    g.msg("Pig tied to hitching post!");
                    return true;
                }
                return false;
            }
        });

        // Transfer rope back to inventory if still in hand
        if (gui.vhand != null) {
            NUtils.dropToInv();
        }

        return Results.SUCCESS();
    }
}
