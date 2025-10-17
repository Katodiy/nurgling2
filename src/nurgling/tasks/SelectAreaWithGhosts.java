package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.overlays.TrellisGhostPreview;

public class SelectAreaWithGhosts extends NTask {
    private final NHitBox hitBox;
    private final boolean[] rotationRef;
    private final boolean[] confirmRef;
    private TrellisGhostPreview ghostPreview = null;
    private NArea.Space result = null;
    private Gob player = null;
    private boolean waitingForConfirm = false;

    public SelectAreaWithGhosts(NHitBox hitBox, boolean[] rotationRef, boolean[] confirmRef) {
        this.hitBox = hitBox;
        this.rotationRef = rotationRef;
        this.confirmRef = confirmRef;
    }

    @Override
    public boolean check() {
        if (NUtils.getGameUI().map != null) {
            NMapView mapView = (NMapView) NUtils.getGameUI().map;

            // Update ghost preview if area is being selected
            if (mapView.isAreaSelectionMode.get() && mapView.areaSpace != null) {
                Pair<Coord2d, Coord2d> currentArea = convertAreaToCoords(mapView.areaSpace);

                if (ghostPreview == null && player == null) {
                    player = NUtils.player();
                    if (player != null) {
                        ghostPreview = new TrellisGhostPreview(player, currentArea, rotationRef[0], hitBox);
                        player.addcustomol(ghostPreview);
                    }
                } else if (ghostPreview != null) {
                    ghostPreview.update(currentArea, rotationRef[0]);
                }
            }

            // Check if selection is complete (area selected)
            if (!mapView.isAreaSelectionMode.get() && !waitingForConfirm) {
                if (mapView.areaSpace != null) {
                    result = mapView.areaSpace;
                    // Don't clear areaSpace yet - keep it for ghost preview updates
                    waitingForConfirm = true;

                    // Create ghost preview with final area
                    Pair<Coord2d, Coord2d> finalArea = convertAreaToCoords(result);

                    if (ghostPreview == null && player == null) {
                        player = NUtils.player();
                    }
                    if (player != null && finalArea != null) {
                        if (ghostPreview != null) {
                            // Update existing preview
                            ghostPreview.update(finalArea, rotationRef[0]);
                        } else {
                            // Create new preview
                            ghostPreview = new TrellisGhostPreview(player, finalArea, rotationRef[0], hitBox);
                            player.addcustomol(ghostPreview);
                        }
                    }
                }
            }

            // Wait for user confirmation
            if (waitingForConfirm) {
                // Update ghost preview if rotation changes
                if (ghostPreview != null && result != null && player != null) {
                    Pair<Coord2d, Coord2d> finalArea = convertAreaToCoords(result);

                    // Check if rotation changed - if so, need to recreate sprite
                    if (ghostPreview.needsUpdate(finalArea, rotationRef[0])) {
                        // Remove old sprite
                        Gob.Overlay ol = player.findol(TrellisGhostPreview.class);
                        if (ol != null) {
                            ol.remove();
                        }

                        // Create new sprite with updated rotation
                        ghostPreview = new TrellisGhostPreview(player, finalArea, rotationRef[0], hitBox);
                        player.addcustomol(ghostPreview);
                    }
                }

                // Check if confirmed
                if (confirmRef != null && confirmRef[0]) {
                    // Clean up ghost preview
                    if (ghostPreview != null && player != null) {
                        Gob.Overlay ol = player.findol(TrellisGhostPreview.class);
                        if (ol != null) {
                            ol.remove();
                        }
                        ghostPreview = null;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public NArea.Space getResult() {
        return result;
    }

    private Pair<Coord2d, Coord2d> convertAreaToCoords(NArea.Space space) {
        Coord begin = null;
        Coord end = null;
        for (Long id : space.space.keySet()) {
            MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(id);
            if (grid != null) {
                haven.Area area = space.space.get(id).area;
                Coord b = area.ul.add(grid.ul);
                Coord e = area.br.add(grid.ul);
                begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
                end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
            }
        }
        if (begin != null) {
            return new Pair<>(begin.mul(MCache.tilesz), end.sub(1, 1).mul(MCache.tilesz).add(MCache.tilesz));
        }
        return null;
    }
}
