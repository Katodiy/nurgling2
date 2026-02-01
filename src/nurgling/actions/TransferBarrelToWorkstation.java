package nurgling.actions;

import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.areas.NGlobalCoord;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

import static nurgling.areas.NContext.workstation_spec_map;
import static nurgling.tools.Finder.findLiftedbyPlayer;

public class TransferBarrelToWorkstation implements Action {
    NContext context;
    String item;

    public TransferBarrelToWorkstation(NContext context, String item) {
        this.context = context;
        this.item = item;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob barrel = context.getBarrelInArea(item);
        if (barrel == null)
            return Results.FAIL();
        
        // Save original barrel position before moving
        Coord2d originalPos = barrel.rc;
        
        new LiftObject(barrel).run(gui);
        
        if (context.workstation != null) {
            // Get specialization name for this workstation type
            Specialisation.SpecName specName = workstation_spec_map.get(context.workstation.station);
            if (specName == null)
                return Results.ERROR("NO WORKSTATION SPEC MAPPING for " + context.workstation.station);
            
            // Find workstation area globally and navigate to it
            NArea area = context.getSpecArea(context.workstation);
            if (area == null) {
                area = NContext.findSpecGlobal(specName.toString());
            }
            if (area == null)
                return Results.ERROR("NO WORKSTATION AREA");
            
            // Navigate to the area using global pathfinding before placing
            // This works even if area is not currently visible
            NUtils.navigateToArea(area);
            
            // Select workstation if not already selected
            if (context.workstation.selected == -1) {
                
                Gob ws = null;
                if (context.workstation.station.startsWith("gfx/terobjs/pow")) {
                    ArrayList<Gob> gobs = Finder.findGobs(area, new NAlias("gfx/terobjs/pow"));
                    gobs.sort(NUtils.d_comp);
                    for (Gob gob : gobs) {
                        if ((gob.ngob.getModelAttribute() & 48) == 0) {
                            ws = gob;
                            break;
                        }
                    }
                } else {
                    ws = Finder.findGob(area, new NAlias(context.workstation.station));
                }
                if (ws != null)
                    context.workstation.selected = ws.id;
            }
            
            Gob ws = Finder.findGob(context.workstation.selected);
            if (ws == null)
                return Results.ERROR("NO WORKSTATION in area");
            
            // Use findLiftedbyPlayer() instead of barrelId since ID can change during location transitions
            Gob liftedBarrel = findLiftedbyPlayer();
            if (liftedBarrel == null || liftedBarrel.ngob.hitBox == null)
                return Results.ERROR("BARREL NOT FOUND - no lifted object");
            
            // Place barrel at diagonal position from workstation corner
            Coord2d placedPos = placeBarrelAtDiagonal(gui, ws, liftedBarrel);
            
            if (placedPos != null) {
                // Get updated barrel reference after placement
                Gob placedBarrel = findLiftedbyPlayer();
                if (placedBarrel == null) {
                    // Barrel was placed, find nearby barrel
                    ArrayList<Gob> nearbyBarrels = Finder.findGobs(new NAlias("barrel"));
                    for (Gob b : nearbyBarrels) {
                        if (b.rc.dist(placedPos) < 5) {
                            placedBarrel = b;
                            break;
                        }
                    }
                }
                if (placedBarrel != null) {
                    // Recalculate and store barrel hash after placement
                    // Hash may change after moving to new position
                    context.storeBarrelInfo(item, placedBarrel.ngob.hash, new NGlobalCoord(originalPos));
                    
                    // Find common approach point for workstation and barrel
                    Coord2d commonPoint = PathFinder.findNearestCommonApproachPoint(ws, placedBarrel);
                    if (commonPoint != null) {
                        context.workstation.targetPoint = new NGlobalCoord(commonPoint);
                    } else {
                        // Fallback: use placed position as target point
                        context.workstation.targetPoint = new NGlobalCoord(placedPos);
                    }
                }
            } else {
                return Results.ERROR("Could not find free diagonal position for barrel");
            }
        }
        return Results.SUCCESS();
    }
    
    /**
     * Place barrel at a diagonal position from one of the workstation corners.
     * Diagonal positions ensure the player can access both objects from a single point.
     * Uses findLiftedbyPlayer() instead of barrelId since ID can change during location transitions.
     * 
     * @return The position where barrel was placed, or null if no free position found
     */
    private Coord2d placeBarrelAtDiagonal(NGameUI gui, Gob ws, Gob barrel) throws InterruptedException {
        if (ws.ngob.hitBox == null) {
            return null;
        }
        
        Coord2d wsCenter = ws.rc;
        Coord2d wsBegin = ws.ngob.hitBox.begin;
        Coord2d wsEnd = ws.ngob.hitBox.end;
        Coord2d barrelEnd = barrel.ngob.hitBox.end;
        
        // Calculate corner positions of workstation (accounting for rotation if any)
        // Corners: top-left, top-right, bottom-right, bottom-left
        Coord2d[] corners = {
            new Coord2d(wsCenter.x + wsBegin.x, wsCenter.y + wsBegin.y),  // corner 0: min X, min Y
            new Coord2d(wsCenter.x + wsEnd.x, wsCenter.y + wsBegin.y),    // corner 1: max X, min Y
            new Coord2d(wsCenter.x + wsEnd.x, wsCenter.y + wsEnd.y),      // corner 2: max X, max Y
            new Coord2d(wsCenter.x + wsBegin.x, wsCenter.y + wsEnd.y)     // corner 3: min X, max Y
        };
        
        // Diagonal directions from each corner (pointing outward)
        Coord2d[] diagonalDirs = {
            new Coord2d(-1, -1).norm(),  // corner 0: toward -X, -Y
            new Coord2d(1, -1).norm(),   // corner 1: toward +X, -Y
            new Coord2d(1, 1).norm(),    // corner 2: toward +X, +Y
            new Coord2d(-1, 1).norm()    // corner 3: toward -X, +Y
        };
        
        // Offset distance: barrel size + small gap
        double offset = Math.max(barrelEnd.x, barrelEnd.y) + 3.0;
        
        // Sort corners by distance to player (try nearest first)
        Coord2d playerPos = NUtils.player().rc;
        ArrayList<Integer> cornerOrder = new ArrayList<>();
        for (int i = 0; i < 4; i++) cornerOrder.add(i);
        cornerOrder.sort((a, b) -> Double.compare(corners[a].dist(playerPos), corners[b].dist(playerPos)));
        
        // Try each corner's diagonal position
        for (int idx : cornerOrder) {
            Coord2d corner = corners[idx];
            Coord2d dir = diagonalDirs[idx];
            
            // Calculate target position for barrel center
            Coord2d targetPos = new Coord2d(
                corner.x + dir.x * offset,
                corner.y + dir.y * offset
            );
            
            // Define search area around target position
            Coord2d ul = targetPos.sub(barrelEnd.x + 2, barrelEnd.y + 2);
            Coord2d br = targetPos.add(barrelEnd.x + 2, barrelEnd.y + 2);
            
            Coord2d pos = Finder.getFreePlace(new Pair<>(ul, br), barrel);
            if (pos != null) {
                // Use findLiftedbyPlayer() to get the currently carried barrel
                new PlaceObject(findLiftedbyPlayer(), pos, 0).run(gui);
                return pos;
            }
        }
        
        return null;
    }
}
