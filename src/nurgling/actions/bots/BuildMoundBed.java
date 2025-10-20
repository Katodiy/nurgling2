package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NHitBox;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Build;
import nurgling.actions.Results;
import nurgling.overlays.BuildGhostPreview;
import nurgling.overlays.NCustomBauble;
import nurgling.tools.NAlias;

public class BuildMoundBed implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            Build.Command command = new Build.Command();
            command.name = "Mound Bed";
            command.windowName = "Moundbed"; // Window name is different from menu name

            // Get the custom hitbox we defined
            NHitBox moundBedHitBox = NHitBox.findCustom("gfx/terobjs/moundbed");
            if (moundBedHitBox == null) {
                return Results.ERROR("Custom hitbox for Mound Bed not found");
            }

            // Set custom hitbox in command so Build action can use it
            command.customHitBox = moundBedHitBox;

            NUtils.getGameUI().msg("Please, select build area");
            SelectArea buildarea = new SelectArea(Resource.loadsimg("baubles/buildArea"));
            buildarea.run(NUtils.getGameUI());

            // Manually add ghost preview with our custom hitbox
            Pair<Coord2d, Coord2d> area = buildarea.getRCArea();
            Gob player = NUtils.player();
            if (player != null && area != null) {
                BuildGhostPreview ghostPreview = new BuildGhostPreview(
                    player,
                    area,
                    moundBedHitBox
                );
                player.addcustomol(ghostPreview);
            }

            NUtils.getGameUI().msg("Please, select area for mulch");
            SelectArea mulcharea = new SelectArea(Resource.loadsimg("baubles/custom"));
            mulcharea.run(NUtils.getGameUI());
            command.ingredients.add(new Build.Ingredient(new Coord(1,1), mulcharea.getRCArea(), new NAlias("Mulch"), 12));

            NUtils.getGameUI().msg("Please, select area for straw");
            SelectArea strawarea = new SelectArea(Resource.loadsimg("baubles/custom"));
            strawarea.run(NUtils.getGameUI());
            command.ingredients.add(new Build.Ingredient(new Coord(1,1), strawarea.getRCArea(), new NAlias("Straw"), 6));

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
