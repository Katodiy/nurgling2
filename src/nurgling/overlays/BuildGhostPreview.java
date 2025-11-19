package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.GhostAlpha;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.pf.NHitBoxD;

import java.awt.Color;
import java.util.*;

/**
 * Ghost preview overlay showing where buildings will be placed using real Gobs with models
 * Similar to BlueprintPlob but for buildings
 */
public class BuildGhostPreview extends GAttrib {
    private Pair<Coord2d, Coord2d> area;
    private NHitBox buildingHitBox;
    private Indir<Resource> buildingResource;
    private List<Gob> ghostGobs = new ArrayList<>();
    private Glob glob;
    private double rotationAngle = 0.0;  // Rotation angle in radians

    public BuildGhostPreview(Gob owner, Pair<Coord2d, Coord2d> area, NHitBox hitBox, Indir<Resource> resource) {
        super(owner);
        this.area = area;
        this.buildingHitBox = hitBox;
        this.buildingResource = resource;
        this.glob = owner.glob;
        this.rotationAngle = 0.0;
        if (area != null && hitBox != null && resource != null) {
            calculateGhostPositions();
        }
    }
    
    public BuildGhostPreview(Gob owner, Pair<Coord2d, Coord2d> area, NHitBox hitBox, Indir<Resource> resource, int rotationCount) {
        super(owner);
        this.area = area;
        this.buildingHitBox = hitBox;
        this.buildingResource = resource;
        this.glob = owner.glob;
        this.rotationAngle = (rotationCount * Math.PI / 2.0);  // Convert rotation count to radians
        if (area != null && hitBox != null && resource != null) {
            calculateGhostPositions();
        }
    }

    /**
     * Calculate all valid building positions using the same logic as Finder.getFreePlace()
     */
    private void calculateGhostPositions() {
        // Clean up existing ghosts
        removeGhosts();

        if (buildingHitBox == null || area == null || buildingResource == null) {
            return;
        }

        // Find all obstacles in the area (same as Finder.getFreePlace)
        ArrayList<NHitBoxD> obstacles = findObstacles();

        // Track placed buildings to avoid showing overlaps
        ArrayList<NHitBoxD> placedBuildings = new ArrayList<>();

        Coord inchMax = area.b.sub(area.a).floor();
        Coord margin = buildingHitBox.end.sub(buildingHitBox.begin).floor(2, 2);

        // Simulate Finder.getFreePlace() behavior: pixel-by-pixel search
        for (int i = margin.x; i <= inchMax.x - margin.x; i++) {
            for (int j = margin.y; j <= inchMax.y - margin.y; j++) {
                Coord2d testPos = area.a.add(i, j);
                NHitBoxD testBox = new NHitBoxD(buildingHitBox.begin, buildingHitBox.end, testPos, 0);

                // Check collisions with obstacles AND already-placed buildings
                boolean passed = true;

                for (NHitBoxD obstacle : obstacles) {
                    if (obstacle.intersects(testBox, false)) {
                        passed = false;
                        break;
                    }
                }

                if (passed) {
                    for (NHitBoxD placed : placedBuildings) {
                        if (placed.intersects(testBox, false)) {
                            passed = false;
                            break;
                        }
                    }
                }

                if (passed) {
                    // This position is valid - create a ghost Gob
                    Coord2d worldPos = new Coord2d(testBox.rc.x, testBox.rc.y);
                    createGhostGob(worldPos);

                    // Add this building to placed list so we don't overlap it
                    placedBuildings.add(new NHitBoxD(buildingHitBox.begin, buildingHitBox.end, testPos, 0));
                }
            }
        }
    }

    /**
     * Create a ghost Gob at the specified position with building model
     */
    private void createGhostGob(Coord2d worldPos) {
        try {
            Gob ghost = new Gob(glob, worldPos);
            ghost.a = rotationAngle;  // Set rotation angle
            
            // Add ghost effect (blue transparency) before adding to OCache
            ghost.setattr(new GhostAlpha(ghost));
            
            synchronized (ghostGobs) {
                ghostGobs.add(ghost);
            }
            glob.oc.add(ghost);  // Synchronous add

            // Load resource asynchronously
            glob.loader.defer(() -> {
                ghost.setattr(new ResDrawable(ghost, buildingResource, Message.nil));
                return null;
            });
        } catch (Exception e) {
            // Silently ignore if can't create
        }
    }

    /**
     * Remove all ghost Gobs
     */
    private void removeGhosts() {
        List<Gob> toRemove;
        synchronized (ghostGobs) {
            toRemove = new ArrayList<>(ghostGobs);
            ghostGobs.clear();
        }
        
        // Synchronous remove with exception handling
        for (Gob ghost : toRemove) {
            try {
                glob.oc.remove(ghost);
            } catch (Exception e) {
                // Ignore concurrent modification - will be handled by dispose if needed
            }
        }
    }

    /**
     * Find obstacles in area (same logic as Finder.getFreePlace)
     */
    private ArrayList<NHitBoxD> findObstacles() {
        ArrayList<NHitBoxD> obstacles = new ArrayList<>();
        NHitBoxD areaBox = new NHitBoxD(area.a, area.b);

        try {
            synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
                for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                    if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() ||
                          gob.getClass().getName().contains("GlobEffector"))) {
                        if (gob.ngob.hitBox != null && gob.getattr(Following.class) == null &&
                            gob.id != NUtils.player().id) {
                            NHitBoxD gobBox = new NHitBoxD(gob);
                            if (gobBox.intersects(areaBox, true)) {
                                obstacles.add(gobBox);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle exceptions finding obstacles
        }

        return obstacles;
    }

    public void dispose() {
        // Use deferred removal on dispose to avoid ConcurrentModificationException
        List<Gob> toRemove;
        synchronized (ghostGobs) {
            toRemove = new ArrayList<>(ghostGobs);
            ghostGobs.clear();
        }
        
        if (!toRemove.isEmpty() && glob != null && glob.loader != null) {
            glob.loader.defer(() -> {
                for (Gob ghost : toRemove) {
                    try {
                        glob.oc.remove(ghost);
                    } catch (Exception e) {
                        // Silently ignore
                    }
                }
                return null;
            });
        }
    }

    /**
     * Check if the preview needs to be updated
     */
    public boolean needsUpdate(Pair<Coord2d, Coord2d> newArea) {
        if (newArea != null && !newArea.equals(this.area)) {
            return true;
        }
        return false;
    }

    /**
     * Update preview when area changes
     */
    public void update(Pair<Coord2d, Coord2d> newArea) {
        if (newArea != null && !newArea.equals(this.area)) {
            this.area = newArea;
            if (area != null && buildingHitBox != null && buildingResource != null) {
                calculateGhostPositions();
            }
        }
    }
    
    /**
     * Update rotation angle and recalculate positions with new hitbox
     */
    public void updateRotation(int rotationCount, NHitBox newHitBox) {
        this.rotationAngle = (rotationCount * Math.PI / 2.0);
        this.buildingHitBox = newHitBox;
        
        // Recalculate ghost positions with new hitbox and rotation
        if (area != null && buildingHitBox != null && buildingResource != null) {
            calculateGhostPositions();
        }
    }
}
