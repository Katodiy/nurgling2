package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.areas.NContext;
import nurgling.overlays.BuildGhostPreview;
import nurgling.overlays.NCustomBauble;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class BuildDryingFrame implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            Build.Command command = new Build.Command();
            command.name = "Drying Frame";
            NContext context = new NContext(gui);

            NUtils.getGameUI().msg("Please, select build area");
            SelectAreaWithLiveGhosts buildarea = new SelectAreaWithLiveGhosts(context, Resource.loadsimg("baubles/buildArea"), "Drying Frame");
            buildarea.run(NUtils.getGameUI());

            // Use BuildMaterialHelper for auto-zone lookup
            BuildMaterialHelper helper = new BuildMaterialHelper(context, gui);
            
            // Branch (5)
            command.ingredients.add(helper.getIngredient(
                new Coord(1, 1),
                new NAlias("Branch"),
                5,
                "baubles/branchStart",
                "Please, select area for branch"
            ));
            
            // Bough (2)
            command.ingredients.add(helper.getIngredient(
                new Coord(2, 1),
                new NAlias("Bough"),
                2,
                "baubles/boughStart",
                "Please, select area for bough"
            ));
            
            // Strings (2)
            command.ingredients.add(helper.getStrings(2));

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
            
            new Build(context, command, buildarea.ghostArea, buildarea.getRotationCount(), ghostPositions, ghostPreview).run(gui);
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
