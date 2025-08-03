package nurgling;

import haven.*;
import nurgling.widgets.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** NUI class extends the main UI to provide customized functionality and integrate with Nurgling's advanced features */
public class NUI extends UI
{
    /** Identifier for the current tick to track time-based operations within the UI */
    public long tickId = 0;
    /** Stores data tables used across the UI for different functionalities */
    public NDataTables dataTables;
    /** Information related to the current session including username and subscription details */
    public NSessInfo sessInfo;
    /** Widget currently being monitored for events and updates */
    Widget monitor = null;
    /** Collection of widget IDs currently being tracked */
    HashSet<Integer> statusWdg = new HashSet<>();

    /** Session information container that holds user data and verification status */
    public class NSessInfo
    {
        /** Username of the current session */
        public String username;
        /** Flag indicating if user is verified */
        public boolean isVerified = false;
        /** Flag indicating if user has active subscription */
        public boolean isSubscribed = false;
        /** Character information associated with the session */
        public NCharacterInfo characterInfo = null;

        /**
         * Constructor for session information.
         * @param username The username for this session.
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
        if (sessInfo == null && sess != null)
        {
            sessInfo = new NSessInfo(sess.username);
        }
        if (gui == null && sessInfo != null)
        {
            for (Widget wdg : widgets.values())
            {
                if (wdg instanceof Img)
                {
                    Img img = (Img) wdg;
                    if (img.tooltip instanceof Widget.KeyboundTip)
                    {
                        if (!sessInfo.isVerified && ((Widget.KeyboundTip) img.tooltip).base.contains("Verif"))
                            sessInfo.isVerified = true;
                        else if (!sessInfo.isSubscribed && ((Widget.KeyboundTip) img.tooltip).base.contains("Subsc"))
                            sessInfo.isSubscribed = true;
                    }
                }
            }
        }
        super.tick();
    }

    @Override
    public void keydown(KeyEvent ev)
    {
        setmods(ev);
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null)
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
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null)
        {
            if (core != null && core.debug && core.isinspect)
            {
                if (modshift)
                {
                    ((NMapView) NUtils.getGameUI().map).inspect(c);
                } else
                {
                    core.isinspect = false;
                    ((NMapView) NUtils.getGameUI().map).ttip.clear();
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
     *
     * @return The calculated delta Z.
     */
    public float getDeltaZ()
    {
        return (float) Math.sin(tickId / 10.) * 1;
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
}
