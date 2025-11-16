package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.MenuSearch;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WaitGobModelAttr;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class LightGob implements Action
{
    ArrayList<Long> gobs;

    int flame_flag;

    public LightGob(ArrayList<Long> gobs, int flame_flag) {
        this.gobs = gobs;
        this.flame_flag = flame_flag;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean timeFoWork = false;
        for (Long gobid : gobs) {
            Gob gob = Finder.findGob(gobid);
            if ((gob.ngob.getModelAttribute() & flame_flag) == 0) {
                timeFoWork = true;
                break;
            }
        }
        if(!timeFoWork) {
            return Results.SUCCESS();
        }

        ArrayList<Gob> candelabrums = Finder.findGobs(new NAlias("gfx/terobjs/candelabrum"));
        for(Gob candelabrum : candelabrums)
        {
            if (candelabrum == null || candelabrum.ngob.getModelAttribute() != 3)
                continue;
            Coord2d pos = new Coord2d(candelabrum.rc.x, candelabrum.rc.y);
            new LiftObject(candelabrum).run(gui);
            for (Long gobid : gobs)
            {
                Gob gob = Finder.findGob(gobid);
                if ((gob.ngob.getModelAttribute() & flame_flag) == 0)
                {
                    new PathFinder(gob).run(gui);
                    NUtils.activateGob(gob);
                    NUtils.getUI().core.addTask(new WaitGobModelAttr(gob, flame_flag));
                }
            }
            new PlaceObject(candelabrum, pos, 0).run(gui);
        }
        return Results.SUCCESS();
    }
}
