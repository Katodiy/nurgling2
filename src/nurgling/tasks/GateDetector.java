package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.Arrays;
import java.util.List;

public class GateDetector {
    private static final double GATE_PROXIMITY_THRESHOLD = 10.0;

    private Gob lastNearbyGate = null;
    private Coord2d lastNearbyPosition = null;
    private boolean wasNearGate = false;

    public static final String[] GATE_NAMES = {
            "gfx/terobjs/arch/polebiggate",
            "gfx/terobjs/arch/drystonewallbiggate",
            "gfx/terobjs/arch/polegate",
            "gfx/terobjs/arch/drystonewallgate",
    };

    public boolean isNearGate() {
        try {
            Gob gate = Finder.findGob(NUtils.player().rc, new NAlias(GATE_NAMES), null, GATE_PROXIMITY_THRESHOLD);
            if (gate != null && lastNearbyGate != gate) {
                lastNearbyGate = gate;
                lastNearbyPosition = NUtils.player().rc;
                wasNearGate = true;
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasPassedGate() {
        if (!wasNearGate || lastNearbyGate == null || lastNearbyPosition == null) {
            return false;
        }

        double halfTile = MCache.tilesz.x / 2.0;
        double minX = lastNearbyGate.rc.x - halfTile;
        double maxX = lastNearbyGate.rc.x + halfTile;
        double minY = lastNearbyGate.rc.y - halfTile;
        double maxY = lastNearbyGate.rc.y + halfTile;

        Coord2d playerNow = NUtils.player().rc;
        Coord2d playerBefore = lastNearbyPosition;

        boolean wasInside =
                playerBefore.x >= minX && playerBefore.x <= maxX &&
                        playerBefore.y >= minY && playerBefore.y <= maxY;

        boolean isNowOutside =
                playerNow.x < minX || playerNow.x > maxX ||
                        playerNow.y < minY || playerNow.y > maxY;

        // Save for next check
        lastNearbyPosition = playerNow;

        return wasInside && isNowOutside;
    }

    public boolean isMovingAwayFromGate() {
        if (!wasNearGate || lastNearbyGate == null || lastNearbyPosition == null) {
            return false;
        }

        // If we're moving away from the last known gate position
        double currentDist = NUtils.player().rc.dist(lastNearbyPosition);
        double previousDist = lastNearbyPosition.dist(lastNearbyGate.rc);

        return currentDist > previousDist && currentDist > GATE_PROXIMITY_THRESHOLD;
    }

    public void reset() {
        lastNearbyGate = null;
        lastNearbyPosition = null;
        wasNearGate = false;
    }

    public Gob getLastNearbyGate() {
        return lastNearbyGate;
    }

    public Coord2d getLastNearbyPosition() {
        return lastNearbyPosition;
    }

    public static boolean isDoorOpen(Gob gob) {
        return gob.ngob.getModelAttribute() == 1;
    }

    public static boolean isGobDoor(Gob gob) {
        List<String> listOfDoors = Arrays.asList(
                "stonestead",
                "stonemansion",
                "greathall",
                "primitivetent",
                "windmill",
                "stonetower",
                "logcabin",
                "timberhouse",
                "minehole",
                "ladder",
                "stairs",
                "cellardoor");
        for (String door : listOfDoors) {
            if(gob.ngob.name.contains(door)) {
                return true;
            }
        }

        return false;
    }

    public static String getDoorPair(String input) {
        String[][] pairs = {
                {"gfx/terobjs/arch/stonestead-door", "gfx/terobjs/arch/stonestead"},
                {"gfx/terobjs/arch/stonemansion-door", "gfx/terobjs/arch/stonemansion"},
                {"gfx/terobjs/arch/greathall-door", "gfx/terobjs/arch/greathall"},
                {"gfx/terobjs/arch/primitivetent-door", "gfx/terobjs/arch/primitivetent"},
                {"gfx/terobjs/arch/windmill-door", "gfx/terobjs/arch/windmill"},
                {"gfx/terobjs/arch/stonetower-door", "gfx/terobjs/arch/stonetower"},
                {"gfx/terobjs/arch/logcabin-door", "gfx/terobjs/arch/logcabin"},
                {"gfx/terobjs/arch/timberhouse-door", "gfx/terobjs/arch/timberhouse"},
                {"gfx/terobjs/minehole", "gfx/terobjs/ladder"},
                {"gfx/terobjs/arch/upstairs", "gfx/terobjs/arch/downstairs"},
                {"gfx/terobjs/arch/cellardoor", "gfx/terobjs/arch/cellarstairs"}
        };

        for (String[] pair : pairs) {
            if (pair[0].equals(input)) return pair[1];
            if (pair[1].equals(input)) return pair[0];
        }

        return null;
    }

    public static boolean isLastActionNonLoadingDoor() {
        if(NUtils.getUI().core.getLastActions() == null) {
            return false;
        }

        List<String> listOfDoors = Arrays.asList("stairs");
        for (String door : listOfDoors) {
            if(NUtils.getUI().core.getLastActions().gob.ngob.name.contains(door) &&
                    !NUtils.getUI().core.getLastActions().gob.ngob.name.contains("cellar")) {
                return true;
            }
        }

        return false;
    }
}