package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import haven.res.gfx.terobjs.roastspit.*;
import nurgling.*;
import nurgling.tasks.*;

public class SelectFlowerAction implements Action
{
    String opt;

    Object target;
    Sprite spr = null;
    boolean ignoreErrors = false;
    public SelectFlowerAction(String opt, WItem item)
    {
        this.opt = opt;
        this.target = item;
    }

    public SelectFlowerAction(String opt, Gob gob)
    {
        this.opt = opt;
        this.target = gob;
    }

    public SelectFlowerAction(String opt, Gob gob, Roastspit spr)
    {
        this.opt = opt;
        this.target = gob;
        this.spr = spr;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(target instanceof WItem)
        {
            WItem item = (WItem) target;
            item.item.wdgmsg("iact", item.c, 0);
        }
        else if (target instanceof Gob)
        {
            Gob gob = (Gob) target;
            if (spr==null)
            {
                gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 1, (int) gob.id, gob.rc.floor(posres),
                        0, -1);
            }
            else
            {
                for (Gob.Overlay ol : gob.ols) {
                    if (ol.spr == spr)
                        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 1, (int) gob.id,
                                gob.rc.floor(posres), ol.id, -1);
                }
            }
        }

        NFlowerMenu fm = NUtils.getFlowerMenu();
        if(fm.chooseOpt(opt))
        {
            NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
            return Results.SUCCESS();
        }
        else
        {
            NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
            if(!ignoreErrors)
                return Results.ERROR("NO OPT:" + opt);
            return Results.FAIL();
        }

    }
}
