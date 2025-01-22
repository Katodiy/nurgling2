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

public class TransferFromVeh implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        SelectArea outsa;
        NUtils.getGameUI().msg("Please, select output area");
        (outsa = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);

        SelectGob selgob;
//        NUtils.getGameUI().msg("Please select output cart or vehicle");
//        (selgob = new SelectGob(Resource.loadsimg("baubles/outputVeh"))).run(gui);
        Gob s = Finder.findGob(new NAlias("cart"));
        Gob target = s;//selgob.result;
        if(target==null)
        {
            return Results.ERROR("Vehicle not found");
        }

        while (new TakeFromVehicle(s).run(gui).IsSuccess()) {
            Gob gob = Finder.findLiftedbyPlayer();
            new FindPlaceAndAction(gob, outsa.getRCArea()).run(gui);
            Coord2d shift = gob.rc.sub(NUtils.player().rc).norm().mul(2);
            new GoTo(NUtils.player().rc.sub(shift)).run(gui);
        }

        return Results.SUCCESS();
    }
}
