package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.res.gfx.terobjs.roastspit.Roastspit;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FriedFish implements Action {

    NAlias powname = new NAlias(new ArrayList<String>(List.of("gfx/terobjs/pow")));


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area with raw fish");
        (insa = new SelectArea()).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please select area for results");
        (outsa = new SelectArea()).run(gui);

        SelectArea powsa;
        NUtils.getGameUI().msg("Please select area with fireplaces");
        (powsa = new SelectArea()).run(gui);


        ArrayList<Gob> allPow = Finder.findGobs(powsa.getRCArea(), powname);
        ArrayList<Gob> pows = new ArrayList<>();
        for (Gob gob : allPow) {
            if ((gob.ngob.getModelAttribute() & 48) == 0) {
                Gob.Overlay ol = (gob.findol(Roastspit.class));
                if (ol != null) {
                    pows.add(gob);
                }
            }
        }
        pows.sort(NUtils.d_comp);
        new FillFuelPow(pows,1).run(gui);
        if(!new LightGob(pows, 4).run(gui).IsSuccess())
            return Results.ERROR("I can't start a fire");
        return Results.SUCCESS();
    }
}
