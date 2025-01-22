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
import java.util.List;

public class CountGobs implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        SelectGob selgob;
        NUtils.getGameUI().msg("Please select a gob");
        (selgob = new SelectGob(Resource.loadsimg("baubles/outputVeh"))).run(gui);
        Gob target = selgob.result;
        if(target==null)
        {
            return Results.ERROR("Gob not found");
        }

        ArrayList<Gob> result = Finder.findGobs(new NAlias(target.ngob.name));
        if(result.size() != 0){
            gui.msg("There are " + result.size() + " objects in the area.");
        }


        return Results.SUCCESS();
    }
}
