package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import static nurgling.areas.NContext.workstation_spec_map;

import haven.render.sl.BinOp;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;

public class UseWorkStation implements Action
{
    public UseWorkStation(NContext context)
    {
        this.cnt = context;
    }

    NContext cnt;
    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(cnt.workstation.selected==-1)
        {
            Gob ws = Finder.findGob(new NAlias(cnt.workstation.station));
            
            boolean isReachable = ws != null && PathFinder.isAvailable(ws);
            
            if(!isReachable) {
                // Workstation not visible or not reachable - need global navigation
                cnt.navigateToAreaIfNeeded(workstation_spec_map.get(cnt.workstation.station).toString());
                ws = Finder.findGob(new NAlias(cnt.workstation.station));
                if(ws == null)
                    return Results.FAIL();
            }
            cnt.workstation.selected = ws.id;
        }
        // Check if workstation is visible AND reachable
        Gob ws = Finder.findGob(cnt.workstation.selected);
        
        if (ws == null) {
            cnt.navigateToAreaIfNeeded(workstation_spec_map.get(cnt.workstation.station).toString());
            ws = Finder.findGob(cnt.workstation.selected);
        } else if (!PathFinder.isAvailable(ws)) {
            cnt.navigateToAreaIfNeeded(workstation_spec_map.get(cnt.workstation.station).toString());
            ws = Finder.findGob(cnt.workstation.selected);
        }
        
        if(ws == null)
            return Results.ERROR("NO WORKSTATION");
        else
        {
            // Try to find common approach point if there's a barrel in work area
            boolean usedCommonPoint = false;
            Gob barrel = findBarrelNearWorkstation(ws);
            
            if (barrel != null) {
                Coord2d commonPoint = PathFinder.findNearestCommonApproachPoint(ws, barrel);
                if (commonPoint != null) {
                    // Use PathFinder to workstation with the knowledge that we want to end up at commonPoint
                    // PathFinder to workstation will use its hardMode logic
                    PathFinder pf = new PathFinder(ws);
                    pf.isHardMode = true;
                    Results pfResult = pf.run(gui);
                    
                    if (pfResult.IsSuccess()) {
                        usedCommonPoint = true;
                    }
                }
            }
            
            // Fallback to regular pathfinding if no common point found
            if (!usedCommonPoint) {
                new PathFinder(ws).run(gui);
            }
            
            if(cnt.workstation.station.contains("gfx/terobjs/pow") || cnt.workstation.station.contains("gfx/terobjs/cauldron"))
            {
                new SelectFlowerAction("Open",Finder.findGob(cnt.workstation.selected)).run(gui);
                if(cnt.workstation.station.contains("gfx/terobjs/pow"))
                {
                    NUtils.addTask(new WaitWindow("Fireplace"));
                }
                else
                {
                    NUtils.addTask(new WaitWindow("Cauldron"));
                }
            }
            else {
                gui.map.wdgmsg("click", Coord.z, ws.rc.floor(posres), 3, 0, 0, (int) ws.id,
                        ws.rc.floor(posres), 0, -1);
                if (cnt.workstation.pose != null)
                    NUtils.getUI().core.addTask(new FollowAndPose(NUtils.player(), cnt.workstation.pose));
                else {
                    NUtils.getUI().core.addTask(new IsMoving(ws.rc, 50));
                    NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/idle"));
                }
            }
        }
        return Results.SUCCESS();
    }
    
    /**
     * Find a barrel that is placed near the workstation.
     * First tries to find by stored hashes (most reliable), then falls back to proximity search.
     */
    private Gob findBarrelNearWorkstation(Gob workstation) throws InterruptedException {
        if (workstation == null) return null;
        
        // First try to find by stored hashes (most reliable when barrel was placed by us)
        if (cnt.workstation != null && !cnt.workstation.placedBarrelHashes.isEmpty()) {
            for (String hash : cnt.workstation.placedBarrelHashes.values()) {
                Gob barrel = findBarrelByHash(hash);
                if (barrel != null) {
                    return barrel;
                }
            }
        }
        
        // Fallback: search for barrels within ~25 units of workstation
        double searchRadius = 25.0;
        Gob nearestBarrel = Finder.findGob(workstation.rc, new NAlias("barrel"), null, searchRadius);
        
        return nearestBarrel;
    }
    
    /**
     * Find barrel by its hash.
     */
    private Gob findBarrelByHash(String hash) {
        if (hash == null) return null;
        
        ArrayList<Gob> barrels = Finder.findGobs(new NAlias("barrel"));
        for (Gob barrel : barrels) {
            if (hash.equals(barrel.ngob.hash)) {
                return barrel;
            }
        }
        return null;
    }
}
