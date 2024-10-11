package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.Follow;
import nurgling.tasks.FollowAndPose;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import static haven.OCache.posres;

public class UseWorkStationNC implements Action
{
    public UseWorkStationNC(Gob workstation)
    {
        this.workstation = workstation;
    }

    Gob workstation;
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        Gob ws = workstation;
        if(ws == null)
            return Results.ERROR("NO WORKSTATION");
        else
        {
            new PathFinder(ws).run(gui);
            gui.map.wdgmsg ( "click", Coord.z, ws.rc.floor ( posres ), 3, 0, 0, ( int ) ws.id,
                    ws.rc.floor ( posres ), 0, -1 );
            NUtils.getUI().core.addTask(new Follow(NUtils.player()));
        }
        return Results.SUCCESS();
    }
}
