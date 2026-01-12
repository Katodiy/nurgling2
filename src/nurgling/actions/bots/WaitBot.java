package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.WaitDuration;

import java.util.Map;

/**
 * A scenario-only bot that waits for a configurable amount of time.
 * The duration is specified in hh:mm:ss format via scenario settings.
 */
public class WaitBot implements Action {
    private Long waitDurationMs = null;

    public WaitBot() {
    }

    public WaitBot(Map<String, Object> settings) {
        if (settings != null && settings.containsKey("waitDurationMs")) {
            Object value = settings.get("waitDurationMs");
            if (value instanceof Long) {
                this.waitDurationMs = (Long) value;
            } else if (value instanceof Integer) {
                this.waitDurationMs = ((Integer) value).longValue();
            } else if (value instanceof Number) {
                this.waitDurationMs = ((Number) value).longValue();
            }
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (waitDurationMs == null || waitDurationMs <= 0) {
            return Results.ERROR("No wait duration configured");
        }

        NUtils.addTask(new WaitDuration(waitDurationMs));

        return Results.SUCCESS();
    }

    /**
     * Parses a time string in hh:mm:ss format to milliseconds.
     * @param timeStr Time string in format "hh:mm:ss" or "mm:ss" or "ss"
     * @return Duration in milliseconds, or -1 if parsing fails
     */
    public static long parseTimeToMs(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return -1;
        }

        try {
            String[] parts = timeStr.trim().split(":");
            long hours = 0, minutes = 0, seconds = 0;

            if (parts.length == 3) {
                hours = Long.parseLong(parts[0]);
                minutes = Long.parseLong(parts[1]);
                seconds = Long.parseLong(parts[2]);
            } else if (parts.length == 2) {
                minutes = Long.parseLong(parts[0]);
                seconds = Long.parseLong(parts[1]);
            } else if (parts.length == 1) {
                seconds = Long.parseLong(parts[0]);
            } else {
                return -1;
            }

            if (hours < 0 || minutes < 0 || seconds < 0 || minutes > 59 || seconds > 59) {
                return -1;
            }

            return (hours * 3600 + minutes * 60 + seconds) * 1000;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formats milliseconds to hh:mm:ss string.
     * @param ms Duration in milliseconds
     * @return Formatted time string
     */
    public static String formatMsToTime(long ms) {
        if (ms < 0) return "00:00:00";

        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
