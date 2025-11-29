package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
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
        
        // TODO: Bot execution will be implemented tomorrow
        // For now, just return success after window closes
        gui.msg("Forager bot: Settings loaded. Execution will be implemented tomorrow.");
        
        return Results.SUCCESS();
        
        /* COMMENTED OUT - TO BE IMPLEMENTED
        Coord2d startPoint = path.waypoints.get(0);
        
        // Check and unload inventory before starting
        if (isInventoryFull(gui)) {
            gui.msg("Inventory full, unloading before starting...");
            new PathFinder(startPoint).run(gui);
            unloadInventory(gui);
        }
        
        // Main loop through sections
        for (int i = 0; i < path.getSectionCount(); i++) {
            ForagerSection section = path.getSection(i);
            
            if (section == null) {
                continue;
            }
            
            gui.msg(String.format("Processing section %d/%d", i + 1, path.getSectionCount()));
            
            // Navigate to section start
            new PathFinder(section.startPoint).run(gui);
            
            // Process actions for this section
            processSection(gui, section);
            
            // Check inventory
            if (isInventoryFull(gui)) {
                gui.msg("Inventory full, returning to unload...");
                
                // Return to start point
                new PathFinder(startPoint).run(gui);
                
                // Unload inventory
                unloadInventory(gui);
                
                // Return to current section
                new PathFinder(section.endPoint).run(gui);
            }
        }
        
        // After completing all sections, travel hearth
        gui.msg("Path complete, traveling to hearth...");
        new TravelToHearthFire().run(gui);
        
        return Results.SUCCESS();
        */
    }
    
    private void processSection(NGameUI gui, ForagerSection section) throws InterruptedException {
        double radius = 50.0;
        
        for (ForagerAction action : section.actions) {
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
                    
                    try {
                        new PathFinder(gob).run(gui);
                        new SelectFlowerAction("Pick", gob).run(gui);
                        NUtils.getUI().core.addTask(new nurgling.tasks.WaitGobRemoval(gob.id));
                    } catch (Exception e) {
                        // Object might have disappeared, continue
                    }
                }
                break;
                
            case FLOWER_ACTION:
                for (Gob gob : gobs) {
                    try {
                        new PathFinder(gob).run(gui);
                        new SelectFlowerAction(action.actionName, gob).run(gui);
                        
                        // Wait for pose change
                        NUtils.getUI().core.addTask(new nurgling.tasks.WaitPose(NUtils.player(), "gfx/borka/idle"));
                    } catch (Exception e) {
                        // Action might have failed, continue
                    }
                }
                break;
                
            case CHAT_NOTIFY:
                if (!gobs.isEmpty()) {
                    gui.msg(String.format("Found %d %s objects!", gobs.size(), action.targetObjectPattern));
                }
                break;
        }
    }
    
    private boolean isInventoryFull(NGameUI gui) {
        try {
            if (gui.vhand != null) {
                return true;
            }
            
            if (gui.getInventory() != null) {
                return gui.getInventory().getFreeSpace() <= 4;
            }
        } catch (Exception e) {
            // Error checking inventory, assume not full
        }
        return false;
    }
    
    private void unloadInventory(NGameUI gui) throws InterruptedException {
        // Use existing unload system
        gui.msg("Unloading inventory...");
        
        // This will use the logistics system to unload items
        // Similar to how other bots do it
        
        // TODO: Implement proper unload using FreeContainers or similar
        // For now, just wait a bit to simulate unloading
        Thread.sleep(1000);
    }
}
