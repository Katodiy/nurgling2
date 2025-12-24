package nurgling.navigation;

/**
 * Centralized debug logging for the chunk navigation system.
 * Replace System.out.println("ChunkNav: ...") calls with ChunkNavDebug.log(...)
 */
public class ChunkNavDebug {
    private static boolean enabled = true; // TODO: revert to Boolean.getBoolean("chunknav.debug");

    /**
     * Log a debug message if debug mode is enabled.
     */
    public static void log(String msg) {
        if (enabled) {
            System.out.println("ChunkNav: " + msg);
        }
    }

    /**
     * Log a formatted debug message if debug mode is enabled.
     */
    public static void log(String format, Object... args) {
        if (enabled) {
            System.out.println("ChunkNav: " + String.format(format, args));
        }
    }

    /**
     * Log a warning message (always shown).
     */
    public static void warn(String msg) {
        System.out.println("ChunkNav WARNING: " + msg);
    }

    /**
     * Log an error message (always shown).
     */
    public static void error(String msg) {
        System.err.println("ChunkNav ERROR: " + msg);
    }

    /**
     * Log an error message with exception (always shown).
     */
    public static void error(String msg, Throwable t) {
        System.err.println("ChunkNav ERROR: " + msg);
        t.printStackTrace();
    }

    /**
     * Check if debug mode is enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable debug mode at runtime.
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    /**
     * Toggle debug mode.
     */
    public static void toggle() {
        enabled = !enabled;
        System.out.println("ChunkNav debug mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }
}
