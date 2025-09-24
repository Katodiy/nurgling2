package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.List;

public class WaitForGobStability extends NTask {
    private final long stabilityWindow;
    private final long maxWaitTime;

    private int lastGobCount = -1;
    private long lastGobCountChangeTime = -1;
    private long startTime = -1;

    public WaitForGobStability() {
        this(600, 5000); // 200ms stability, 5s max wait
    }

    public WaitForGobStability(long stabilityWindow, long maxWaitTime) {
        this.stabilityWindow = stabilityWindow;
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public boolean check() {
        long currentTime = System.currentTimeMillis();

        if (startTime == -1) {
            startTime = currentTime;
        }

        // Safety timeout - don't wait forever
        if (currentTime - startTime > maxWaitTime) {
            return true;
        }

        // Count gobs in the specified area
        List<Gob> nearbyGobs = Finder.findGobs(new NAlias(""));
        int currentGobCount = nearbyGobs.size();

        // If gob count changed, reset stability timer
        if (currentGobCount != lastGobCount) {
            lastGobCount = currentGobCount;
            lastGobCountChangeTime = currentTime;
            return false;
        }

        // If no change recorded yet, we're not stable
        if (lastGobCountChangeTime == -1) {
            return false;
        }

        // Check if we've been stable long enough
        return (currentTime - lastGobCountChangeTime) >= stabilityWindow;
    }
}