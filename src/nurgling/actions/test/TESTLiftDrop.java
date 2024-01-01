package nurgling.actions.test;

import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.LiftObject;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.PathFinder;
import nurgling.actions.PlaceObject;
import nurgling.areas.NArea;
import nurgling.tasks.ChangeModelAtrib;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

/*
* Lift/Place test
* */

public class TESTLiftDrop extends Test
{

    public TESTLiftDrop()
    {
        this.num = 100;
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {

        Gob trough = Finder.findGob(new NAlias("gfx/terobjs/trough"));
        Coord2d pos = trough.rc;
        Gob cistern  = Finder.findGob(new NAlias("gfx/terobjs/cistern"));
        new LiftObject(trough).run(gui);
        new PathFinder ( cistern ).run(gui);
        NUtils.activateGob ( cistern );
        NUtils.getUI().core.addTask(new ChangeModelAtrib(trough, 7));
        new PlaceObject(trough, pos).run(gui);
        new LiftObject(trough).run(gui);
        new PathFinder ( cistern ).run(gui);
        NUtils.activateGob ( cistern );
        NUtils.getUI().core.addTask(new ChangeModelAtrib(trough, 0));
        new PlaceObject(trough, pos).run(gui);
    }
}
