package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import static nurgling.actions.PathFinder.pfmdelta;
import nurgling.tasks.*;

public class GoTo implements Action
{
    final Coord2d targetCoord;

    public GoTo(Coord2d targetCoord)
    {
        this.targetCoord = targetCoord.add(MCache.tileqsz);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        gui.map.wdgmsg("click", Coord.z, targetCoord.floor(posres), 1, 0);
        NUtils.getUI().core.addTask(new IsMoving(targetCoord));
        NUtils.getUI().core.addTask(new MovingCompleted(targetCoord));
        if(NUtils.getGameUI().map.player().rc.dist(targetCoord) > 2*pfmdelta)
            return Results.FAIL();
        return Results.SUCCESS();
    }
}
