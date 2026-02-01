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

import static nurgling.tools.Finder.findLiftedbyPlayer;

public class TransferBarrelInWorkArea implements Action {
    NContext context;
    String item;

    public TransferBarrelInWorkArea(NContext context, String item) {
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
        
        if (context.workstation == null) {
            // Find barrelworkarea globally and navigate to it
            NArea area = context.getSpecArea(Specialisation.SpecName.barrelworkarea);
            if (area == null) {
                area = NContext.findSpecGlobal(Specialisation.SpecName.barrelworkarea.toString());
            }
            if (area != null) {
                // Navigate to the area using global pathfinding before placing
                // This works even if area is not currently visible
                NUtils.navigateToArea(area);
                
                // Now that we've navigated, getRCArea should be available
                if (area.getRCArea() == null)
                    return Results.ERROR("Could not get area coordinates after navigation");
                
                int count = Finder.findGobs(area, new NAlias("barrel")).size();

                Pair<Coord2d, Coord2d> rcarea = area.getRCArea();
                Coord2d center = rcarea.b.sub(rcarea.a).div(2).add(rcarea.a);
                Coord2d bshift = barrel.ngob.hitBox.end.sub(barrel.ngob.hitBox.begin).div(2);
                
                Coord2d placedPos = null;
                
                // Place barrels at corners relative to center (diagonal positions)
                // Use findLiftedbyPlayer() instead of barrelId since ID can change during location transitions
                if (count == 0) {
                    placedPos = center.sub(4.5, 4.5).sub(bshift);
                    new PlaceObject(findLiftedbyPlayer(), placedPos, 0).run(gui);
                } else if (count == 1) {
                    placedPos = center.sub(4.5, -4.5).sub(bshift.x, -bshift.y);
                    new PlaceObject(findLiftedbyPlayer(), placedPos, 0).run(gui);
                } else if (count == 2) {
                    placedPos = center.sub(4.5, 4.5).sub(-bshift.x, bshift.y);
                    new PlaceObject(findLiftedbyPlayer(), placedPos, 0).run(gui);
                } else if (count == 3) {
                    placedPos = center.sub(4.5, -4.5).sub(-bshift.x, -bshift.y);
                    new PlaceObject(findLiftedbyPlayer(), placedPos, 0).run(gui);
                }
                
                if (placedPos != null) {
                    // Get updated barrel reference after placement using findLiftedbyPlayer
                    Gob placedBarrel = findLiftedbyPlayer();
                    if (placedBarrel == null) {
                        // Barrel was placed, find it at the placed position
                        placedBarrel = Finder.findGob(area, new NAlias("barrel"));
                    }
                    if (placedBarrel != null) {
                        // Store barrel tracking info
                        context.storeBarrelInfo(item, placedBarrel.ngob.hash, new NGlobalCoord(originalPos));
                    }
                }
            }
        }
        return Results.SUCCESS();
    }
}
