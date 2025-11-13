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
    /** Flag for session info initialization frequency control */
    private boolean sessInfoChecked = false;
    /** Counter for periodic operations ticks */
    private int periodicCheckTick = 0;
    /** Precomputed Z multiplier for performance */
    private static final double DELTA_Z_DIVISOR = 10.0;
    /** Periodicity for session verification checks */
    private static final int SESSION_CHECK_PERIOD = 60;
    /** Horse mounting state tracking */
    private boolean wasMountedOnHorse = false;
    private long lastHorseSpeedCheck = 0;

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
        
        // Only check for verification/subscription periodically to reduce CPU load
        if (gui == null && sessInfo != null && !sessInfoChecked && periodicCheckTick % SESSION_CHECK_PERIOD == 0)
        {
            checkSessionVerification();
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
            if (core != null && core.debug && core.isinspect)
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
                        mapView.inspect(c);
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
     * Checks session verification and subscription status by examining widget tooltips.
     * This method is called periodically to reduce CPU load.
     */
    private void checkSessionVerification()
    {
        if (sessInfo == null) return;
        
        for (Widget wdg : widgets.values())
        {
            if (wdg instanceof Img)
            {
                Img img = (Img) wdg;
                if (img.tooltip instanceof Widget.KeyboundTip)
                {
                    String tooltipText = ((Widget.KeyboundTip) img.tooltip).base;
                    if (!sessInfo.isVerified && tooltipText.contains("Verif"))
                    {
                        sessInfo.isVerified = true;
                    }
                    else if (!sessInfo.isSubscribed && tooltipText.contains("Subsc"))
                    {
                        sessInfo.isSubscribed = true;
                    }
                    
                    // If both flags are set, we can stop checking
                    if (sessInfo.isVerified && sessInfo.isSubscribed)
                    {
                        sessInfoChecked = true;
                        break;
                    }
                }
            }
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
}
