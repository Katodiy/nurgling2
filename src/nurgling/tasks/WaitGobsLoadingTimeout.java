package nurgling.tasks;

import nurgling.NUtils;

public class WaitGobsLoadingTimeout extends NTask {
    private final long quietPeriodMs;

    public WaitGobsLoadingTimeout(long quietPeriodMs) {
        this.quietPeriodMs = quietPeriodMs;
    }

    @Override
    public boolean check() {
        if (NUtils.getGameUI() == null || NUtils.getGameUI().map == null || NUtils.getGameUI().map.glob == null)
            return false;

        long now = System.currentTimeMillis();
        long lastActivity = NUtils.getGameUI().map.glob.oc.lastGobActivity;
        return now - lastActivity >= quietPeriodMs;
    }
}
