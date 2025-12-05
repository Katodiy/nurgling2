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
    ArrayList<String> gobs;

    int flame_flag;

    public LightGob(ArrayList<String> gobs, int flame_flag) {
        this.gobs = gobs;
        this.flame_flag = flame_flag;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        boolean timeFoWork = false;
        for (String gobHash : gobs) {
            Gob gob = Finder.findGob(gobHash);
            if (gob != null && (gob.ngob.getModelAttribute() & flame_flag) == 0) {
                timeFoWork = true;
                break;
            }
        }
        if(!timeFoWork) {
            return Results.SUCCESS();
        }

        // First, try to find a lit candelabrum (modelAttribute == 3)
        Gob litCandelabrum = null;
        ArrayList<Gob> candelabrums = Finder.findGobs(new NAlias("gfx/terobjs/candelabrum"));
        for(Gob candelabrum : candelabrums)
        {
            if (candelabrum != null && candelabrum.ngob.getModelAttribute() == 3) {
                litCandelabrum = candelabrum;
                break;
            }
        }
        
        // If we found a lit candelabrum, use it for lighting
        if (litCandelabrum != null) {
            Coord2d pos = new Coord2d(litCandelabrum.rc.x, litCandelabrum.rc.y);
            new LiftObject(litCandelabrum).run(gui);
            for (String gobHash : gobs)
            {
                Gob gob = Finder.findGob(gobHash);
                if (gob != null && (gob.ngob.getModelAttribute() & flame_flag) == 0)
                {
                    new PathFinder(gob).run(gui);
                    NUtils.activateGob(gob);
                    NUtils.getUI().core.addTask(new WaitGobModelAttr(gob, flame_flag));
                }
            }
            new PlaceObject(litCandelabrum, pos, 0).run(gui);
            return Results.SUCCESS();
        }
        
        // Alternative method: use branches to light fire
        // This is used when no lit candelabrum is available
        gui.msg("No lit candelabrum found, using alternative fire lighting method with branches");
        
        for (String gobHash : gobs)
        {
            Gob gob = Finder.findGob(gobHash);
            if (gob != null && (gob.ngob.getModelAttribute() & flame_flag) == 0)
            {
                // Use LightFire action with branches (no candelabrum)
                Results lightResult = new LightFire(gob).run(gui);
                if (!lightResult.IsSuccess()) {
                    gui.error("Failed to light fire on object: " + gob.ngob.name);
                    return lightResult;
                }
                
                Gob updatedGob = Finder.findGob(gob.id);
                if (updatedGob != null && (updatedGob.ngob.getModelAttribute() & flame_flag) == 0) {
                    // Fire not lit successfully
                    return Results.ERROR("Fire lighting failed - state did not change");
                }
            }
        }
        
        return Results.SUCCESS();
    }
}
