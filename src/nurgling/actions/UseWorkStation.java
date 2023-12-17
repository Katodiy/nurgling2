package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
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
        Gob ws = Finder.findGob(new NAlias(cnt.workstation.station));
        if(ws == null)
            return Results.ERROR("NO WORKSTATION");
        else
        {
            new PathFinder(ws).run(gui);
            gui.map.wdgmsg ( "click", Coord.z, ws.rc.floor ( posres ), 3, 0, 0, ( int ) ws.id,
                    ws.rc.floor ( posres ), 0, -1 );
            NUtils.getUI().core.addTask(new FollowAndPose(NUtils.player(),cnt.workstation.pose));
        }
        return Results.SUCCESS();
    }
}
