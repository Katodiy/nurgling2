package nurgling.actions.bots;

import haven.Coord;
import haven.Gob;
import haven.Coord2d;
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
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class BuildSmokeShed implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            Build.Command command = new Build.Command();
            command.name = "Smoke Shed";
            NContext context = new NContext(gui);

            NUtils.getGameUI().msg("Please, select build area");
            SelectAreaWithLiveGhosts buildarea = new SelectAreaWithLiveGhosts(context, Resource.loadsimg("baubles/buildArea"), "Smoke Shed");
            buildarea.run(NUtils.getGameUI());

            // Use BuildMaterialHelper for auto-zone lookup
            BuildMaterialHelper helper = new BuildMaterialHelper(context, gui);
            
            // Board (12)
            command.ingredients.add(helper.getBoards(12));
            
            // Block (4)
            command.ingredients.add(helper.getBlocks(4));
            
            // Thatching material (6) - try thatch first, then boughs
            NAlias thatchAlias = new NAlias("Straw", "Reeds", "Glimmermoss", "Tarsticks", "Brown Kelp");
            NAlias boughAlias = new NAlias("Bough");
            if (helper.hasZone(thatchAlias)) {
                command.ingredients.add(helper.getIngredient(
                    new Coord(1, 1),
                    thatchAlias,
                    6,
                    "baubles/tatching",
                    "Please, select area for thatching material"
                ));
            } else if (helper.hasZone(boughAlias)) {
                command.ingredients.add(helper.getIngredient(
                    new Coord(2, 1),
                    boughAlias,
                    6,
                    "baubles/tatching",
                    "Please, select area for boughs"
                ));
            } else {
                // Fallback - ask user
                NUtils.getGameUI().msg("Please, select area for thatching material");
                String areaId = context.createArea("Thatching", Resource.loadsimg("baubles/tatching"));
                nurgling.areas.NArea thatchingArea = context.getAreaById(areaId);
                if (Finder.findGob(thatchingArea.getRCArea(), new NAlias("stockpile-bough")) != null) {
                    command.ingredients.add(new Build.Ingredient(new Coord(2, 1), thatchingArea, boughAlias, 6));
                } else {
                    command.ingredients.add(new Build.Ingredient(new Coord(1, 1), thatchingArea, thatchAlias, 6));
                }
            }
            
            // Brick (10)
            command.ingredients.add(helper.getBricks(10));

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
