package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NHitBox;
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

public class BuildMoundBed implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            Build.Command command = new Build.Command();
            command.name = "Mound Bed";
            command.windowName = "Moundbed"; // Window name is different from menu name
            NContext context = new NContext(gui);

            // Get the custom hitbox we defined
            NHitBox moundBedHitBox = NHitBox.findCustom("gfx/terobjs/moundbed");
            if (moundBedHitBox == null) {
                return Results.ERROR("Custom hitbox for Mound Bed not found");
            }

            // Set custom hitbox in command so Build action can use it
            command.customHitBox = moundBedHitBox;

            NUtils.getGameUI().msg("Please, select build area");
            // Pass custom hitbox to SelectAreaWithLiveGhosts since plob won't have it
            SelectAreaWithLiveGhosts buildarea = new SelectAreaWithLiveGhosts(context, Resource.loadsimg("baubles/buildArea"), "Mound Bed", moundBedHitBox);
            buildarea.run(NUtils.getGameUI());

            // Use BuildMaterialHelper for auto-zone lookup
            BuildMaterialHelper helper = new BuildMaterialHelper(context, gui);
            
            // Mulch (12)
            command.ingredients.add(helper.getIngredient(
                new Coord(1, 1),
                new NAlias("Mulch"),
                12,
                "baubles/mulchArea",
                "Please, select area for mulch"
            ));
            
            // Straw (6)
            command.ingredients.add(helper.getIngredient(
                new Coord(1, 1),
                new NAlias("Straw"),
                6,
                "baubles/strawArea",
                "Please, select area for straw"
            ));

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
