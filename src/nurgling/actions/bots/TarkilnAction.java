package nurgling.actions.bots;

import haven.*;
import nurgling.NFlowerMenu;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

import static nurgling.actions.bots.Butcher.order;

public class TarkilnAction implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        NArea.Specialisation tarkilnsa = new NArea.Specialisation(Specialisation.SpecName.tarkiln.toString());
        NArea area = NArea.findSpec(tarkilnsa);
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(tarkilnsa);

        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        Context context = new Context();

        NArea npile_area = NArea.findOut(new NAlias("coal"),1);
        Pair<Coord2d,Coord2d> pile_area = npile_area!=null?npile_area.getRCArea():null;
        if(pile_area==null)
        {
            NUtils.getGameUI().msg("Please select area for output coal");
            SelectArea onsa;
            (onsa = new SelectArea(Resource.loadsimg("baubles/coalPiles"))).run(gui);
            pile_area = onsa.getRCArea();
        }

        NUtils.getGameUI().msg("Please select area for fuel");
        SelectArea insa;
        (insa = new SelectArea(Resource.loadsimg("baubles/fuel"))).run(gui);

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            ArrayList<Gob> tarkilns = Finder.findGobs(area, new NAlias("gfx/terobjs/tarkiln"));
            ArrayList<Gob> forRemove = new ArrayList<>();
            for(Gob tarkiln : tarkilns) {
                if((tarkiln.ngob.getModelAttribute()&4)!=0)
                    forRemove.add(tarkiln);
            }
            tarkilns.removeAll(forRemove);

            for(Gob tarkiln : tarkilns) {
                new CollectFromGob(tarkiln, "Collect coal", "gfx/borka/bushpickan", true, new Coord(1, 1), 8, new NAlias("Coal"), pile_area).run(gui);
            }
            new TransferToPiles(pile_area, new NAlias("Coal")).run(gui);

            if(!new FillFuelTarkilns(tarkilns,insa.getRCArea()).run(gui).IsSuccess())
                return Results.FAIL();

            new LightGob(tarkilns,16).run(gui);
        }

        return Results.SUCCESS();
    }


}
