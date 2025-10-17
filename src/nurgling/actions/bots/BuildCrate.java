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
import nurgling.tools.VSpec;
import org.json.JSONObject;

import java.util.ArrayList;

public class BuildCrate implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        try {
            Build.Command command = new Build.Command();
            command.name = "Crate";

            NUtils.getGameUI().msg("Please, select build area");
            SelectAreaWithPreview buildarea = new SelectAreaWithPreview(Resource.loadsimg("baubles/buildArea"), "Crate");
            buildarea.run(NUtils.getGameUI());

            NUtils.getGameUI().msg("Please, select area for board");
            SelectArea brancharea = new SelectArea(Resource.loadsimg("baubles/boardIng"));
            brancharea.run(NUtils.getGameUI());
            command.ingredients.add(new Build.Ingredient(new Coord(4,1),brancharea.getRCArea(),new NAlias("Board"),4));


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
