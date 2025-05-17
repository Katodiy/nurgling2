package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class GateDetector {
    private static final String[] GATE_NAMES = {
        "gfx/terobjs/arch/polebiggate",
        "gfx/terobjs/arch/drystonewallbiggate",
        "gfx/terobjs/arch/polegate",
        "gfx/terobjs/arch/drystonewallgate"
    };

    private static final double GATE_PROXIMITY_THRESHOLD = 10.0;

    private Gob lastNearbyGate = null;
    private Coord2d lastNearbyPosition = null;
    private boolean wasNearGate = false;
    private Coord2d lastDistance = null; // Track the last distance vector

    public boolean isNearGate() {
        try {
            Gob gate = Finder.findGob(NUtils.player().rc, new NAlias(GATE_NAMES), null, GATE_PROXIMITY_THRESHOLD);
            if (gate != null && lastNearbyGate != gate) {
                lastNearbyGate = gate;
                lastNearbyPosition = NUtils.player().rc;
                wasNearGate = true;
                // Store the initial distance vector
                lastDistance = gate.rc.sub(NUtils.player().rc);
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
        lastDistance = null;
    }

    public Gob getLastNearbyGate() {
        return lastNearbyGate;
    }

    public Coord2d getLastNearbyPosition() {
        return lastNearbyPosition;
    }
}