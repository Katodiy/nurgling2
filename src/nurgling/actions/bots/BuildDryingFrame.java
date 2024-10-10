package nurgling.actions.bots;

import haven.Coord;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.tools.NAlias;

public class BuildDryingFrame implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Build.Command command = new Build.Command();
        command.name = "Drying Frame";

        NUtils.getGameUI().msg("Please, select input area");
        SelectArea buildarea = new SelectArea();
        buildarea.run(NUtils.getGameUI());

        NUtils.getGameUI().msg("Please, select output area for branch");
        SelectArea brancharea = new SelectArea();
        brancharea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),brancharea.getRCArea(),new NAlias("Branch"),5));

        NUtils.getGameUI().msg("Please, select output area for bough");
        SelectArea bougharea = new SelectArea();
        bougharea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(2,1),bougharea.getRCArea(),new NAlias("Bough"),2));

        NUtils.getGameUI().msg("Please, select output area for strings");
        SelectArea stringarea = new SelectArea();
        stringarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),stringarea.getRCArea(),new NAlias("Flax Fibres", "Hemp Fibres", "Spindly Taproot", "Cattail Fibres", "Stinging Nettle", "Hide Strap", "Straw Twine", "Bark Cordage"),2));

        new Build(command, buildarea.getRCArea()).run(gui);
        return Results.SUCCESS();
    }
}
