package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.UI;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tasks.WaitCheckable;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitPrepBlocksState;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectBark implements Action {

    NAlias ntrees = new NAlias(new ArrayList<String>(List.of("gfx/terobjs/tree")),new ArrayList<String>(Arrays.asList("log", "oldtrunk", "stump")));


    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg("Please select area with trees");
        (insa = new SelectArea()).run(gui);

        SelectArea outsa;
        NUtils.getGameUI().msg("Please select area for piles");
        (outsa = new SelectArea()).run(gui);

        ArrayList<Gob> trees = Finder.findGobs(insa.getRCArea(),ntrees);
        trees.sort(NUtils.d_comp);
        for(Gob tree : trees)
        {

            new CollectFromGob(tree,"Take bark", "gfx/borka/treepickan",new Coord(1,1),new NAlias("Bark", "bark"),outsa.getRCArea()).run(gui);
        }
        new TransferToPiles(outsa.getRCArea(), new NAlias("Bark", "bark") ).run(gui);
        return Results.SUCCESS();
    }
}
