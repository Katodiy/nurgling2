package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import static nurgling.actions.PathFinder.pfmdelta;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

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

        if(!NParser.checkName(NUtils.getCursorName(), "arw")) {
            NUtils.getGameUI().map.wdgmsg("click", Coord.z, NUtils.player().rc.floor(posres),3, 0);
            NUtils.getUI().core.addTask(new GetCurs("arw"));
        }

        gui.msg(targetCoord.toString());
        gui.map.wdgmsg("click", Coord.z, targetCoord.floor(posres), 1, 0);
        Following fl = NUtils.player().getattr(Following.class);
        if( fl!= null )
        {
            Gob gob = null;
            if((gob = Finder.findGob(fl.tgt))!=null) {
                if (NParser.isIt(gob, new NAlias("horse"))) {
                    NUtils.getUI().core.addTask(new IsPoseMov(targetCoord, gob, new NAlias("gfx/kritter/horse/pace", "gfx/kritter/horse/walking", "gfx/kritter/horse/trot", "gfx/kritter/horse/gallop")));
                    NUtils.getUI().core.addTask(new IsNotPose(gob, new NAlias("gfx/kritter/horse/pace", "gfx/kritter/horse/walking", "gfx/kritter/horse/trot", "gfx/kritter/horse/gallop")));
                }
                else if (NParser.isIt(gob, new NAlias("dugout"))) {
                    NUtils.getUI().core.addTask(new IsPoseMov(targetCoord, NUtils.player(), new NAlias("gfx/borka/dugoutrowan")));
                    NUtils.getUI().core.addTask(new IsNotPose(NUtils.player(), new NAlias("gfx/borka/dugoutrowan")));
                }
                else if (NParser.isIt(gob, new NAlias("rowboat"))) {
                    NUtils.getUI().core.addTask(new IsPoseMov(targetCoord, NUtils.player(), new NAlias("gfx/borka/rowing")));
                    NUtils.getUI().core.addTask(new IsNotPose(NUtils.player(), new NAlias("gfx/borka/rowing")));
                }
            }
        }
        else {
            NUtils.getUI().core.addTask(new IsMoving(targetCoord));
            NUtils.getUI().core.addTask(new MovingCompleted(targetCoord));
        }
        if(NUtils.getGameUI().map.player().rc.dist(targetCoord) > 2*pfmdelta)
            return Results.FAIL();
        return Results.SUCCESS();
    }
}
