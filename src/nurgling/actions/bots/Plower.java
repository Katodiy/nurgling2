package nurgling.actions.bots;

import haven.Gob;
import haven.Resource;
import haven.Widget;
import haven.Window;
import haven.res.lib.itemtex.ItemTex;
import haven.res.ui.barterbox.Shopbox;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import org.json.JSONArray;
import org.json.JSONObject;

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
