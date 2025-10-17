package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.overlays.NCustomBauble;
import nurgling.overlays.TrellisGhostPreview;
import nurgling.widgets.TrellisDirectionDialog;

import java.awt.image.BufferedImage;

public class SelectAreaWithRotation implements Action {

    public SelectAreaWithRotation(BufferedImage image, NHitBox hitBox) {
        this.image = image;
        this.trellisHitBox = hitBox;
    }

    BufferedImage image = null;
    BufferedImage spr = null;
    NHitBox trellisHitBox = null;
    public NArea.Space result;
    public int orientation = 0; // 0=NS-East, 1=NS-West, 2=EW-North, 3=EW-South
    private TrellisDirectionDialog dirDialog = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (!((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.get()) {
            Gob player = NUtils.player();
            ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.set(true);

            // Add direction dialog to the UI
            dirDialog = new TrellisDirectionDialog();
            gui.add(dirDialog, UI.scale(200, 200));

            if(image!=null && player!=null) {
                player.addcustomol(new NCustomBauble(player,image, spr,((NMapView) NUtils.getGameUI().map).isAreaSelectionMode));
            }

            // Use appropriate task based on whether we have a hitbox (for ghost previews)
            if (trellisHitBox != null) {
                // Create orientation reference array that can be updated by the dialog
                int[] orientationRef = new int[] { orientation };
                boolean[] confirmRef = new boolean[] { false };
                dirDialog.setReferences(orientationRef, confirmRef);
                dirDialog.show();
                dirDialog.raise();

                nurgling.tasks.SelectAreaWithGhosts sa;
                NUtils.getUI().core.addTask(sa = new nurgling.tasks.SelectAreaWithGhosts(trellisHitBox, orientationRef, confirmRef));
                if (sa.getResult() != null) {
                    result = sa.getResult();
                    orientation = orientationRef[0];
                }
            } else {
                nurgling.tasks.SelectArea sa;
                NUtils.getUI().core.addTask(sa = new nurgling.tasks.SelectArea());
                if (sa.getResult() != null) {
                    result = sa.getResult();
                    orientation = 0;
                }
            }

            // Clean up dialog
            if(dirDialog != null) {
                dirDialog.reqdestroy();
                dirDialog = null;
            }
        }
        else {
            return Results.FAIL();
        }
        return Results.SUCCESS();
    }

    public Pair<Coord2d,Coord2d> getRCArea() {
        Coord begin = null;
        Coord end = null;
        for (Long id : result.space.keySet()) {
            MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(id);
            haven.Area area = result.space.get(id).area;
            Coord b = area.ul.add(grid.ul);
            Coord e = area.br.add(grid.ul);
            begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
            end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
        }
        if (begin != null)
            return new Pair<Coord2d, Coord2d>(begin.mul(MCache.tilesz), end.sub(1, 1).mul(MCache.tilesz).add(MCache.tilesz));
        return null;
    }

    public boolean getRotation() {
        // For backward compatibility: orientation 2 and 3 are East-West
        return orientation >= 2;
    }
}
