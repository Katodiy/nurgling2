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
    private int rotation = 0; // 0, 1, 2, 3 representing 0°, 90°, 180°, 270°

    public BlueprintPlob(MapView mapView, Map<Coord, String> blueprintData, int width, int height) {
        this.mapView = mapView;
        this.glob = mapView.glob;
        this.blueprintData = blueprintData;
        this.width = width;
        this.height = height;
        
        // Preload all tree resources before showing blueprint
        preloadTreeResources();
        
        createMapOverlay();
        
        // Register as grabber to intercept mouse clicks
        mapView.grab(this);
    }

    private void preloadTreeResources() {
        // Collect unique tree types
        Set<String> uniqueTreeTypes = new HashSet<>();
        for (String treeType : blueprintData.values()) {
            uniqueTreeTypes.add(treeType);
        }
        
        // Preload all possible fallen fruit resources for fruit trees
        // This fixes the issue where fruit trees fail to load because their fallen fruit resources aren't loaded
        String[] fruitResources = {
            "gfx/terobjs/items/applegreen-yester",
            "gfx/terobjs/items/apple-yester",
            "gfx/terobjs/items/cherry-yester",
            "gfx/terobjs/items/plum-yester",
            "gfx/terobjs/items/pear-yester",
            "gfx/terobjs/items/mulberry-yester",
            "gfx/terobjs/items/quince-yester",
            "gfx/terobjs/items/medlar-yester",
            "gfx/terobjs/items/sorb-yester",
            "gfx/terobjs/items/fig-yester",
            "gfx/terobjs/items/olive-yester",
            "gfx/terobjs/items/lemon-yester",
            "gfx/terobjs/items/orange-yester",
            "gfx/terobjs/items/almond-yester",
            "gfx/terobjs/items/walnut-yester",
            "gfx/terobjs/items/chestnut-yester"
        };
        
        for (String fruitRes : fruitResources) {
            try {
                Resource.remote().loadwait(fruitRes);
            } catch (Exception e) {
                // Ignore if fruit doesn't exist
            }
        }
        
        // Load all tree resources
        for (String treeType : uniqueTreeTypes) {
            try {
                String resPath = treeType.replace("/mm/", "/");
                Resource.remote().loadwait(resPath);
            } catch (Exception e) {
                // Ignore loading errors
            }
        }
    }
    
    private void createMapOverlay() {
        Coord size = getRotatedSize();
        Area area = new Area(new Coord(0, 0), size);
        mapOverlay = glob.map.new Overlay(area, BlueprintOverlay.blueprintol);
    }
    
    private void createGhostTrees() {
        for (Map.Entry<Coord, String> entry : blueprintData.entrySet()) {
            Coord gridPos = entry.getKey();
            String treeType = entry.getValue();
            
            // Convert minimap path to regular resource path
            String resPath = treeType.replace("/mm/", "/");
            
            // Apply rotation to grid position
            Coord rotatedGridPos = rotateCoord(gridPos);
            
            // Create ghost tree Gob at absolute position
            Coord2d worldPos = currentTilePos.mul(MCache.tilesz).add(
                rotatedGridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                rotatedGridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
            );
            
            try {
                Gob ghost = new Gob(glob, worldPos);
                ghostTrees.add(ghost);
                
                // Add ghost effect (blue transparency)
                ghost.setattr(new GhostAlpha(ghost));
                
                // Resource should already be preloaded, so load() will return quickly
                Indir<Resource> res = Resource.remote().load(resPath);
                ghost.setattr(new ResDrawable(ghost, res, Message.nil));
                
                // Add to world after attributes are set
                glob.oc.add(ghost);
            } catch (Exception e) {
                // Silently ignore ghost creation errors
            }
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
            Coord size = getRotatedSize();
            Coord max = currentTilePos.add(size);
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
            
            // Apply rotation to grid position
            Coord rotatedGridPos = rotateCoord(gridPos);
            
            Coord2d worldPos = currentTilePos.mul(MCache.tilesz).add(
                rotatedGridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                rotatedGridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
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
        rotate(amount);
        return true;
    }
    
    @Override
    public void mmousemove(Coord mc) {
        // Position updates are handled in WaitBlueprintPlacement check() method
    }
    
    public void rotate(int amount) {
        rotation = (rotation + amount) % 4;
        if (rotation < 0) rotation += 4;
        
        // Update overlay with new size
        updateOverlayPosition();
        
        // Update ghost positions with new rotation
        if (ghostsCreated) {
            updateGhostPositions();
        }
    }
    
    private Coord rotateCoord(Coord original) {
        switch (rotation) {
            case 0: // 0° - no rotation
                return original;
            case 1: // 90° clockwise
                return new Coord(height - 1 - original.y, original.x);
            case 2: // 180°
                return new Coord(width - 1 - original.x, height - 1 - original.y);
            case 3: // 270° clockwise (90° counter-clockwise)
                return new Coord(original.y, width - 1 - original.x);
            default:
                return original;
        }
    }
    
    private Coord getRotatedSize() {
        // When rotated by 90° or 270°, width and height are swapped
        if (rotation == 1 || rotation == 3) {
            return new Coord(height, width);
        }
        return new Coord(width, height);
    }
}
