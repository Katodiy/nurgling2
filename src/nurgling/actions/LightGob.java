package nurgling.actions;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitGobModelAttr;
import nurgling.tasks.WaitTargetSize;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

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
                    NUtils.activateGob ( gob );
                    NUtils.getUI().core.addTask(new WaitGobModelAttr(gob,2));
                }
            }
            new PlaceObject(candelabrum, pos, 0).run(gui);
        }
        return Results.SUCCESS();
    }
}
