package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.FindPlaceAndAction;
import nurgling.actions.LiftObject;
import nurgling.actions.Results;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TransferLog implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg("Please, select input area");
        (insa = new SelectArea()).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please, select output area");
        (outsa = new SelectArea()).run(gui);
        ArrayList<Gob> logs;
        while (!(logs = Finder.findGobs(insa.getRCArea(), new NAlias("log", "oldtrunk"))).isEmpty()) {
            logs.sort(NUtils.d_comp);
            Gob log = logs.get(0);
            new LiftObject(log).run(gui);
            new FindPlaceAndAction(log, outsa.getRCArea()).run(gui);
        }

        return Results.SUCCESS();
    }
}
