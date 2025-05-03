package nurgling.actions.bots;

import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tools.NAlias;

public class Plower implements Action
{


    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        SelectArea outsa;
        NUtils.getGameUI().msg("Please select area for plowing");
        (outsa = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);

        new PatrolArea(new NAlias( "vehicle/plow" ), outsa.getRCArea() ).run(gui);

        return Results.SUCCESS();
    }


}
