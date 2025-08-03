package nurgling.actions;

import haven.Coord;
import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.bots.cheese.CheeseUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Clears ready cheese from all racks to buffer containers and records rack capacity
 * Use getLastRecordedCapacity() to access the capacity data after running
 */
public class ClearRacksAndRecordCapacity implements Action {
    private final Coord TRAY_SIZE = new Coord(1, 2);
    private Map<CheeseBranch.Place, Integer> lastRecordedCapacity = new HashMap<>();
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        lastRecordedCapacity = new HashMap<>();
        Map<CheeseBranch.Place, Integer> rackCapacity = new HashMap<>();
        
        CheeseBranch.Place[] places = {
                CheeseBranch.Place.outside,
                CheeseBranch.Place.inside,
                CheeseBranch.Place.mine,
                CheeseBranch.Place.cellar
        };
        
        for (CheeseBranch.Place place : places) {
            gui.msg("=== Clearing " + place + " area and recording capacity ===");
            
            // Step 1: Clear ready cheese from racks to buffer containers and get capacity
            gui.msg("1. Clearing ready cheese from " + place + " racks and recording capacity");
            int capacity = clearReadyCheeseFromArea(gui, place);
            rackCapacity.put(place, capacity);
            gui.msg(place + " racks can fit " + capacity + " more trays");
        }
        
        gui.msg("=== Full picture obtained - rack capacity across all areas ===");
        lastRecordedCapacity = rackCapacity;
        return Results.SUCCESS();
    }
    
    /**
     * Get the last recorded rack capacity data
     * @return Map of place to available capacity, or empty map if not yet recorded
     */
    public Map<CheeseBranch.Place, Integer> getLastRecordedCapacity() {
        return new HashMap<>(lastRecordedCapacity);
    }
    
    /**
     * Clear ready cheese from a specific area's racks to its buffer containers
     * Uses the new MoveReadyCheeseToBuffers action for efficient batch processing
     * @return total capacity of all racks in the area
     */
    private int clearReadyCheeseFromArea(NGameUI gui, CheeseBranch.Place place) {
        try {
            // Create a fresh context to avoid caching issues when navigating between areas
            NContext freshContext = new NContext(gui);
            NArea area = freshContext.getSpecArea(Specialisation.SpecName.cheeseRacks, place.toString());
            if (area == null) {
                gui.msg("No cheese racks area found for " + place);
                return 0;
            }
            
            // Find all cheese racks and buffer containers in this area
            ArrayList<Gob> rackGobs = Finder.findGobs(area, new NAlias("gfx/terobjs/cheeserack"));
            ArrayList<Gob> bufferGobs = Finder.findGobs(area, new NAlias(new ArrayList<String>(NContext.contcaps.keySet()), new ArrayList<>()));
            
            // Convert to Container objects
            ArrayList<Container> racks = new ArrayList<>();
            for (Gob rack : rackGobs) {
                racks.add(new Container(rack, "Rack"));
            }
            
            ArrayList<Container> buffers = new ArrayList<>();
            for (Gob buffer : bufferGobs) {
                buffers.add(new Container(buffer, NContext.contcaps.get(buffer.ngob.name)));
            }
            
            gui.msg("Found " + racks.size() + " cheese racks and " + buffers.size() + " buffer containers in " + place + " area");
            
            // Use the new efficient action to move ready cheese and get capacity data
            MoveReadyCheeseToBuffers moveAction = new MoveReadyCheeseToBuffers(racks, buffers, place);
            MoveReadyCheeseToBuffers.ResultWithCapacity result = moveAction.runWithCapacity(gui);
            
            // Calculate total capacity from all racks
            int totalCapacity = 0;
            for (Integer capacity : result.rackCapacities.values()) {
                totalCapacity += capacity;
            }
            
            gui.msg("Finished clearing ready cheese from " + place + " area");
            return totalCapacity;
            
        } catch (Exception e) {
            gui.msg("Error clearing " + place + " area: " + e.getMessage());
            return 0;
        }
    }
}