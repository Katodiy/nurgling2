package nurgling.actions.bots;

import haven.Coord;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.tools.NAlias;
import nurgling.overlays.BuildGhostPreview;
import haven.Gob;

public class BuildTtub implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
        Build.Command command = new Build.Command();
        command.name = "Tanning Tub";

        NUtils.getGameUI().msg("Please, select build area");
        SelectAreaWithPreview buildarea = new SelectAreaWithPreview(Resource.loadsimg("baubles/buildArea"), "Tanning Tub");
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
        } finally {
            // Always clean up ghost preview when bot finishes or is interrupted
            Gob player = NUtils.player();
            if (player != null) {
                Gob.Overlay ghostOverlay = player.findol(BuildGhostPreview.class);
                if (ghostOverlay != null) {
                    ghostOverlay.remove();
                }
            }
        }
    }
}
