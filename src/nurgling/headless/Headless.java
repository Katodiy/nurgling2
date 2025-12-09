package nurgling.headless;

/**
 * Global headless mode flag and utilities.
 *
 * CRITICAL: setHeadless(true) must be called BEFORE any haven widget classes
 * are loaded, otherwise their static texture initializers will fail.
 */
public class Headless {
    private static volatile boolean headless = false;
    private static volatile boolean initialized = false;

    /**
     * Set the headless mode flag. Must be called before any widget class loading.
     */
    public static void setHeadless(boolean value) {
        if (initialized && headless != value) {
            System.err.println("WARNING: Headless mode already initialized, cannot change");
            return;
        }
        headless = value;
        initialized = true;

        if (value) {
            // Set Java AWT headless mode
            System.setProperty("java.awt.headless", "true");
        }
    }

    /**
     * Check if running in headless mode.
     */
    public static boolean isHeadless() {
        return headless;
    }

    /**
     * Check if headless mode has been initialized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Check command line args for headless flag.
     * Call this EARLY in main() before any other initialization.
     */
    public static boolean hasHeadlessFlag(String[] args) {
        for (String arg : args) {
            if (arg.equals("--headless") || arg.equals("-H")) {
                return true;
            }
        }
        // Also check environment variable
        String env = System.getenv("NURGLING_HEADLESS");
        return "true".equalsIgnoreCase(env) || "1".equals(env);
    }
}
