package nurgling.actions.bots;

import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class Destroyer implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea outsa;
        NUtils.getGameUI().msg("Please, select input area");
        (outsa = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);
        ArrayList<Gob> gobs = Finder.findGobs(outsa.getRCArea(), null);
        while (!gobs.isEmpty()) {
            gobs.sort(NUtils.d_comp);
            for (Gob gob : gobs) {
                if(PathFinder.isAvailable(gob)) {
                    PathFinder pf = new PathFinder(gob);
                    pf.isHardMode = true;
                    pf.run(gui);
                    while (Finder.findGob(gob.id) != null) {
                        new RestoreResources(gob).run(gui);
                        NUtils.destroy(gob);
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return NUtils.getEnergy() < 0.25 || NUtils.getStamina() < 0.25 || Finder.findGob(gob.id) == null;
                            }
                        });
                    }
                }
            }
            gobs = Finder.findGobs(outsa.getRCArea(), null);
        }
        return Results.SUCCESS();
    }
}
