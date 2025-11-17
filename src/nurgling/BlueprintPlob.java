package nurgling;

import haven.*;
import haven.render.*;
import nurgling.overlays.BlueprintOverlay;

import java.util.*;

public class BlueprintPlob implements MapView.Grabber {
    private final Map<Coord, String> blueprintData;
    private final int width;
    private final int height;
    private MCache.Overlay mapOverlay;
    private Coord currentTilePos = Coord.z;
    private boolean placed = false;
    private Glob glob;
    private MapView mapView;
    private List<Gob> ghostTrees = new ArrayList<>();
    private boolean ghostsCreated = false;

    public BlueprintPlob(MapView mapView, Map<Coord, String> blueprintData, int width, int height) {
        this.mapView = mapView;
        this.glob = mapView.glob;
        this.blueprintData = blueprintData;
        this.width = width;
        this.height = height;
        createMapOverlay();
        
        // Register as grabber to intercept mouse clicks
        mapView.grab(this);
    }

    private void createMapOverlay() {
        Area area = new Area(new Coord(0, 0), new Coord(width, height));
        mapOverlay = glob.map.new Overlay(area, BlueprintOverlay.blueprintol);
    }
    
    private void createGhostTrees() {
        for (Map.Entry<Coord, String> entry : blueprintData.entrySet()) {
            Coord gridPos = entry.getKey();
            String treeType = entry.getValue();
            
            // Convert minimap path to regular resource path
            String resPath = treeType.replace("/mm/", "/");
            
            // Create ghost tree Gob at absolute position
            Coord2d worldPos = currentTilePos.mul(MCache.tilesz).add(
                gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
            );
            
            Gob ghost = new Gob(glob, worldPos);
            ghostTrees.add(ghost);
            glob.oc.add(ghost);
            
            // Add ghost effect (blue transparency)
            ghost.setattr(new GhostAlpha(ghost));
            
            // Load resource asynchronously like Plob does
            Indir<Resource> res = Resource.remote().load(resPath);
            
            // Use Loader.defer with Supplier that will be called repeatedly until success
            glob.loader.defer(() -> {
                // This will be called repeatedly by loader until it succeeds
                ghost.setattr(new ResDrawable(ghost, res, Message.nil));
                return null; // Return value doesn't matter
            });
        }
    }


    public void adjustPosition(Coord pc, Coord2d mc) {
        Coord newTilePos = mc.floor(MCache.tilesz);
        
        if (!newTilePos.equals(currentTilePos)) {
            currentTilePos = newTilePos;
            
            // Create ghosts on first position update
            if (!ghostsCreated && !currentTilePos.equals(Coord.z)) {
                createGhostTrees();
                ghostsCreated = true;
            }
            
            updateOverlayPosition();
        }
    }

    private void updateOverlayPosition() {
        if (mapOverlay != null) {
            Coord min = currentTilePos;
            Coord max = currentTilePos.add(width, height);
            Area area = new Area(min, max);
            mapOverlay.update(area);
            updateGhostPositions();
        }
    }
    
    private void updateGhostPositions() {
        int i = 0;
        for (Map.Entry<Coord, String> entry : blueprintData.entrySet()) {
            if (i >= ghostTrees.size()) break;
            
            Coord gridPos = entry.getKey();
            Gob ghost = ghostTrees.get(i);
            
            Coord2d worldPos = currentTilePos.mul(MCache.tilesz).add(
                gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
            );
            
            ghost.move(worldPos, 0);
            i++;
        }
    }


    public Coord2d getPosition() {
        // Return world position (center of tile) like Plob
        return currentTilePos.mul(MCache.tilesz).add(MCache.tilesz.div(2));
    }

    public Map<Coord, String> getBlueprintData() {
        return blueprintData;
    }

    public void place() {
        placed = true;
        
        // Release grabber immediately after placing
        if (mapView != null) {
            mapView.release(this);
        }
    }

    public boolean isPlaced() {
        return placed;
    }
    
    public boolean isActive() {
        return mapOverlay != null;
    }

    public void remove() {
        if (mapOverlay != null) {
            mapOverlay.destroy();
            mapOverlay = null;
        }
        // Remove ghost trees
        for (Gob ghost : ghostTrees) {
            glob.oc.remove(ghost);
        }
        ghostTrees.clear();
        
        if (mapView != null) {
            mapView.release(this);
        }
    }
    
    // Grabber interface implementation
    @Override
    public boolean mmousedown(Coord mc, int button) {
        if (button == 1) {
            place();
            return true;
        } else if (button == 3) {
            placed = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mmouseup(Coord mc, int button) {
        return false;
    }
    
    @Override
    public boolean mmousewheel(Coord mc, int amount) {
        return false;
    }
    
    @Override
    public void mmousemove(Coord mc) {
        // Position updates are handled in WaitBlueprintPlacement check() method
    }
}
