package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitGobModelAttr;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class LightGob implements Action
{
    ArrayList<Gob> gobs;

    int flame_flag;

    public LightGob(ArrayList<Gob> gobs, int flame_flag) {
        this.gobs = gobs;
        this.flame_flag = flame_flag;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob candelabrum = Finder.findGob(new NAlias("gfx/terobjs/candelabrum"));
        if(candelabrum == null || candelabrum.ngob.getModelAttribute() !=3 )
            candelabrum = null;
        if(candelabrum!=null) {
            Coord2d pos = new Coord2d(candelabrum.rc.x, candelabrum.rc.y);
            new LiftObject(candelabrum).run(gui);
            for (Gob gob : gobs) {
                if ((gob.ngob.getModelAttribute() & flame_flag) == 0) {
                    new PathFinder(gob).run(gui);
//                    NUtils.activateGob ( gob );
//                    NUtils.getUI().core.addTask(new WaitGobModelAttr(gob,flame_flag));
                }
            }
            new PlaceObject(candelabrum, pos, 0).run(gui);
        }
        else
        {
            return Results.FAIL();
        }
        return Results.SUCCESS();
    }
}
