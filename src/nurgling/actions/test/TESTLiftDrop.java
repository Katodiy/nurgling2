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
        this.num = 1;
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {

        Gob trough = Finder.findGob(new NAlias("gfx/kritter/cattle/calf"));
        Coord2d pos = trough.rc;
        new LiftObject(trough).run(gui);
        new PlaceObject(trough, pos).run(gui);
    }
}
