package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Resource;

import java.util.ArrayList;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.overlays.BuildGhostPreview;
import nurgling.overlays.NCustomBauble;
import nurgling.tools.NAlias;
import nurgling.tools.VSpec;

public class BuildKiln implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
        Build.Command command = new Build.Command();
        command.name = "Kiln";

        NUtils.getGameUI().msg("Please, select build area");
    SelectAreaWithLiveGhosts buildarea = new SelectAreaWithLiveGhosts(Resource.loadsimg("baubles/buildArea"), "Kiln");
        buildarea.run(NUtils.getGameUI());

        NUtils.getGameUI().msg("Please, select area for clay");
        SelectArea clayarea = new SelectArea(Resource.loadsimg("baubles/clayPiles"));
        clayarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),clayarea.getRCArea(),new NAlias("Clay"),35));

        // Get ghost positions from BuildGhostPreview if available
        ArrayList<Coord2d> ghostPositions = null;
        BuildGhostPreview ghostPreview = null;
        Gob player = NUtils.player();
        if (player != null) {
            ghostPreview = player.getattr(BuildGhostPreview.class);
            if (ghostPreview != null) {
                ghostPositions = new ArrayList<>(ghostPreview.getGhostPositions());
            }
        }

        new Build(command, buildarea.getRCArea(), buildarea.getRotationCount(), ghostPositions, ghostPreview).run(gui);
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
