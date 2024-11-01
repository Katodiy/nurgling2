package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class BuildSmokeShed implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Build.Command command = new Build.Command();
        command.name = "Smoke Shed";

        NUtils.getGameUI().msg("Please, select build area");
        SelectArea buildarea = new SelectArea(Resource.loadsimg("baubles/buildArea"));
        buildarea.run(NUtils.getGameUI());

        NUtils.getGameUI().msg("Please, select area for board");
        SelectArea boardarea = new SelectArea(Resource.loadsimg("baubles/boardIng"));
        boardarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(4,1),boardarea.getRCArea(),new NAlias("Board"),12));

        NUtils.getGameUI().msg("Please, select area for block");
        SelectArea blockarea = new SelectArea(Resource.loadsimg("baubles/blockIng"));
        blockarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,2),blockarea.getRCArea(),new NAlias("Block"),4));

        NUtils.getGameUI().msg("Please, select area for thatching material");
        SelectArea thatchingarea = new SelectArea(Resource.loadsimg("baubles/tatching"));
        thatchingarea.run(NUtils.getGameUI());
        if (Finder.findGob(thatchingarea.getRCArea(), new NAlias("stockpile-bough"))!= null) {
            command.ingredients.add(new Build.Ingredient(new Coord(2, 1), thatchingarea.getRCArea(), new NAlias("Bough"), 6));
        }else{
            command.ingredients.add(new Build.Ingredient(new Coord(1, 1), thatchingarea.getRCArea(), new NAlias("Straw", "Reeds", "Glimmermoss", "Tarsticks"), 6));
        }
        NUtils.getGameUI().msg("Please, select area for brick");
        SelectArea brickarea = new SelectArea(Resource.loadsimg("baubles/bricks"));
        brickarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1), brickarea.getRCArea(),new NAlias("Brick"),10));

        new Build(command, buildarea.getRCArea()).run(gui);
        return Results.SUCCESS();
    }
}
