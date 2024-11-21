package nurgling.actions.bots;

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

public class TransferFromVehToVeh implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        SelectGob selingob;
        NUtils.getGameUI().msg("Please select output cart or vehicle");
        (selingob = new SelectGob(Resource.loadsimg("baubles/outputVeh"))).run(gui);

        SelectGob seloutgob;
        NUtils.getGameUI().msg("Please select input cart or vehicle");
        (seloutgob = new SelectGob(Resource.loadsimg("baubles/inputVeh"))).run(gui);
        if(selingob.result==null || seloutgob.result==null)
        {
            return Results.ERROR("Vehicle not found");
        }

        IsVehicleFull ivf = new IsVehicleFull(seloutgob.result);
        ivf.run(gui);
        int maxCount = ivf.getCount();

        int count = 0;
        while ( count < maxCount && new TakeFromVehicle(selingob.result).run(gui).IsSuccess()) {
            Gob gob = Finder.findLiftedbyPlayer();
            new TransferToVehicle(gob, seloutgob.result).run(gui);
            count++;
        }

        return Results.SUCCESS();
    }
}
