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

public class ReturnBarrelFromWorkArea implements Action {
    NContext context;
    String item;

    public ReturnBarrelFromWorkArea(NContext context, String item) {
        this.context = context;
        this.item = item;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (context.workstation == null)
            context.getSpecArea(Specialisation.SpecName.barrelworkarea);
        else
            context.getSpecArea(context.workstation);
        
        long barrelid = -1;
        Gob gob = null;
        
        // First try to find barrel by stored hash for this specific item
        String storedHash = context.getPlacedBarrelHash(item);
        if (storedHash != null) {
            gui.msg("ReturnBarrelFromWorkArea: Looking for barrel with hash " + storedHash.substring(0, Math.min(16, storedHash.length())) + "... for item '" + item + "'");
            gob = findBarrelByHash(gui, storedHash);
            if (gob != null) {
                gui.msg("ReturnBarrelFromWorkArea: Found barrel by hash, id=" + gob.id);
                new LiftObject(gob).run(gui);
                barrelid = gob.id;
            } else {
                gui.msg("ReturnBarrelFromWorkArea: Barrel with stored hash not found after navigation!");
            }
        }
        
        // Fallback: find barrel in work area by content
        if (barrelid == -1) {
            gob = context.getBarrelInWorkArea(item);
            if (gob != null && NUtils.barrelHasContent(gob) && 
                context.getBarrelStorage(item) != null &&
                NUtils.getContentsOfBarrel(gob).equals(context.getBarrelStorage(item).olname)) {
                gui.msg("ReturnBarrelFromWorkArea: Found barrel by content match");
                new LiftObject(gob).run(gui);
                barrelid = gob.id;
            }
        }

        // Fallback: find any barrel in work area
        if (barrelid == -1) {
            gob = context.getBarrelInWorkArea(item);
            if (gob != null && !NUtils.barrelHasContent(gob)) {
                gui.msg("ReturnBarrelFromWorkArea: Found empty barrel in work area");
                new LiftObject(gob).run(gui);
                barrelid = gob.id;
            }
        }
        
        if (barrelid == -1) {
            gui.msg("ReturnBarrelFromWorkArea: Could not find barrel to return for item '" + item + "'");
            return Results.ERROR("Could not find barrel to return");
        }
        
        // Determine target position: use stored original position or fallback to barrel storage
        NGlobalCoord targetCoord = context.getOriginalBarrelCoord(item);
        if (targetCoord == null && context.getBarrelStorage(item) != null) {
            targetCoord = context.getBarrelStorage(item).coord;
        }
        
        if (targetCoord == null) {
            gui.msg("ReturnBarrelFromWorkArea: No target position for barrel return");
            return Results.ERROR("No target position for barrel return");
        }
        
        gui.msg("ReturnBarrelFromWorkArea: Returning barrel to " + targetCoord.getCurrentCoord());
        context.navigateToBarrelArea(item);
        new PlaceObject(Finder.findGob(barrelid), targetCoord.getCurrentCoord(), 0).run(gui);
        
        // Clear barrel tracking info for this item after returning
        context.clearBarrelInfo(item);
        gui.msg("ReturnBarrelFromWorkArea: Cleared barrel info for item '" + item + "'");
        
        if (context.workstation == null)
            context.getSpecArea(Specialisation.SpecName.barrelworkarea);
        else
            context.getSpecArea(context.workstation);
        
        return Results.SUCCESS();
    }
    
    /**
     * Find barrel by its hash. If not found, navigates to workstation area and retries.
     */
    private Gob findBarrelByHash(NGameUI gui, String hash) throws InterruptedException {
        if (hash == null) return null;
        
        ArrayList<Gob> barrels = Finder.findGobs(new NAlias("barrel"));
        for (Gob barrel : barrels) {
            if (hash.equals(barrel.ngob.hash)) {
                return barrel;
            }
        }
        
        // Barrel not in cache - try to navigate to workstation area to reload objects
        gui.msg("findBarrelByHash: Barrel not in cache, navigating to workstation area...");
        
        NArea area;
        if (context.workstation == null) {
            area = context.getSpecArea(Specialisation.SpecName.barrelworkarea);
        } else {
            area = context.getSpecArea(context.workstation);
        }
        
        if (area != null && area.getRCArea() != null) {
            haven.Pair<haven.Coord2d, haven.Coord2d> rcArea = area.getRCArea();
            haven.Coord2d center = rcArea.b.sub(rcArea.a).div(2).add(rcArea.a);
            new PathFinder(center).run(gui);
            
            // Wait for objects to load
            Thread.sleep(500);
            
            // Retry search
            barrels = Finder.findGobs(new NAlias("barrel"));
            for (Gob barrel : barrels) {
                if (hash.equals(barrel.ngob.hash)) {
                    gui.msg("findBarrelByHash: Found barrel after navigation");
                    return barrel;
                }
            }
            
            // Still not found - search by proximity to workstation
            if (context.workstation != null && context.workstation.selected != -1) {
                Gob ws = Finder.findGob(context.workstation.selected);
                if (ws != null) {
                    for (Gob barrel : barrels) {
                        if (barrel.rc.dist(ws.rc) < 30) {
                            gui.msg("findBarrelByHash: Found barrel near workstation by proximity");
                            return barrel;
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
