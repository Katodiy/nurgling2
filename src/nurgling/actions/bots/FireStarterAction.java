package nurgling.actions.bots;

import haven.Gob;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NContext;

import java.util.ArrayList;

/**
 * Fire Starter Bot - Ignites objects and refuels them if needed.
 * 
 * User clicks on a target object, and the bot determines what it is
 * based on the model and handles fueling + lighting appropriately.
 * 
 * Fuel is only added to:
 * - Cauldron (checks model for fuel presence)
 * - Fire Place / pow (checks model for fuel presence)
 * 
 * All other objects are just lit without fuel check.
 */
public class FireStarterAction implements Action {
    
    // Fire object configuration based on gob path
    private static class FireObjectConfig {
        final String displayName;
        final int flameFlag;
        final boolean needsFuel; // only cauldron and pow need fuel check
        
        FireObjectConfig(String displayName, int flameFlag, boolean needsFuel) {
            this.displayName = displayName;
            this.flameFlag = flameFlag;
            this.needsFuel = needsFuel;
        }
    }
    
    /**
     * Determine fire object configuration based on gob name
     */
    private FireObjectConfig getConfig(String gobName) {
        if (gobName.contains("gfx/terobjs/oven")) {
            return new FireObjectConfig("Oven", 4, false);
        } else if (gobName.contains("gfx/terobjs/primsmelter")) {
            return new FireObjectConfig("Stack Furnace", 2, false);
        } else if (gobName.contains("gfx/terobjs/smelter")) {
            return new FireObjectConfig("Smelter", 2, false);
        } else if (gobName.contains("gfx/terobjs/fineryforge")) {
            return new FireObjectConfig("Finery Forge", 8, false);
        } else if (gobName.contains("gfx/terobjs/smokeshed")) {
            return new FireObjectConfig("Smoke Shed", 16, false);
        } else if (gobName.contains("gfx/terobjs/tarkiln")) {
            return new FireObjectConfig("Tar Kiln", 4, false);
        } else if (gobName.contains("gfx/terobjs/cauldron")) {
            // Cauldron needs fuel check
            // bit 1 = fuel, bit 2 = fire burning
            return new FireObjectConfig("Cauldron", 2, true);
        } else if (gobName.contains("gfx/terobjs/kiln")) {
            return new FireObjectConfig("Kiln", 4, false);
        } else if (gobName.contains("gfx/terobjs/pow")) {
            // Fire Place (pow) needs fuel check
            return new FireObjectConfig("Fire Place", 4, true);
        }
        return null;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Ask user to click on target object
        SelectGob selgob;
        NUtils.getGameUI().msg("Click on the object to ignite");
        (selgob = new SelectGob(Resource.loadsimg("baubles/selectItem"))).run(gui);
        
        Gob target = selgob.getResult();
        if (target == null) {
            return Results.ERROR("No object selected");
        }
        
        String gobName = target.ngob.name;
        if (gobName == null) {
            return Results.ERROR("Cannot determine object type");
        }
        
        // Get configuration for this object type
        FireObjectConfig config = getConfig(gobName);
        if (config == null) {
            return Results.ERROR("Unknown object type: " + gobName);
        }
        
        gui.msg("Fire Starter: Processing " + config.displayName);
        
        // Check if already burning
        if ((target.ngob.getModelAttribute() & config.flameFlag) != 0) {
            gui.msg(config.displayName + " is already burning");
            return Results.SUCCESS();
        }
        
        // Handle fuel for cauldron and pow (check by model attribute)
        if (config.needsFuel) {
            if (gobName.contains("gfx/terobjs/pow")) {
                return handleFirePlace(gui, target, config);
            } else if (gobName.contains("gfx/terobjs/cauldron")) {
                return handleCauldron(gui, target, config);
            }
        }
        
        // For all other objects - just light them
        ArrayList<String> toLight = new ArrayList<>();
        toLight.add(target.ngob.hash);
        
        gui.msg("Lighting " + config.displayName + "...");
        Results lightResult = new LightGob(toLight, config.flameFlag).run(gui);
        
        if (!lightResult.IsSuccess()) {
            return Results.ERROR("Failed to light fire");
        }
        
        gui.msg("Fire Starter: Completed successfully!");
        return Results.SUCCESS();
    }
    
    /**
     * Handle Cauldron - check fuel by model and add if needed
     * Fuel marker on model: bit 1 (fuel present)
     */
    private Results handleCauldron(NGameUI gui, Gob target, FireObjectConfig config) throws InterruptedException {
        NContext context = new NContext(gui);
        
        ArrayList<Gob> gobs = new ArrayList<>();
        gobs.add(target);
        
        // Check if fuel is present on model (marker 1)
        // If (modelAttribute & 1) == 0 - no fuel, need to add
        if ((target.ngob.getModelAttribute() & 1) == 0) {
            gui.msg("Adding fuel to Cauldron...");
            Results fuelResult = new FillFuelPowOrCauldron(context, gobs, 1).run(gui);
            
            if (!fuelResult.IsSuccess()) {
                return Results.ERROR("NO FUEL");
            }
            gui.msg("Fuel added successfully");
        }
        
        // Light the cauldron
        ArrayList<String> toLight = new ArrayList<>();
        toLight.add(target.ngob.hash);
        
        gui.msg("Lighting Cauldron...");
        Results lightResult = new LightGob(toLight, config.flameFlag).run(gui);
        
        if (!lightResult.IsSuccess()) {
            return Results.ERROR("Failed to light fire");
        }
        
        gui.msg("Fire Starter: Completed successfully!");
        return Results.SUCCESS();
    }
    
    /**
     * Handle Fire Place (pow) - check fuel by model and add if needed
     * Fuel marker on model: bit 1 (fuel present)
     */
    private Results handleFirePlace(NGameUI gui, Gob target, FireObjectConfig config) throws InterruptedException {
        NContext context = new NContext(gui);
        
        // Check if already in use (modelAttribute & 48 means hearthfire or special state)
        if ((target.ngob.getModelAttribute() & 48) != 0) {
            gui.msg("Fire Place is already in use");
            return Results.SUCCESS();
        }
        
        ArrayList<Gob> gobs = new ArrayList<>();
        gobs.add(target);
        
        // Check if fuel is present on model (marker 1)
        // If (modelAttribute & 1) == 0 - no fuel, need to add
        if ((target.ngob.getModelAttribute() & 1) == 0) {
            gui.msg("Adding fuel (Blocks) to Fire Place...");
            Results fuelResult = new FillFuelPowOrCauldron(context, gobs, 1).run(gui);
            
            if (!fuelResult.IsSuccess()) {
                return Results.ERROR("NO FUEL");
            }
            gui.msg("Fuel added successfully");
        }
        
        // Light the fireplace
        ArrayList<String> toLight = new ArrayList<>();
        toLight.add(target.ngob.hash);
        
        gui.msg("Lighting Fire Place...");
        Results lightResult = new LightGob(toLight, config.flameFlag).run(gui);
        
        if (!lightResult.IsSuccess()) {
            return Results.ERROR("Failed to light fire");
        }
        
        gui.msg("Fire Starter: Completed successfully!");
        return Results.SUCCESS();
    }
}
