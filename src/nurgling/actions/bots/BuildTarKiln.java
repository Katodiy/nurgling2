package nurgling.actions.bots;

import haven.Coord;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.tools.NAlias;
import nurgling.tools.VSpec;

public class BuildTarKiln implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Build.Command command = new Build.Command();
        command.name = "Tar Kiln";

        NUtils.getGameUI().msg("Please, select build area");
        SelectArea buildarea = new SelectArea(Resource.loadsimg("baubles/buildArea"));
        buildarea.run(NUtils.getGameUI());

        NUtils.getGameUI().msg("Please, select area for stone");
        SelectArea stonearea = new SelectArea(Resource.loadsimg("baubles/chipperPiles"));
        stonearea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),stonearea.getRCArea(), VSpec.getNamesInCategory("Stone"),35));

        NUtils.getGameUI().msg("Please, select area for clay");
        SelectArea clayarea = new SelectArea(Resource.loadsimg("baubles/clayPiles"));
        clayarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),clayarea.getRCArea(),new NAlias("Clay"),50));


        new Build(command, buildarea.getRCArea()).run(gui);
        return Results.SUCCESS();
    }
}
