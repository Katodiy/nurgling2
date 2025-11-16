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
        this(600, 5000);
    }

    public WaitForGobStability(long stabilityWindow, long maxWaitTime) {
        this.stabilityWindow = stabilityWindow;
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public boolean check() {
        boolean res = false;
        List<Gob> nearbyGobs = Finder.findGobs(new NAlias(""));
        int currentGobCount = nearbyGobs.size();

        // If gob count changed, reset stability timer
        if (currentGobCount != lastGobCount) {
            lastGobCount = currentGobCount;
            lastGobCountChangeTime = System.currentTimeMillis();
            res = false;
        }

        // If no change recorded yet, we're not stable
        if (lastGobCountChangeTime == -1) {
            res = false;
        }

        // Check if we've been stable long enough
        res = ((System.currentTimeMillis() - lastGobCountChangeTime) >= stabilityWindow);
        if(res)
        {
            long currentTime = System.currentTimeMillis();

            if (startTime == -1) {
                startTime = currentTime;
            }

            // Safety timeout - don't wait forever
            if (currentTime - startTime > maxWaitTime) {
                return true;
            }
        }
        return res;
    }
}