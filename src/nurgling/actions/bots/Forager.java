package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.conf.NForagerProp;
import nurgling.routes.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

public class Forager implements Action {
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Forager w = null;
        NForagerProp prop = null;
        try {
            NUtils.getUI().core.addTask(new nurgling.tasks.WaitCheckable(
                NUtils.getGameUI().add((w = new nurgling.widgets.bots.Forager()), UI.scale(200, 200))
            ));
            prop = w.prop;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            if (w != null)
                w.destroy();
        }
        
        if (prop == null) {
            return Results.ERROR("No configuration");
        }
        
        NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset == null || preset.foragerPath == null) {
            return Results.ERROR("No path configured");
        }
        
        ForagerPath path = preset.foragerPath;
        
        if (path.getSectionCount() == 0) {
            return Results.ERROR("Path has no sections");
        }
        
        gui.msg("Forager bot starting: " + path.name + " with " + path.getSectionCount() + " sections");
        
        // Get first waypoint to navigate to start
        MCache mcache = gui.map.glob.map;
        Coord2d startPos = path.waypoints.get(0).toCoord2d(mcache);
        if(startPos == null) {
            return Results.ERROR("Cannot get start position - grid not loaded");
        }
        
        gui.msg("Moving to start position...");
        new PathFinder(startPos).run(gui);
        
        // Check and unload inventory before starting
        if (isInventoryFull(gui)) {
            gui.msg("Inventory full, unloading before starting...");
            unloadInventory(gui);
            // Return to start
            new PathFinder(startPos).run(gui);
        }
        
        // Main loop through sections
        for (int i = 0; i < path.getSectionCount(); i++) {
            ForagerSection section = path.getSection(i);
            if (section == null) continue;
            
            gui.msg(String.format("Processing section %d/%d", i + 1, path.getSectionCount()));
            
            // Navigate to section center
            Coord2d sectionCenter = section.getCenterPoint();
            new PathFinder(sectionCenter).run(gui);
            
            // Process actions for this section
            processSection(gui, section, preset.actions);
            
            // Check inventory after each section
            if (isInventoryFull(gui)) {
                gui.msg("Inventory full, returning to unload...");
                new PathFinder(startPos).run(gui);
                unloadInventory(gui);
                // Return to section
                new PathFinder(sectionCenter).run(gui);
            }
        }
        
        // After completing all sections, travel to hearth
        gui.msg("Path complete, traveling to hearth...");
        new TravelToHearthFire().run(gui);
        
        return Results.SUCCESS();
    }
    
    private void processSection(NGameUI gui, ForagerSection section, java.util.List<ForagerAction> actions) throws InterruptedException {
        double radius = 50.0;
        
        // Use actions from preset, not from section
        for (ForagerAction action : actions) {
            processAction(gui, action, section.getCenterPoint(), radius);
        }
    }
    
    private void processAction(NGameUI gui, ForagerAction action, Coord2d center, double radius) throws InterruptedException {
        ArrayList<Gob> gobs = Finder.findGobs(center, new NAlias(action.targetObjectPattern), null, radius);
        
        if (gobs.isEmpty()) {
            return;
        }
        
        switch (action.actionType) {
            case PICK:
                for (Gob gob : gobs) {
                    if (isInventoryFull(gui)) {
                        break;
                    }

                    new PathFinder(gob).run(gui);
                    new SelectFlowerAction("Pick", gob).run(gui);
                    NUtils.getUI().core.addTask(new nurgling.tasks.WaitGobRemoval(gob.id));


                }
                break;
                
            case FLOWER_ACTION:
                for (Gob gob : gobs) {
                        new PathFinder(gob).run(gui);
                        new SelectFlowerAction(action.actionName, gob).run(gui);
                        
                        // Wait for pose change
                        NUtils.getUI().core.addTask(new nurgling.tasks.WaitPose(NUtils.player(), "gfx/borka/idle"));
                }
                break;
                
            case CHAT_NOTIFY:
                if (!gobs.isEmpty()) {
                    gui.msg(String.format("Found %d %s objects!", gobs.size(), action.targetObjectPattern));
                }
                break;
        }
    }
    
    private boolean isInventoryFull(NGameUI gui) throws InterruptedException
    {

        if (gui.vhand != null) {
            return true;
        }

        if (gui.getInventory() != null) {
            return gui.getInventory().getFreeSpace() <= 4;
        }

        return false;
    }
    
    private void unloadInventory(NGameUI gui) throws InterruptedException {
        gui.msg("Inventory full! Please unload manually and press Enter to continue...");
        new FreeInventory2(new NContext(gui)).run(gui);
        gui.msg("Inventory cleared, continuing...");
    }
}
