package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TransferLog implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg("Please, select input area");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please, select output area");
        (outsa = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);
        ArrayList<Gob> logs;
        while (!(logs = Finder.findGobs(insa.getRCArea(), new NAlias("log"))).isEmpty()) {
            ArrayList<Gob> availableLogs = new ArrayList<>();
            for (Gob currGob: logs)
            {
                if(PathFinder.isAvailable(currGob))
                    availableLogs.add(currGob);
            }
            if(availableLogs.isEmpty())
                return Results.ERROR("Cant reach any object");

            availableLogs.sort(NUtils.d_comp);
            Gob log = availableLogs.get(0);
            new LiftObject(log).run(gui);
            new FindPlaceAndAction(log, outsa.getRCArea()).run(gui);
            Coord2d shift = log.rc.sub(NUtils.player().rc).norm().mul(2);
            new GoTo(NUtils.player().rc.sub(shift)).run(gui);
        }

        return Results.SUCCESS();
    }
}
