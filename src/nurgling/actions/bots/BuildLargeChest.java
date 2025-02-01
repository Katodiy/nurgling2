package nurgling.actions.bots;

import haven.Coord;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.tools.NAlias;

public class BuildLargeChest implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Build.Command command = new Build.Command();
        command.name = "Large Chest";

        NUtils.getGameUI().msg("Please, select build area");
        SelectArea buildarea = new SelectArea(Resource.loadsimg("baubles/buildArea"));
        buildarea.run(NUtils.getGameUI());

        // Boards (5)
        NUtils.getGameUI().msg("Please, select area for boards");
        SelectArea boardarea = new SelectArea(Resource.loadsimg("baubles/boardIng"));
        boardarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(4,1), boardarea.getRCArea(), new NAlias("Board"), 5));

        // Metal Bars (2)
        NUtils.getGameUI().msg("Please, select area for metal bars");
        SelectArea bararea = new SelectArea(Resource.loadsimg("baubles/mbars"));
        bararea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1), bararea.getRCArea(), new NAlias("Bar of Bronze", "Bar of Cast Iron", "Bar of Wrought Iron"), 2));

        // Leather (4)
        NUtils.getGameUI().msg("Please, select area for leather");
        SelectArea leatherarea = new SelectArea(Resource.loadsimg("baubles/leather"));
        leatherarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1), leatherarea.getRCArea(), new NAlias("Leather"), 4));

        // Rope (2)
        NUtils.getGameUI().msg("Please, select area for rope");
        SelectArea ropearea = new SelectArea(Resource.loadsimg("baubles/rope"));
        ropearea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,2), ropearea.getRCArea(), new NAlias("Rope"), 2));

        NUtils.getGameUI().msg("Please, select area for bone glue");
        SelectArea gluearea = new SelectArea(Resource.loadsimg("baubles/glue"));
        gluearea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1), gluearea.getRCArea(), new NAlias("Bone Glue"), 3));

        new Build(command, buildarea.getRCArea()).run(gui);
        return Results.SUCCESS();
    }
}