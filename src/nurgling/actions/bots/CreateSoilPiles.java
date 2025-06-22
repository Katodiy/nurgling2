package nurgling.actions.bots;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class CreateSoilPiles implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea outsa;
        NUtils.getGameUI().msg("Please, select input area");
        (outsa = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);
        new CreateFreePiles(outsa.getRCArea(),new NAlias("Soil"),new NAlias("gfx/terobjs/stockpile-soil")).run(gui);
        return Results.SUCCESS();
    }
}
