package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.conf.*;

import java.util.*;

public class NBuddyWnd extends BuddyWnd
{
    ICheckBox settings;
    NKinSettings ks = null;
    final Coord shift = UI.scale(16,5);
    public NBuddyWnd()
    {
        add(settings = new ICheckBox(NStyle.settingsi[0], NStyle.settingsi[1], NStyle.settingsi[2], NStyle.settingsi[3])
        {
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                if(val)
                {
                    if(ks == null)
                    {
                        ks = new NKinSettings(settings);
                        ui.root.add(ks, NUtils.getGameUI().zerg.rootpos());
                    }
                    ks.show();
                    ks.raise();
                    ks.move(NUtils.getGameUI().zerg.rootpos().sub(UI.scale(0,50)));
                }
                else
                    ks.hide();
            }
        }, new Coord(sz.x - NStyle.settingsi[0].sz().x / 2, NStyle.settingsi[0].sz().y / 2).sub(shift));

        pack();

    }

    final Set<Integer> req = new HashSet<>();
    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        double now = Utils.rtime();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().zerg!=null && NUtils.getGameUI().zerg.visible && parent.visible)
        {
            synchronized (req)
            {
                int count = 0;
                if (req.isEmpty())
                    for (Buddy b : buddies)
                    {
                        if ((now - b.upTime > 10 || b.upTime == 0) && count++<7)
                        {
                            wdgmsg("ch", b.id);
                            req.add(b.id);
                            b.upTime = now;
                        }
                    }
            }
            for (Buddy b : buddies)
            {
                b.lastOnline = Text.render(lastOnline(b.atime, b, null));
            }
        }
    }

    int lastSet = -1;

    @Override
    public void uimsg(String msg, Object... args)
    {
        synchronized (req)
        {
            if(!req.isEmpty() )
            {
                if (msg.equals("i-set"))
                {
                    if (req.contains((int) args[0]))
                    {
                        lastSet = (int) args[0];
                        req.remove((int) args[0]);
                        return;
                    }
                }
            }
            if(lastSet!=-1)
            {
                if(msg.equals("i-atime") && lastSet!=-1)
                {
                    for(Buddy b : buddies)
                    {
                        if(b.id == lastSet)
                        {
                            b.atime = (long)Utils.ntime() - ((Number)args[0]).longValue();
                            lastSet = -1;
                            return;
                        }
                    }
                }
                if(msg.equals("i-ava"))
                {
                    return;
                }
            }
        }
        super.uimsg(msg, args);
    }

}
