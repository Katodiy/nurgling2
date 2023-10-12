package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import nurgling.tasks.*;

public class GoTo implements Action
{
    final Coord2d targetCoord;

    public GoTo(Coord2d targetCoord)
    {
        this.targetCoord = targetCoord;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        gui.map.wdgmsg("click", Coord.z, targetCoord.floor(posres), 1, 0);
        NUtils.getUI().core.addTask(new IsMoving(targetCoord));
        NUtils.getUI().core.addTask(new MovingCompleted(targetCoord));
        return Results.SUCCESS();
    }
}
