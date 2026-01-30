package nurgling.tasks;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.overlays.BuildGhostPreview;

public class SelectAreaWithLiveGhosts extends NTask {
    private final NHitBox originalHitBox;
    private final Indir<Resource> buildingResource;
    private final Message spriteData;
    private BuildGhostPreview ghostPreview = null;
    private NArea.Space result = null;
    private Gob player = null;
    private int rotationCount = 0; // 0, 1, 2, 3 for 0°, 90°, 180°, 270°
    private NHitBox currentHitBox;

    public SelectAreaWithLiveGhosts(NHitBox hitBox, Indir<Resource> resource) {
        this(hitBox, resource, Message.nil);
    }
    
    public SelectAreaWithLiveGhosts(NHitBox hitBox, Indir<Resource> resource, Message sdt) {
        this.originalHitBox = hitBox;
        this.currentHitBox = hitBox;
        this.buildingResource = resource;
        this.spriteData = sdt;
    }

    @Override
    public boolean check() {
        if (NUtils.getGameUI().map != null) {
            NMapView mapView = (NMapView) NUtils.getGameUI().map;
            
            // Check for rotation key (R)
            if (mapView.isAreaSelectionMode.get() && mapView.ui != null) {
                // Check if R key was pressed (key code 82)
                if (mapView.ui.modflags() == 0 && checkRotationKey()) {
                    rotationCount = (rotationCount + 1) % 4;
                    currentHitBox = getRotatedHitBox();
                    
                    // Update rotation for existing ghosts
                    if (ghostPreview != null) {
                        ghostPreview.updateRotation(rotationCount, currentHitBox);
                    }
                }
            }

            // Update ghost preview if area is being selected IN PROGRESS
            if (mapView.isAreaSelectionMode.get() && mapView.selection != null) {
                // Get current selection coordinates from Selector
                Pair<Coord2d, Coord2d> currentArea = getCurrentSelectionArea(mapView);
                
                if (currentArea != null) {
                    if (ghostPreview == null && player == null) {
                        player = NUtils.player();
                        if (player != null) {
                            ghostPreview = new BuildGhostPreview(player, currentArea, currentHitBox, buildingResource, rotationCount, spriteData);
                            player.setattr(ghostPreview);
                        }
                    } else if (ghostPreview != null) {
                        ghostPreview.update(currentArea);
                    }
                }
            }
            
            // Also check if area was just completed (for final update)
            if (mapView.isAreaSelectionMode.get() && mapView.areaSpace != null) {
                Pair<Coord2d, Coord2d> currentArea = convertAreaToCoords(mapView.areaSpace);

                if (ghostPreview == null && player == null) {
                    player = NUtils.player();
                if (player != null && currentArea != null) {
                        ghostPreview = new BuildGhostPreview(player, currentArea, currentHitBox, buildingResource, rotationCount, spriteData);
                        player.setattr(ghostPreview);
                    }
                } else if (ghostPreview != null && currentArea != null) {
                    ghostPreview.update(currentArea);
                }
            }

            // Check if selection is complete (area selected)
            if (!mapView.isAreaSelectionMode.get() && mapView.areaSpace != null) {
                result = mapView.areaSpace;
                mapView.areaSpace = null;  // Reset areaSpace for next selection

                // Create ghost preview with final area if not yet created
                Pair<Coord2d, Coord2d> finalArea = convertAreaToCoords(result);

                if (ghostPreview == null && player == null) {
                    player = NUtils.player();
                }
                if (player != null && finalArea != null) {
                    if (ghostPreview != null) {
                        // Update existing preview with final area
                        ghostPreview.update(finalArea);
                    } else {
                        // Create new preview with final area
                        ghostPreview = new BuildGhostPreview(player, finalArea, currentHitBox, buildingResource, rotationCount, spriteData);
                        player.setattr(ghostPreview);
                    }
                }

                return true;
            }
        }
        return false;
    }

    public NArea.Space getAreaSpace() {
        return result;
    }
    
    public int getRotationCount() {
        return rotationCount;
    }
    
    private boolean checkRotationKey() {
        NMapView mapView = (NMapView) NUtils.getGameUI().map;
        if (mapView.rotationRequested) {
            mapView.rotationRequested = false;  // Reset flag
            return true;
        }
        return false;
    }
    
    private NHitBox getRotatedHitBox() {
        NHitBox box = originalHitBox;
        for (int i = 0; i < rotationCount; i++) {
            box = box.rotate();
        }
        return box;
    }

    /**
     * Get current selection area from active Selector (during selection process)
     */
    private Pair<Coord2d, Coord2d> getCurrentSelectionArea(NMapView mapView) {
        try {
            if (mapView.currentSelectionCoords != null) {
                Coord c1 = mapView.currentSelectionCoords.a;
                Coord c2 = mapView.currentSelectionCoords.b;
                
                // These are already absolute tile coordinates from Selector
                // Just convert to world coordinates
                Coord2d begin = c1.mul(MCache.tilesz);
                Coord2d end = c2.sub(1, 1).mul(MCache.tilesz).add(MCache.tilesz);
                return new Pair<>(begin, end);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private Pair<Coord2d, Coord2d> convertAreaToCoords(NArea.Space space) {
        if (space == null || space.space == null) {
            return null;
        }
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
