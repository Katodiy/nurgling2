package nurgling.tasks;

/**
 * A task that waits for a specified duration using wall-clock time.
 * This task uses System.currentTimeMillis() to track elapsed time,
 * making it accurate for real-world timing regardless of game tick rate.
 */
public class WaitDuration extends NTask {
    private final long targetTimeMs;

    /**
     * Creates a new WaitDuration task.
     * @param durationMs The duration to wait in milliseconds
     */
    public WaitDuration(long durationMs) {
        this.targetTimeMs = System.currentTimeMillis() + durationMs;
        this.infinite = true;  // Disable counter-based timeout
    }

    @Override
    public boolean check() {
        return System.currentTimeMillis() >= targetTimeMs;
    }
}
