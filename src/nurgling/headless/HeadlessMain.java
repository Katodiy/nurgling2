package nurgling.headless;

import haven.*;
import nurgling.NConfig;
import nurgling.NCore;
import haven.MainFrame;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Entry point for headless mode.
 * Handles authentication, session creation, and game loop.
 */
public class HeadlessMain {

    // Exit codes
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_BOT_FAILURE = 1;
    public static final int EXIT_BOT_ERROR = 2;
    public static final int EXIT_CONNECTION_LOST = 3;
    public static final int EXIT_CONFIG_ERROR = 4;
    public static final int EXIT_AUTH_FAILED = 5;
    public static final int EXIT_CHARACTER_NOT_FOUND = 6;

    private final HeadlessConfig config;
    private HeadlessPanel panel;
    private Thread panelThread;

    public HeadlessMain(HeadlessConfig config) {
        this.config = config;
    }

    /**
     * Main entry point for headless mode (CLI args).
     */
    public static void main(String[] args) {
        // Set headless mode FIRST, before any widget classes load
        Headless.setHeadless(true);

        // Set up resource loading BEFORE NConfig (fonts need resources)
        MainFrame.setupres();

        // Initialize NConfig (required for fonts, settings, etc.)
        NConfig nconfig = new NConfig();
        nconfig.read();
        MainFrame.config = nconfig;

        // Initialize Widget names (required for widget factory)
        Widget.initnames();

        // Parse configuration
        HeadlessConfig config = HeadlessConfig.parse(args);

        // Check for help flag (but not -h which is now headless flag)
        for (String arg : args) {
            if (arg.equals("--help")) {
                HeadlessConfig.printUsage();
                System.exit(EXIT_SUCCESS);
                return;
            }
        }

        // Validate configuration
        if (!config.isValid()) {
            System.err.println("Error: " + config.getValidationError());
            System.err.println();
            HeadlessConfig.printUsage();
            System.exit(EXIT_CONFIG_ERROR);
            return;
        }

        log("Starting headless mode");
        log("Config: " + config);

        HeadlessMain main = new HeadlessMain(config);
        int exitCode = main.run();

        log("Exiting with code: " + exitCode);
        System.exit(exitCode);
    }

    /**
     * Entry point for headless mode with pre-built config (from -bots file).
     * Called by MainFrame when -h and -bots flags are both present.
     */
    public static void runWithConfig(HeadlessConfig config) {
        // Headless should already be set by MainFrame, but ensure it
        if (!Headless.isHeadless()) {
            Headless.setHeadless(true);
        }

        // Set up resource loading BEFORE NConfig (fonts need resources)
        MainFrame.setupres();

        // Initialize NConfig (required for fonts, settings, etc.)
        NConfig nconfig = new NConfig();
        nconfig.read();
        MainFrame.config = nconfig;

        // Initialize Widget names (required for widget factory)
        Widget.initnames();

        // Validate configuration
        if (!config.isValid()) {
            System.err.println("Error: " + config.getValidationError());
            System.exit(EXIT_CONFIG_ERROR);
            return;
        }

        log("Starting headless mode (from bot config file)");
        log("Config: " + config);

        HeadlessMain main = new HeadlessMain(config);
        int exitCode = main.run();

        log("Exiting with code: " + exitCode);
        System.exit(exitCode);
    }

    /**
     * Run the headless client.
     */
    public int run() {
        try {
            // Authenticate
            log("Authenticating as: " + config.user);
            SimpleAuthClient authClient = new SimpleAuthClient(config.server, config.authPort);
            SimpleAuthResponse authResponse = authClient.authenticate(config);

            if (!authResponse.isSuccess()) {
                System.err.println("Authentication failed: " + authResponse.getError());
                return EXIT_AUTH_FAILED;
            }

            log("Authenticated successfully as: " + authResponse.getUsername());

            // Set up bot mode configuration
            setupBotMod();

            // Run game loop (potentially with restart)
            do {
                int result = runSession(authResponse);
                if (result != EXIT_SUCCESS && !config.loop) {
                    return result;
                }
                if (config.loop) {
                    log("Loop mode enabled, restarting...");
                    // Re-authenticate for new session
                    authResponse = authClient.authenticate(config);
                    if (!authResponse.isSuccess()) {
                        System.err.println("Re-authentication failed: " + authResponse.getError());
                        return EXIT_AUTH_FAILED;
                    }
                }
            } while (config.loop);

            return EXIT_SUCCESS;

        } catch (InterruptedException e) {
            log("Interrupted");
            Thread.currentThread().interrupt();
            return EXIT_BOT_ERROR;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return EXIT_BOT_ERROR;
        }
    }

    /**
     * Set up NConfig.botmod for auto-login and character selection.
     */
    private void setupBotMod() {
        NCore.BotmodSettings settings = new NCore.BotmodSettings(
            config.user,
            config.password,
            config.character,
            config.scenarioId
        );

        // Set stack trace file for autorunner debugging
        if (config.stackTraceFile != null) {
            settings.stackTraceFile = config.stackTraceFile;
        }

        // Set the botmod configuration
        NConfig.botmod = settings;
        log("Bot mode configured for character: " + config.character + ", scenarioId: " + config.scenarioId);
    }

    /**
     * Run a single game session.
     */
    private int runSession(SimpleAuthResponse auth) throws InterruptedException {
        log("Creating game session...");

        try {
            // Create session
            Session.User user = new Session.User(auth.getUsername());
            InetSocketAddress serverAddr = new InetSocketAddress(
                InetAddress.getByName(config.server),
                config.gamePort
            );

            Session session = new Session(serverAddr, user, Connection.encrypt.get(), auth.getCookie());
            log("Session created");

            // Skip Bootstrap entirely - go directly to RemoteUI
            // Bootstrap creates login screen which we don't need since we already authenticated
            RemoteUI remoteUI = new RemoteUI(session);

            // Create headless panel
            panel = new HeadlessPanel();

            // Start panel thread
            panelThread = new HackThread(() -> {
                try {
                    runUILoop(remoteUI);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "HeadlessUI");
            panelThread.start();

            // Wait for panel thread to finish
            panelThread.join();

            log("Session ended");
            return EXIT_SUCCESS;

        } catch (Connection.SessionError e) {
            System.err.println("Session error: " + e.getMessage());
            return EXIT_CONNECTION_LOST;
        } catch (java.io.IOException e) {
            System.err.println("IO error: " + e.getMessage());
            return EXIT_CONNECTION_LOST;
        }
    }

    /**
     * Run the UI loop with the given runner.
     */
    private void runUILoop(UI.Runner runner) throws InterruptedException {
        // Start the panel's tick loop in background
        Thread tickThread = new HackThread(panel, "HeadlessTick");
        tickThread.start();

        try {
            while (runner != null) {
                UI ui = panel.newui(runner);
                runner.init(ui);
                runner = runner.run(ui);
            }
        } finally {
            panel.stop();
            tickThread.interrupt();
            tickThread.join(5000);
        }
    }

    /**
     * Stop the headless client.
     */
    public void stop() {
        if (panel != null) {
            panel.stop();
        }
        if (panelThread != null) {
            panelThread.interrupt();
        }
    }

    private static void log(String message) {
        System.out.println("[Headless] " + message);
    }
}
