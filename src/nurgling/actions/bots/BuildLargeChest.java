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
import nurgling.tools.NAlias;

public class BuildLargeChest implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
        Build.Command command = new Build.Command();
        command.name = "Large Chest";

        NUtils.getGameUI().msg("Please, select build area");
        SelectAreaWithPreview buildarea = new SelectAreaWithPreview(Resource.loadsimg("baubles/buildArea"), "Large Chest");
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