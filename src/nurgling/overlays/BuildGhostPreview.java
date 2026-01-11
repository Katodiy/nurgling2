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
    private Message spriteData;
    private List<Gob> ghostGobs = new ArrayList<>();
    private Glob glob;
    private double rotationAngle = 0.0;  // Rotation angle in radians

    public BuildGhostPreview(Gob owner, Pair<Coord2d, Coord2d> area, NHitBox hitBox, Indir<Resource> resource) {
        this(owner, area, hitBox, resource, 0, Message.nil);
    }
    
    public BuildGhostPreview(Gob owner, Pair<Coord2d, Coord2d> area, NHitBox hitBox, Indir<Resource> resource, int rotationCount) {
        this(owner, area, hitBox, resource, rotationCount, Message.nil);
    }
    
    public BuildGhostPreview(Gob owner, Pair<Coord2d, Coord2d> area, NHitBox hitBox, Indir<Resource> resource, int rotationCount, Message sdt) {
        super(owner);
        this.area = area;
        this.buildingHitBox = hitBox;
        this.buildingResource = resource;
        this.spriteData = (sdt != null) ? sdt : Message.nil;
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
            if (buildingResource == null) {
                return;
            }
            
            Gob ghost = new Gob(glob, worldPos);
            ghost.a = rotationAngle;
            
            ghost.setattr(new GhostAlpha(ghost));
            ghost.setattr(new ResDrawable(ghost, buildingResource, spriteData));
            
            synchronized (ghostGobs) {
                ghostGobs.add(ghost);
            }
            
            glob.oc.add(ghost);
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
        System.out.println("[BuildGhostPreview] dispose() called, removing " + ghostGobs.size() + " ghosts");
        List<Gob> toRemove;
        synchronized (ghostGobs) {
            toRemove = new ArrayList<>(ghostGobs);
            ghostGobs.clear();
        }
        
        if (toRemove.isEmpty()) {
            System.out.println("[BuildGhostPreview] No ghosts to remove");
            return;
        }
        
        // Always do immediate removal first
        for (Gob ghost : toRemove) {
            try {
                if (glob != null && glob.oc != null) {
                    glob.oc.remove(ghost);
                }
            } catch (Exception e) {
                System.out.println("[BuildGhostPreview] Error removing ghost: " + e.getMessage());
            }
        }
        
        // Also schedule deferred removal as backup
        if (glob != null && glob.loader != null) {
            final List<Gob> deferredRemove = new ArrayList<>(toRemove);
            glob.loader.defer(() -> {
                for (Gob ghost : deferredRemove) {
                    try {
                        glob.oc.remove(ghost);
                    } catch (Exception e) {
                        // Already removed or error - ignore
                    }
                }
                return null;
            });
        }
        
        System.out.println("[BuildGhostPreview] dispose() completed");
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
    
    public ArrayList<Coord2d> getGhostPositions() {
        ArrayList<Coord2d> positions = new ArrayList<>();
        synchronized (ghostGobs) {
            for (Gob ghost : ghostGobs) {
                positions.add(ghost.rc);
            }
        }
        System.out.println("[BuildGhostPreview] getGhostPositions() returning " + positions.size() + " positions");
        return positions;
    }
    
    public void removeGhost(Coord2d pos) {
        synchronized (ghostGobs) {
            System.out.println("[BuildGhostPreview] removeGhost called for position: " + pos + ", total ghosts: " + ghostGobs.size());
            Gob toRemove = null;
            double minDist = Double.MAX_VALUE;
            // Find the closest ghost within tolerance
            for (Gob ghost : ghostGobs) {
                double dist = ghost.rc.dist(pos);
                if (dist < 5.0 && dist < minDist) {  // Increased tolerance to 5 units
                    minDist = dist;
                    toRemove = ghost;
                }
            }
            if (toRemove != null) {
                ghostGobs.remove(toRemove);
                System.out.println("[BuildGhostPreview] Removed ghost at " + toRemove.rc + " (dist=" + minDist + "), remaining: " + ghostGobs.size());
                try {
                    glob.oc.remove(toRemove);
                } catch (Exception e) {
                    System.out.println("[BuildGhostPreview] Error removing ghost from glob.oc: " + e.getMessage());
                }
            } else {
                System.out.println("[BuildGhostPreview] No ghost found near position " + pos);
            }
        }
    }
}
