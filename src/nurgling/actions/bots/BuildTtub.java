package nurgling.actions.bots;

import haven.Coord;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.tools.NAlias;

public class BuildTtub implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Build.Command command = new Build.Command();
        command.name = "Tanning Tub";

        NUtils.getGameUI().msg("Please, select build area");
        SelectArea buildarea = new SelectArea(Resource.loadsimg("baubles/buildArea"));
        buildarea.run(NUtils.getGameUI());

        NUtils.getGameUI().msg("Please, select area for board");
        SelectArea brancharea = new SelectArea(Resource.loadsimg("baubles/boardIng"));
        brancharea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(4,1),brancharea.getRCArea(),new NAlias("Board"),4));

        NUtils.getGameUI().msg("Please, select area for block");
        SelectArea bougharea = new SelectArea(Resource.loadsimg("baubles/blockIng"));
        bougharea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,2),bougharea.getRCArea(),new NAlias("Block"),2));


        new Build(command, buildarea.getRCArea()).run(gui);
        return Results.SUCCESS();
    }
}
