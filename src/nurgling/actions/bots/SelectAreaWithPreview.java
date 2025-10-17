package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.overlays.BuildGhostPreview;
import nurgling.tasks.WaitPlob;

import java.awt.image.BufferedImage;

/**
 * SelectArea that shows a ghost preview of building placements
 * Activates the build menu to get hitbox, shows preview, then cancels placement
 */
public class SelectAreaWithPreview extends SelectArea {
    private String buildingName;

    public SelectAreaWithPreview(BufferedImage image, String buildingName) {
        super(image);
        this.buildingName = buildingName;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // First, do the normal area selection
        Results result = super.run(gui);

        if (!result.IsSuccess()) {
            return result;
        }

        // Now activate build menu to get hitbox
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
            if (pag.button() != null && pag.button().name().equals(buildingName)) {
                pag.button().use(new MenuGrid.Interaction(1, 0));
                break;
            }
        }

        if(NUtils.getGameUI().map.placing == null) {
            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                if (pag.button() != null && pag.button().name().equals(buildingName)) {
                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }
        }

        NUtils.addTask(new WaitPlob());
        MapView.Plob plob = NUtils.getGameUI().map.placing.get();

        // Determine if rotation is needed based on area shape
        Pair<Coord2d, Coord2d> area = getRCArea();
        boolean needRotate = (Math.abs(area.b.x - area.a.x) < Math.abs(area.b.y - area.a.y));

        // Show ghost preview
        Gob player = NUtils.player();
        if (player != null && area != null) {
            BuildGhostPreview ghostPreview = new BuildGhostPreview(
                player,
                area,
                needRotate ? plob.ngob.hitBox.rotate() : plob.ngob.hitBox
            );
            player.addcustomol(ghostPreview);
        }

        // Cancel placement cursor - will be reactivated during building
        NUtils.getGameUI().map.placing = null;

        return Results.SUCCESS();
    }
}
