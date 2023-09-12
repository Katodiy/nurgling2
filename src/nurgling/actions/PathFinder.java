package nurgling.actions;

import haven.*;
import nurgling.*;

public class PathFinder implements Action
{
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        NUtils.moveTo(Coord2d.z);
        return Results.SUCCESS();
    }
}
