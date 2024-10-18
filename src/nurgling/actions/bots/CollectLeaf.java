package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.CollectFromGob;
import nurgling.actions.Results;
import nurgling.actions.TransferToPiles;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectLeaf implements Action {

    NAlias ntrees = new NAlias(new ArrayList<String>(List.of("gfx/terobjs/tree", "gfx/terobjs/bushes/teabush")),new ArrayList<String>(Arrays.asList("log", "oldtrunk", "stump")));


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
            String pickAction = tree.ngob.name.contains("tree") ? "gfx/borka/treepickan" : "gfx/borka/bushpickan";
            new CollectFromGob(tree,"Pick leaf", pickAction,new Coord(1,1),new NAlias("Leaf", "leaf", "Leaves", "leaves"),outsa.getRCArea()).run(gui);
        }
        new TransferToPiles(outsa.getRCArea(), new NAlias("Leaf", "leaf","Leaves", "leaves") ).run(gui);
        return Results.SUCCESS();
    }
}
