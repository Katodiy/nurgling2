package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.pf.*;

public class PathFinder implements Action
{
    NPFMap pfmap;

    public PathFinder(Coord2d begin, Coord2d end)
    {
        pfmap = new NPFMap(new Coord2d(Math.min(begin.x,end.x),Math.min(begin.y,end.y)),new Coord2d(Math.max(begin.x,end.x),Math.max(begin.y,end.y)));
        pfmap.build();
    }

    PathFinder(Coord2d end)
    {
        this(NUtils.getGameUI().map.player().rc,end);
    }

    PathFinder(Gob target)
    {
        this(target.rc);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

        return Results.SUCCESS();
    }
}
