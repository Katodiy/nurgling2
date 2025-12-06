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

        NUtils.getGameUI().msg("Please, select build area");
        SelectAreaWithLiveGhosts buildarea = new SelectAreaWithLiveGhosts(Resource.loadsimg("baubles/buildArea"), "Drying Frame");
        buildarea.run(NUtils.getGameUI());

        NUtils.getGameUI().msg("Please, select area for branch");
        SelectArea brancharea = new SelectArea(Resource.loadsimg("baubles/branchStart"));
        brancharea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),brancharea.getRCArea(),new NAlias("Branch"),5));

        NUtils.getGameUI().msg("Please, select area for bough");
        SelectArea bougharea = new SelectArea(Resource.loadsimg("baubles/boughStart"));
        bougharea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(2,1),bougharea.getRCArea(),new NAlias("Bough"),2));

        NUtils.getGameUI().msg("Please, select area for strings");
        SelectArea stringarea = new SelectArea(Resource.loadsimg("baubles/stringsIng"));
        stringarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1),stringarea.getRCArea(),new NAlias("Flax Fibres", "Hemp Fibres", "Spindly Taproot","Grass Twine", "Cattail Fibres", "Stinging Nettle", "Hide Strap", "Straw Twine", "Bark Cordage"),2));

        // Get ghost positions from BuildGhostPreview if available
        ArrayList<Coord2d> ghostPositions = null;
        BuildGhostPreview ghostPreview = null;
        Gob player = NUtils.player();
        if (player != null) {
            ghostPreview = player.getattr(BuildGhostPreview.class);
            if (ghostPreview != null) {
                ghostPositions = new ArrayList<>(ghostPreview.getGhostPositions());
                NUtils.getGameUI().msg("Found " + ghostPositions.size() + " ghost positions");
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
