package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.overlays.BuildGhostPreview;
import nurgling.overlays.NCustomBauble;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class BuildSmokeShed implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
        Build.Command command = new Build.Command();
        command.name = "Smoke Shed";

        NUtils.getGameUI().msg("Please, select build area");
        SelectAreaWithLiveGhosts buildarea = new SelectAreaWithLiveGhosts(Resource.loadsimg("baubles/buildArea"), "Smoke Shed");
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
            command.ingredients.add(new Build.Ingredient(new Coord(1, 1), thatchingarea.getRCArea(), new NAlias("Straw", "Reeds", "Glimmermoss", "Tarsticks", "Brown Kelp"), 6));
        }
        NUtils.getGameUI().msg("Please, select area for brick");
        SelectArea brickarea = new SelectArea(Resource.loadsimg("baubles/bricks"));
        brickarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1), brickarea.getRCArea(),new NAlias("Brick"),10));

        new Build(command, buildarea.getRCArea()).run(gui);
        return Results.SUCCESS();
        } finally {
            // Always clean up ghost preview when bot finishes or is interrupted
            Gob player = NUtils.player();
            if (player != null) {
                BuildGhostPreview ghostPreview = player.getattr(BuildGhostPreview.class);
                if (ghostPreview != null) {
                    ghostPreview.dispose();
                    player.delattr(BuildGhostPreview.class);
                }

                // Remove custom bauble overlay
                Gob.Overlay baubleOverlay = player.findol(NCustomBauble.class);
                if (baubleOverlay != null) {
                    baubleOverlay.remove();
                }

                // Clean up area selection mode
                if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                    ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.set(false);
                }
            }
        }
    }
}
