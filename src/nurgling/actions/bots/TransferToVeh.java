package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.conf.NCarrierProp;
import nurgling.tasks.WaitCheckable;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class TransferToVeh implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Carrier w = null;
        NCarrierProp prop = null;
        try {
            NUtils.getUI().core.addTask(new WaitCheckable( NUtils.getGameUI().add((w = new nurgling.widgets.bots.Carrier()), UI.scale(200,200))));
            prop = w.prop;
        }
        catch (InterruptedException e)
        {
            throw e;
        }
        finally {
            if(w!=null)
                w.destroy();
        }
        if(prop == null)
        {
            return Results.ERROR("No config");
        }



        SelectArea insa;
        NUtils.getGameUI().msg("Please, select input area");
        (insa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);

        SelectGob selgob;
        NUtils.getGameUI().msg("Please select output cart or vehicle");
        (selgob = new SelectGob(Resource.loadsimg("baubles/inputVeh"))).run(gui);
        Gob target = selgob.result;
        if(target==null)
        {
            return Results.ERROR("Vehicle not found");
        }

        IsVehicleFull ivf = new IsVehicleFull(selgob.result);
        ivf.run(gui);
        int maxCount = ivf.getCount();

        int count = 0;
        ArrayList<Gob> gobs;
        while (!(gobs = Finder.findGobs(insa.getRCArea(), new NAlias(prop.object))).isEmpty() && count < maxCount) {
            ArrayList<Gob> availableLogs = new ArrayList<>();
            for (Gob currGob: gobs)
            {
                if(PathFinder.isAvailable(currGob))
                    availableLogs.add(currGob);
            }
            if(availableLogs.isEmpty())
                return Results.ERROR("Cant reach any object");

            availableLogs.sort(NUtils.d_comp);
            Gob log = availableLogs.get(0);
            new LiftObject(log).run(gui);
            new TransferToVehicle(log, selgob.result).run(gui);
            count++;
        }

        return Results.SUCCESS();
    }
}
