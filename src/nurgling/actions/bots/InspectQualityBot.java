package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.Results;
import nurgling.overlays.QualityOl;
import nurgling.tasks.GetCurs;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;

import java.util.ArrayList;

import static haven.OCache.posres;

/**
 * Bot that inspects quality of all objects in selected area.
 * For each object: navigates to it, activates inspect pagina, 
 * left clicks to inspect, waits for quality overlay, then cancels inspect.
 */
public class InspectQualityBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Prompt user to select area
        SelectArea selectArea;
        NUtils.getGameUI().msg("Please select area for quality inspection");
        (selectArea = new SelectArea(Resource.loadsimg("baubles/inputArea"))).run(gui);
        
        // Get all gobs in the selected area
        ArrayList<Gob> gobs = Finder.findGobs(selectArea.getRCArea(), null);
        
        if (gobs.isEmpty()) {
            NUtils.getGameUI().msg("No objects found in selected area");
            return Results.SUCCESS();
        }
        
        NUtils.getGameUI().msg("Found " + gobs.size() + " objects to inspect");
        
        // Sort gobs by distance from player
        gobs.sort(NUtils.d_comp);
        
        // Iterate through each object
        for (Gob gob : gobs) {
            if (!PathFinder.isAvailable(gob)) {
                continue;
            }
            
            try {
                // Navigate to the object
                PathFinder pathFinder = new PathFinder(gob);
                pathFinder.run(gui);
                
                // Activate inspect pagina
                gui.ui.rcvr.rcvmsg(NUtils.getUI().getMenuGridId(), "act", "inspect");
                
                // Wait for cursor to change to magnifying glass (study)
                NUtils.getUI().core.addTask(new GetCurs("study"));
                
                // Left click on the object to inspect it
                NUtils.clickGob(gob);
                NUtils.getGameUI().map.clickedGob = new MapView.ClickedGob(gob,1);
                
                // Wait for QualityOl overlay to appear on the object
                final Gob targetGob = gob;
                NUtils.getUI().core.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        return targetGob.findol(QualityOl.class) != null;
                    }
                });
                
                // Right click under player to cancel inspect mode
                Gob player = NUtils.player();
                if (player != null) {
                    gui.map.wdgmsg("click", Coord.z, player.rc.floor(posres), 3, 0);
                }
                
                // Wait for cursor to return to arrow
                NUtils.getUI().core.addTask(new GetCurs("arw"));
                
            } catch (Exception e) {
                NUtils.getGameUI().error("Error inspecting object: " + e.getMessage());
                // Try to reset cursor to arrow if something goes wrong
                try {
                    NUtils.getDefaultCur();
                } catch (Exception ignored) {
                }
            }
        }
        
        NUtils.getGameUI().msg("Quality inspection completed for all objects");
        return Results.SUCCESS();
    }
}
