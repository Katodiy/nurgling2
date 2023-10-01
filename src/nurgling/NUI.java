package nurgling;

import haven.*;
import nurgling.widgets.*;

import java.awt.event.*;

public class NUI extends UI
{
    public class NSessInfo {
        public String username;
        public boolean isVerified = false;
        public boolean isSubscribed = false;
        public NCharacterInfo characterInfo = null;
        public NSessInfo(String username) {
            this.username = username;
        }
    }
    public long tickId  = 0;
    public NDataTables dataTables;
    public NSessInfo sessInfo;
    public NUI(Context uictx, Coord sz, Runner fun)
    {
        super(uictx, sz, fun);
        if (fun != null)
        {
            root.add(core = new NCore());
            bind(core, 7001);
            dataTables = new NDataTables();
        }
    }

    @Override
    public void tick()
    {
        tickId+=1;
        if (sessInfo == null && sess != null)
        {
            sessInfo = new NSessInfo(sess.username);
        }
        if(GameUI.getInstance() == null && sessInfo!=null)
        {
            for (Widget wdg : widgets.values()) {
                if (wdg instanceof Img) {
                    Img img = (Img) wdg;
                    if (img.tooltip instanceof Widget.KeyboundTip) {
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
    public void keydown(KeyEvent ev) {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            if (ev.getKeyCode() == KeyEvent.VK_SHIFT) {
                core.isinspect = true;
            }
        }
        super.keydown(ev);
    }

    @Override
    public void mousemove(MouseEvent ev, Coord c) {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            if (core!=null && core.isinspect) {
                if (modshift) {
                    ((NMapView) NUtils.getGameUI().map).inspect(c);
                } else {
                    core.isinspect = false;
                    ((NMapView) NUtils.getGameUI().map).ttip.clear();
                }
            }
        }
        super.mousemove(ev, c);
    }
}
