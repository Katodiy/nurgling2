package nurgling.actions;

import haven.*;
import static haven.OCache.posres;

import haven.render.sl.BinOp;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;

public class UseWorkStation implements Action
{
    public UseWorkStation(Context context)
    {
        this.cnt = context;
    }

    Context cnt;
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        Gob ws = (cnt.workstation.selected==null)?Finder.findGob(new NAlias(cnt.workstation.station)):cnt.workstation.selected;
        if(ws == null)
            return Results.ERROR("NO WORKSTATION");
        else
        {
            new PathFinder(ws).run(gui);
            if(cnt.workstation.station.contains("gfx/terobjs/pow") || cnt.workstation.station.contains("gfx/terobjs/cauldron"))
            {
                new SelectFlowerAction("Open",cnt.workstation.selected).run(gui);
                if(cnt.workstation.station.contains("gfx/terobjs/pow"))
                {
                    NUtils.addTask(new WaitWindow("Fireplace"));
                }
                else
                {
                    NUtils.addTask(new WaitWindow("Cauldron"));
                }
            }
            else {
                gui.map.wdgmsg("click", Coord.z, ws.rc.floor(posres), 3, 0, 0, (int) ws.id,
                        ws.rc.floor(posres), 0, -1);
                if (cnt.workstation.pose != null)
                    NUtils.getUI().core.addTask(new FollowAndPose(NUtils.player(), cnt.workstation.pose));
                else {
                    NUtils.getUI().core.addTask(new IsMoving(ws.rc, 50));
                    NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
                }
            }
        }
        return Results.SUCCESS();
    }
}
