package nurgling;

import haven.*;
import nurgling.widgets.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/** NUI class extends the main UI to provide customized functionality and integrate with Nurgling's advanced features */
public class NUI extends UI
{
    /** Current tick identifier to track time-based operations within the UI */
    public long tickId = 0;
    /** Data tables for various functionalities */
    public NDataTables dataTables;
    /** Session information including username and subscription details */
    public NSessInfo sessInfo;
    /** Currently monitored widget for events */
    Widget monitor = null;
    /** List of widget IDs currently tracked */
    HashSet<Integer> statusWdg = new HashSet<>();
    /** Counter for periodic operations ticks */
    private int periodicCheckTick = 0;
    /** Precomputed Z multiplier for performance */
    private static final double DELTA_Z_DIVISOR = 10.0;
    /** Global UI opacity (0.0 = transparent, 1.0 = opaque) */
    private float uiOpacity = 1.0f;
    /** Horse mounting state tracking */
    private boolean wasMountedOnHorse = false;
    private long lastHorseSpeedCheck = 0;
    /** Window background mode (true = solid color, false = textures) */
    private boolean useSolidBackground = false;
    /** Window background color for solid mode */
    private java.awt.Color windowBackgroundColor = new java.awt.Color(32, 32, 32);

    /** Static verification flags - persist across NUI instances */
    private static boolean staticIsVerified = false;
    private static boolean staticIsSubscribed = false;
    private static String staticVerifiedUser = null;

    /** Container for session data and verification */
    public class NSessInfo
    {
        /** Username for the session */
        public String username;
        /** Verification flag */
        public boolean isVerified = false;
        /** Subscription flag */
        public boolean isSubscribed = false;
        /** Character info for the session */
        public NCharacterInfo characterInfo = null;

        /**
         * Session information constructor.
         * @param username The session's username.
         */
        public NSessInfo(String username)
        {
            this.username = username;
            // Restore static flags if same user, reset if different user
            if (username != null && username.equals(staticVerifiedUser)) {
                this.isVerified = staticIsVerified;
                this.isSubscribed = staticIsSubscribed;
            } else if (username != null && staticVerifiedUser != null && !username.equals(staticVerifiedUser)) {
                // Different user - reset static flags
                staticIsVerified = false;
                staticIsSubscribed = false;
                staticVerifiedUser = null;
            }
        }

        /** Update verification status and save to static */
        public void setVerified(boolean verified) {
            this.isVerified = verified;
            staticIsVerified = verified;
            staticVerifiedUser = this.username;
        }

        /** Update subscription status and save to static */
        public void setSubscribed(boolean subscribed) {
            this.isSubscribed = subscribed;
            staticIsSubscribed = subscribed;
            staticVerifiedUser = this.username;
        }
    }

    /**
     * Constructor for NUI.
     *
     * @param uictx The context for the UI.
     * @param sz    The size of the UI.
     * @param fun   The runner function for the UI.
     */
    public NUI(Context uictx, Coord sz, Runner fun)
    {
        super(uictx, sz, fun);
        if (fun != null)
        {
            root.add(core = new NCore());
            bind(core, 7001);
            core.debug = (Boolean) NConfig.get(NConfig.Key.debug);
            dataTables = new NDataTables();

            // Load opacity settings from config
            loadOpacitySettings();
        }
    }

    /**
     * Initializes session info immediately when session is available.
     * This must be called before GameUI is created to avoid race conditions
     * where characterInfo would not be set.
     */
    public void initSessInfo()
    {
        if (sessInfo == null && sess != null)
        {
            sessInfo = new NSessInfo(sess.user.name);
        }
    }

    @Override
    public void tick()
    {
        tickId += 1;
        periodicCheckTick++;
        
        // Initialize session info once
        if (sessInfo == null && sess != null)
        {
            sessInfo = new NSessInfo(sess.user.name);
        }

        // Check for horse mount/dismount periodically (every 10 ticks = ~0.5 seconds)
        if (gui != null && periodicCheckTick % 10 == 0) {
            checkHorseMountState();
        }

        super.tick();
    }

    @Override
    public void keydown(KeyEvent ev)
    {
        setmods(ev);
        if (gui != null && gui.map != null)
        {
            if (ev.getKeyCode() == KeyEvent.VK_SHIFT)
            {
                core.isinspect = true;
                return;
            }
        }
        super.keydown(ev);
    }

    @Override
    public void mousemove(MouseEvent ev, Coord c)
    {
        if (gui != null && gui.map != null)
        {
            if (core != null && core.isinspect)
            {
                NMapView mapView = (NMapView) gui.map;
                if (modshift)
                {
                    // Apply throttling for inspect calls
                    long currentTime = System.currentTimeMillis();
                    boolean shouldInspect = false;
                    
                    // Check time since last inspect
                    if (currentTime - lastInspectTime >= INSPECT_THROTTLE_MS) {
                        shouldInspect = true;
                    }
                    
                    // Check distance from last inspect position
                    if (lastInspectCoord != null && c.dist(lastInspectCoord) >= INSPECT_MIN_DISTANCE) {
                        shouldInspect = true;
                    }
                    
                    // If first inspect or conditions met
                    if (lastInspectCoord == null || shouldInspect) {
                        // Check which inspect mode to use
                        boolean debugMode = core.debug;
                        
                        if (debugMode) {
                            // Debug mode takes priority - show full info
                            mapView.inspect(c);
                        } else {
                            // Check simpleInspect only if debug is off
                            boolean simpleInspect = (Boolean) NConfig.get(NConfig.Key.simpleInspect);
                            if (simpleInspect) {
                                // Simple inspect mode - show only gob and tile
                                mapView.inspectSimple(c);
                            }
                        }
                        
                        lastInspectTime = currentTime;
                        lastInspectCoord = c;
                    }
                } else
                {
                    core.isinspect = false;
                    mapView.ttip.clear();
                    // Reset throttling when Shift is released
                    lastInspectCoord = null;
                    lastInspectTime = 0;
                }
            }
        }
        super.mousemove(ev, c);
    }

    /**
     * Searches for a widget of a specific class within the root.
     *
     * @param c The class type of the widget to find.
     * @return The widget if found, otherwise null.
     */
    public Widget findInRoot(Class<?> c)
    {
        for (Widget wdg : root.children())
        {
            if (wdg.getClass() == c)
            {
                return wdg;
            }
        }
        return null;
    }

    /**
     * Searches for a window with a specific caption within the root.
     *
     * @param cap The caption of the window.
     * @return The window if found, otherwise null.
     */
    public Window findInRoot(String cap)
    {
        for (Widget wdg : root.children())
        {
            if (wdg instanceof Window)
            {
                if (((Window) wdg).cap.equals(cap))
                    return (Window) wdg;
            }
        }
        return null;
    }

    /**
     * Retrieves the ID of the MenuGrid widget.
     *
     * @return The ID of the MenuGrid widget.
     */
    public int getMenuGridId()
    {
        int id = 0;
        // Check all registered widgets
        for (Map.Entry<Widget, Integer> widget : rwidgets.entrySet())
        {
            if (widget.getKey() instanceof MenuGrid)
            {
                // If the checked widget is Equipment, return id
                id = widget.getValue();
            }
        }
        return id;
    }

    /**
     * Calculates the delta Z value for some transformations.
     * Optimized version using precomputed constants.
     *
     * @return The calculated delta Z.
     */
    public float getDeltaZ()
    {
        return (float) Math.sin(tickId / DELTA_Z_DIVISOR);
    }


    /**
     * Enables monitoring for a specific widget.
     *
     * @param widget The widget to monitor.
     */
    public void enableMonitor(Widget widget)
    {
        monitor = widget;
        statusWdg = new HashSet<>();
    }

    /**
     * Disables the current widget monitoring.
     */
    public void disableMonitor()
    {

    }

    @Override
    public void addwidget(int id, int parent, Object... pargs)
    {
        super.addwidget(id, parent, pargs);
//      if(monitor!=null && (parent == monitor.wdgid() || (getwidget(parent)!=null && getwidget(parent).parent!=null && getwidget(parent).parent.wdgid() == monitor.wdgid()))) {
        if (monitor != null)
        {
            synchronized (statusWdg)
            {
                statusWdg.add(id);
            }
        }
    }

    @Override
    public void destroy(Widget wdg)
    {
        synchronized (statusWdg)
        {
            statusWdg.remove(wdg.wdgid());
        }
        super.destroy(wdg);
    }

    /**
     * Retrieves information about the monitored widgets.
     *
     * @return An ArrayList of monitored widgets.
     */
    public ArrayList<Widget> getMonitorInfo()
    {
        ArrayList<Widget> res = new ArrayList<>();
        synchronized (statusWdg)
        {
            for (int id : statusWdg)
            {
                res.add(getwidget(id));
            }
        }
        return res;
    }

    /** Cached value of modifier flags to improve performance */
    private int cachedModFlags = -1;
    /** Last modifier state to detect changes */
    private int lastModifiersNUI = -1;
    
    /** Timestamp of last inspect call */
    private long lastInspectTime = 0;
    /** Last coordinates of inspect call */
    private Coord lastInspectCoord = null;
    /** Minimum interval between inspect calls (20 FPS cap) */
    private static final long INSPECT_THROTTLE_MS = 50;
    /** Minimum distance for new inspect call */
    private static final int INSPECT_MIN_DISTANCE = 5;

    /**
     * Returns cached modifier flags to enhance performance.
     * Cache is invalidated on modifier state change.
     * 
     * @return Bitmask of active modifier keys
     */
    public int modflags() {
        if (cachedModFlags != -1) {
            return cachedModFlags;
        }
        
        cachedModFlags = ((modshift ? MOD_SHIFT : 0) |
                          (modctrl ? MOD_CTRL : 0) |
                          (modmeta ? MOD_META : 0) |
                          (modsuper ? MOD_SUPER : 0));
        return cachedModFlags;
    }

    /**
     * Optimized method to set modifiers with caching for performance enhancement.
     * Avoids unnecessary calculations if modifiers remain unchanged.
     * 
     * @param ev Input event containing modifier information
     */
    protected void setmods(InputEvent ev) {
        int mod = ev.getModifiersEx();
        if (lastModifiersNUI == mod) {
            return;
        }
        lastModifiersNUI = mod;
        
        boolean oldModshift = modshift;
        modshift = (mod & InputEvent.SHIFT_DOWN_MASK) != 0;
        modctrl = (mod & InputEvent.CTRL_DOWN_MASK) != 0;
        modmeta = (mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0;

        cachedModFlags = -1;
        
        // Reset tooltip cache when Shift state changes to force tooltip refresh
        if (oldModshift != modshift) {
            prevtt = null;
        }
    }

    /**
     * Check if player has mounted or dismounted a horse and apply preferred speed
     */
    private void checkHorseMountState() {
        try {
            // Throttle checks to prevent excessive processing
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHorseSpeedCheck < 500) { // 0.5 second minimum interval
                return;
            }
            lastHorseSpeedCheck = currentTime;

            // Check if player exists and game is ready
            if (gui.map == null || NUtils.player() == null) {
                return;
            }

            // Check for Following attribute (indicates mounted on something)
            haven.Following following = NUtils.player().getattr(haven.Following.class);
            boolean currentlyMounted = false;

            if (following != null) {
                // Player is mounted on something, check if it's a horse
                haven.Gob mount = gui.ui.sess.glob.oc.getgob(following.tgt);
                if (mount != null && mount.ngob != null && mount.ngob.name != null) {
                    // Check if the mount is a horse by checking the resource path
                    String mountName = mount.ngob.name.toLowerCase();
                    currentlyMounted = mountName.contains("horse") ||
                                     mountName.contains("stallion") ||
                                     mountName.contains("mare");
                }
            }

            // Detect state change
            if (currentlyMounted && !wasMountedOnHorse) {
                // Just mounted a horse - apply preferred horse speed
                applyPreferredHorseSpeed();
                wasMountedOnHorse = true;
            } else if (!currentlyMounted && wasMountedOnHorse) {
                // Just dismounted - apply preferred walking speed
                applyPreferredWalkingSpeed();
                wasMountedOnHorse = false;
            }

        } catch (Exception e) {
            // Silently handle any errors to prevent spam
            System.err.println("[NUI] Horse mount check failed: " + e.getMessage());
        }
    }

    /**
     * Apply user's preferred horse speed
     */
    private void applyPreferredHorseSpeed() {
        try {
            Object speedPref = NConfig.get(NConfig.Key.preferredHorseSpeed);
            if (speedPref instanceof Number) {
                int preferredSpeed = ((Number) speedPref).intValue();
                if (preferredSpeed >= 0 && preferredSpeed <= 3) {
                    // Small delay to ensure mount is fully processed
                    new Thread(() -> {
                        try {
                            Thread.sleep(200); // Brief pause for mount to complete
                            NUtils.setSpeed(preferredSpeed);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            System.err.println("[NUI] Failed to set preferred horse speed: " + e.getMessage());
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            System.err.println("[NUI] Failed to apply preferred horse speed: " + e.getMessage());
        }
    }

    /**
     * Apply user's preferred walking speed when dismounting
     */
    private void applyPreferredWalkingSpeed() {
        try {
            Object speedPref = NConfig.get(NConfig.Key.preferredMovementSpeed);
            if (speedPref instanceof Number) {
                int preferredSpeed = ((Number) speedPref).intValue();
                if (preferredSpeed >= 0 && preferredSpeed <= 3) {
                    // Small delay to ensure dismount is fully processed
                    new Thread(() -> {
                        try {
                            Thread.sleep(200); // Brief pause for dismount to complete
                            NUtils.setSpeed(preferredSpeed);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            System.err.println("[NUI] Failed to set preferred walking speed: " + e.getMessage());
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            System.err.println("[NUI] Failed to apply preferred walking speed: " + e.getMessage());
        }
    }

    /**
     * Set global UI opacity
     * @param opacity Value between 0.0 (transparent) and 1.0 (opaque)
     */
    public void setUIOpacity(float opacity) {
        this.uiOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    /**
     * Get current global UI opacity
     * @return Current opacity value between 0.0 and 1.0
     */
    public float getUIOpacity() {
        return this.uiOpacity;
    }

    /**
     * Set whether to use solid color background for windows
     * @param useSolid true for solid color, false for textures
     */
    public void setUseSolidBackground(boolean useSolid) {
        this.useSolidBackground = useSolid;
    }

    /**
     * Get whether solid color background is enabled
     * @return true if using solid color, false if using textures
     */
    public boolean getUseSolidBackground() {
        return this.useSolidBackground;
    }

    /**
     * Set window background color for solid mode
     * @param color The background color to use
     */
    public void setWindowBackgroundColor(java.awt.Color color) {
        this.windowBackgroundColor = color;
    }

    /**
     * Get window background color for solid mode
     * @return The current background color
     */
    public java.awt.Color getWindowBackgroundColor() {
        return this.windowBackgroundColor;
    }

    /**
     * Load opacity settings from NConfig at startup
     */
    private void loadOpacitySettings() {
        Object configOpacityObj = NConfig.get(NConfig.Key.uiOpacity);
        Boolean configUseSolid = (Boolean) NConfig.get(NConfig.Key.useSolidBackground);
        java.awt.Color configColor = NConfig.getColor(NConfig.Key.windowBackgroundColor, new java.awt.Color(32, 32, 32));

        // Handle opacity with proper type conversion (JSON may return BigDecimal)
        float opacity = 1.0f; // default
        if (configOpacityObj instanceof Number) {
            opacity = ((Number) configOpacityObj).floatValue();
        }

        // Apply loaded settings
        this.uiOpacity = opacity;
        this.useSolidBackground = configUseSolid != null ? configUseSolid : false;
        this.windowBackgroundColor = configColor;
    }

}
