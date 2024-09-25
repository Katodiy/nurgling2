package nurgling.tasks;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.DynamicPf;
import nurgling.tools.NParser;

public class DynMovingCompleted implements NTask
{
    Gob gob;
    Coord2d old;
    Coord2d targetCoord;
    public DynamicPf.WorkerPf wpf;
    public DynMovingCompleted(DynamicPf.WorkerPf wpf, Gob target, Coord2d targetCoord)
    {
        this.wpf = wpf;
        this.gob = target;
        this.old = new Coord2d(gob.rc.x, gob.rc.y);
        this.targetCoord = new Coord2d(targetCoord.x, targetCoord.y);
        (t = new Thread(wpf)).start();
    }

    Thread t;

    @Override
    public boolean check()
    {
        Coord2d plc;
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && NUtils.getGameUI().map.player() != null)
        {
            Drawable drawable = (Drawable) NUtils.getGameUI().map.player().getattr(Drawable.class);
            if (drawable != null)
            {
                String pose;
                if (drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && !NParser.checkName(pose, "borka/walking", "borka/running", "borka/wading"))
                    if(targetCoord.dist(NUtils.player().rc) < MCache.tileqsz.len()) {
                        return true;
                    }
                    else
                    {
                        if (wpf.ready.get())
                        {
                            nu = true;
                            return true;
                        }
                    }
                if (wpf.ready.get())
                    {
                        if(gob.rc.dist(old)> MCache.tilehsz.len()) {
                            nu = true;
                            return true;
                        }
                        else
                        {
                            try {
                                t.join();
                            } catch (InterruptedException e) {
                            }
                            wpf.ready.set(false);
                            (t = new Thread(wpf)).start();
                        }
                    }
            }
        }
        return false;
    }
    boolean nu = false;
    public boolean needUpdate()
    {
        return nu;
    }
}
