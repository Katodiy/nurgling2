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
        long barrelId = barrel.id;
        
        if (context.workstation == null) {
            NArea area = context.getSpecArea(Specialisation.SpecName.barrelworkarea);
            if (area != null && area.getRCArea() != null) {
                int count = Finder.findGobs(area, new NAlias("barrel")).size();

                Pair<Coord2d, Coord2d> rcarea = area.getRCArea();
                Coord2d center = rcarea.b.sub(rcarea.a).div(2).add(rcarea.a);
                Coord2d bshift = barrel.ngob.hitBox.end.sub(barrel.ngob.hitBox.begin).div(2);
                
                Coord2d placedPos = null;
                
                // Place barrels at corners relative to center (diagonal positions)
                if (count == 0) {
                    placedPos = center.sub(4.5, 4.5).sub(bshift);
                    new PlaceObject(Finder.findGob(barrelId), placedPos, 0).run(gui);
                } else if (count == 1) {
                    placedPos = center.sub(4.5, -4.5).sub(bshift.x, -bshift.y);
                    new PlaceObject(Finder.findGob(barrelId), placedPos, 0).run(gui);
                } else if (count == 2) {
                    placedPos = center.sub(4.5, 4.5).sub(-bshift.x, bshift.y);
                    new PlaceObject(Finder.findGob(barrelId), placedPos, 0).run(gui);
                } else if (count == 3) {
                    placedPos = center.sub(4.5, -4.5).sub(-bshift.x, -bshift.y);
                    new PlaceObject(Finder.findGob(barrelId), placedPos, 0).run(gui);
                }
                
                if (placedPos != null) {
                    gui.msg("TransferBarrelInWorkArea: Barrel placed at " + placedPos + " for item '" + item + "'");
                    
                    // Get updated barrel reference after placement
                    barrel = Finder.findGob(barrelId);
                    if (barrel != null) {
                        // Store barrel tracking info
                        context.storeBarrelInfo(item, barrel.ngob.hash, new NGlobalCoord(originalPos));
                        gui.msg("TransferBarrelInWorkArea: Stored barrel hash = " + barrel.ngob.hash + " for item '" + item + "'");
                        gui.msg("TransferBarrelInWorkArea: Original barrel pos = " + originalPos);
                    } else {
                        gui.msg("TransferBarrelInWorkArea: ERROR - barrel not found after placement!");
                    }
                }
            }
        }
        return Results.SUCCESS();
    }
}
