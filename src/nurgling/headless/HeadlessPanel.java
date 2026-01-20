package nurgling.headless;

import haven.*;
import nurgling.NUI;

import java.awt.*;

/**
 * Headless implementation of UIPanel for running without graphics.
 * Provides the game loop and UI management without any rendering.
 */
public class HeadlessPanel implements UIPanel, UI.Context {
    private volatile UI ui;
    private volatile boolean running = true;
    private volatile boolean background = false;
    private Coord size;
    private final Object uiLock = new Object();

    // Tick rate for headless mode (20 ticks per second matches server tick rate)
    private static final double TICK_RATE = 20.0;
    private static final double TICK_DURATION = 1.0 / TICK_RATE;

    public HeadlessPanel(Coord size) {
        this.size = size;
    }

    public HeadlessPanel() {
        this(new Coord(800, 600));
    }

    @Override
    public UI newui(UI.Runner fun) {
        UI prevui;
        UI newui = new NUI(this, size, fun);
        UI.setInstance((NUI) newui);

        // Initialize headless rendering environment
        // This is critical for operations that use hit-testing (placement, clicks)
        newui.env = new HeadlessEnvironment();
        log("Initialized HeadlessEnvironment for UI");

        synchronized (uiLock) {
            prevui = this.ui;
            this.ui = newui;
        }

        if (prevui != null) {
            synchronized (prevui) {
                prevui.destroy();
            }
        }

        return newui;
    }

    private static void log(String message) {
        System.out.println("[HeadlessPanel] " + message);
    }

    @Override
    public void background(boolean bg) {
        this.background = bg;
    }

    @Override
    public void setSize(int w, int h) {
        this.size = new Coord(w, h);
        UI currentui = this.ui;
        if (currentui != null && currentui.root != null) {
            synchronized (currentui) {
                currentui.root.resize(size);
            }
        }
    }

    @Override
    public Dimension getSize() {
        return new Dimension(size.x, size.y);
    }

    @Override
    public void setCursor(Cursor c) {
        // No cursor in headless mode
    }

    @Override
    public Component getParent() {
        // No parent component in headless mode
        return null;
    }

    @Override
    public void setmousepos(Coord c) {
        // No mouse in headless mode, but update UI position
        UI currentui = this.ui;
        if (currentui != null) {
            currentui.mc = c;
        }
    }

    /**
     * Stop the headless panel.
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Check if the panel is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the current UI.
     */
    public UI getUI() {
        return ui;
    }

    @Override
    public void run() {
        try {
            double lastTick = Utils.rtime();
            int tickCount = 0;
            long lastLogTime = System.currentTimeMillis();

            while (running) {
                double now = Utils.rtime();
                double elapsed = now - lastTick;

                UI currentui;
                synchronized (uiLock) {
                    currentui = this.ui;
                }

                if (currentui != null) {
                    synchronized (currentui) {
                        // Tick the session (network, glob, etc.)
                        if (currentui.sess != null) {
                            currentui.sess.glob.ctick();
                            // Send pending map requests (normally done in MapView.gtick during rendering)
                            currentui.sess.glob.map.sendreqs();
                        }

                        // Tick the UI
                        currentui.tick();
                        currentui.lastevent = now;
                    }
                }

                tickCount++;
                // Log every 5 seconds
                if (System.currentTimeMillis() - lastLogTime > 5000) {
                    String rootInfo = "root: no";
                    if (currentui != null && currentui.root != null) {
                        int childCount = 0;
                        for (Widget w = currentui.root.child; w != null; w = w.next) {
                            childCount++;
                        }
                        rootInfo = "root children: " + childCount;
                    }
                    log("Running: " + tickCount + " ticks" +
                        ", gui: " + (currentui != null && currentui.gui != null ? "yes" : "no") +
                        ", sess: " + (currentui != null && currentui.sess != null ? "connected" : "no") +
                        ", " + rootInfo);
                    lastLogTime = System.currentTimeMillis();
                }

                // Maintain tick rate
                double targetTime = lastTick + TICK_DURATION;
                double sleepTime = targetTime - Utils.rtime();
                if (sleepTime > 0) {
                    Thread.sleep((long)(sleepTime * 1000));
                }
                lastTick = targetTime;

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run a single tick iteration (for external control).
     */
    public void tick() {
        UI currentui;
        synchronized (uiLock) {
            currentui = this.ui;
        }

        if (currentui != null) {
            synchronized (currentui) {
                if (currentui.sess != null) {
                    currentui.sess.glob.ctick();
                }
                currentui.tick();
                currentui.lastevent = Utils.rtime();
            }
        }
    }
}
