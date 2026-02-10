package nurgling.headless;

import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Configuration for headless mode.
 * Parses from command-line arguments, environment variables, or bot config file.
 */
public class HeadlessConfig {
    // Required
    public String user;
    public String password;
    public String character;

    // Optional
    public Integer scenarioId;
    public String server = "game.havenandhearth.com";
    public int authPort = 1871;
    public int gamePort = 1870;
    public boolean loop = false;
    public boolean verbose = false;
    public String stackTraceFile; // For autorunner debugging

    /**
     * Parse configuration from command-line arguments and environment variables.
     * Command-line arguments take priority over environment variables.
     */
    public static HeadlessConfig parse(String[] args) {
        HeadlessConfig config = new HeadlessConfig();

        // 1. Load from environment first (lowest priority)
        config.user = System.getenv("NURGLING_USER");
        config.password = System.getenv("NURGLING_PASSWORD");
        config.character = System.getenv("NURGLING_CHAR");
        String envBot = System.getenv("NURGLING_BOT");
        if (envBot != null && !envBot.isEmpty()) {
            config.scenarioId = Integer.parseInt(envBot);
        }

        String envServer = System.getenv("NURGLING_SERVER");
        if (envServer != null && !envServer.isEmpty()) {
            config.server = envServer;
        }

        // 2. Override with command-line args (highest priority)
        for (String arg : args) {
            if (arg.startsWith("--user=")) {
                config.user = arg.substring(7);
            } else if (arg.startsWith("--password=")) {
                config.password = arg.substring(11);
            } else if (arg.startsWith("--char=")) {
                config.character = arg.substring(7);
            } else if (arg.startsWith("--bot=")) {
                config.scenarioId = Integer.parseInt(arg.substring(6));
            } else if (arg.startsWith("--server=")) {
                config.server = arg.substring(9);
            } else if (arg.startsWith("--auth-port=")) {
                config.authPort = Integer.parseInt(arg.substring(12));
            } else if (arg.startsWith("--game-port=")) {
                config.gamePort = Integer.parseInt(arg.substring(12));
            } else if (arg.equals("--loop")) {
                config.loop = true;
            } else if (arg.equals("--verbose") || arg.equals("-v")) {
                config.verbose = true;
            }
        }

        return config;
    }

    /**
     * Parse configuration from a JSON bot config file (used with -bots flag).
     * This is the format used by electron-hh-autorunner.
     */
    public static HeadlessConfig parseFromFile(String path) {
        HeadlessConfig config = new HeadlessConfig();

        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
            JSONObject json = new JSONObject(jsonString);

            config.user = json.optString("user", null);
            config.password = json.optString("password", null);
            config.character = json.optString("character", null);

            if (json.has("scenarioId")) {
                config.scenarioId = json.getInt("scenarioId");
            }

            if (json.has("stackTraceFile")) {
                config.stackTraceFile = json.getString("stackTraceFile");
            }

            // Server settings from environment (file doesn't contain these)
            String envServer = System.getenv("NURGLING_SERVER");
            if (envServer != null && !envServer.isEmpty()) {
                config.server = envServer;
            }

        } catch (Exception e) {
            System.err.println("Failed to parse bot config file: " + e.getMessage());
            e.printStackTrace();
        }

        return config;
    }

    /**
     * Check if the configuration has all required parameters.
     */
    public boolean isValid() {
        return user != null && !user.isEmpty() &&
               password != null && !password.isEmpty() &&
               character != null && !character.isEmpty();
    }

    /**
     * Get validation error message if configuration is invalid.
     */
    public String getValidationError() {
        if (user == null || user.isEmpty()) {
            return "Missing required parameter: --user";
        }
        if (password == null || password.isEmpty()) {
            return "Missing required parameter: --password";
        }
        if (character == null || character.isEmpty()) {
            return "Missing required parameter: --char";
        }
        return null;
    }

    /**
     * Print usage information.
     */
    public static void printUsage() {
        System.out.println("Nurgling2 Headless Mode");
        System.out.println("========================");
        System.out.println();
        System.out.println("Usage: java -jar nurgling.jar --headless [options]");
        System.out.println();
        System.out.println("Required Options:");
        System.out.println("  --user=USERNAME       Account username");
        System.out.println("  --password=PASSWORD   Account password");
        System.out.println("  --char=CHARACTER      Character name to login");
        System.out.println();
        System.out.println("Optional:");
        System.out.println("  --bot=BOTNAME         Bot to run (e.g., CheeseProductionBot)");
        System.out.println("  --server=HOST         Game server (default: game.havenandhearth.com)");
        System.out.println("  --auth-port=PORT      Auth port (default: 1871)");
        System.out.println("  --game-port=PORT      Game port (default: 1870)");
        System.out.println("  --loop                Restart bot on completion");
        System.out.println("  --verbose, -v         Verbose logging");
        System.out.println();
        System.out.println("Environment Variables:");
        System.out.println("  NURGLING_HEADLESS=true    Enable headless mode");
        System.out.println("  NURGLING_USER=username");
        System.out.println("  NURGLING_PASSWORD=password");
        System.out.println("  NURGLING_CHAR=character");
        System.out.println("  NURGLING_BOT=botname");
        System.out.println("  NURGLING_SERVER=host");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar nurgling.jar --headless \\");
        System.out.println("    --user=myaccount --password=secret \\");
        System.out.println("    --char=MyFarmer --bot=WheatFarmerQ");
        System.out.println();
        System.out.println("Exit Codes:");
        System.out.println("  0 - Success");
        System.out.println("  1 - Bot failure");
        System.out.println("  2 - Bot error");
        System.out.println("  3 - Connection lost");
        System.out.println("  4 - Configuration error");
        System.out.println("  5 - Authentication failed");
        System.out.println("  6 - Character not found");
    }

    @Override
    public String toString() {
        return String.format("HeadlessConfig{user='%s', char='%s', scenarioId=%s, server='%s'}",
                user, character, scenarioId, server);
    }
}
