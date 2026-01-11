package nurgling.actions;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.bots.cheese.CheeseRackOverlayUtils;
import nurgling.actions.bots.cheese.CheeseConstants;
import nurgling.actions.bots.cheese.CheeseAreaManager;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.cheese.CheeseBranch;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Clears ready cheese from all racks to buffer containers and records rack capacity
 * Use getLastRecordedCapacity() to access the capacity data after running
 * Use getBufferEmptinessMap() to access buffer emptiness data for optimization
 */
public class ClearRacksAndRecordCapacity implements Action {
    // Use centralized cheese tray size constant
    private Map<CheeseBranch.Place, Integer> lastRecordedCapacity = new HashMap<>();
    private Map<CheeseBranch.Place, Boolean> bufferEmptinessMap = new HashMap<>();
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        lastRecordedCapacity = new HashMap<>();
        bufferEmptinessMap = new HashMap<>();
        Map<CheeseBranch.Place, Integer> rackCapacity = new HashMap<>();

        CheeseBranch.Place[] places = {
                CheeseBranch.Place.inside,
                CheeseBranch.Place.cellar,
                CheeseBranch.Place.outside,
                CheeseBranch.Place.mine
        };

        for (CheeseBranch.Place place : places) {
            // Get ALL areas for this place type (supports multiple cellars, multiple inside areas, etc.)
            ArrayList<NArea> areasForPlace = CheeseAreaManager.getAllCheeseAreas(place);

            if (areasForPlace.isEmpty()) {
                gui.msg("No cheese racks area found for " + place);
                rackCapacity.put(place, 0);
                bufferEmptinessMap.put(place, true);
                continue;
            }

            // Aggregate capacity and buffer emptiness across all areas of this place type
            int totalCapacity = 0;
            boolean allBuffersEmptyForPlace = true;

            for (NArea area : areasForPlace) {
                // Step 1: Clear ready cheese from racks to buffer containers and get capacity
                int areaCapacity = clearReadyCheeseFromArea(gui, area, place);
                totalCapacity += areaCapacity;

                // Step 2: Check buffer emptiness in this area
                boolean areaBuffersEmpty = checkBufferEmptinessForArea(gui, area, place);
                if (!areaBuffersEmpty) {
                    allBuffersEmptyForPlace = false;
                }
            }

            rackCapacity.put(place, totalCapacity);
            bufferEmptinessMap.put(place, allBuffersEmptyForPlace);
        }

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
     * Get the buffer emptiness map for optimization
     * @return Map of place to boolean indicating if all buffers are empty
     */
    public Map<CheeseBranch.Place, Boolean> getBufferEmptinessMap() {
        return new HashMap<>(bufferEmptinessMap);
    }
    
    /**
     * Clear ready cheese from a specific area's racks to its buffer containers
     * Uses the new MoveReadyCheeseToBuffers action for efficient batch processing
     * @param gui The game UI
     * @param area The specific area to process
     * @param place The place type (for logging)
     * @return total capacity of all racks in the area
     */
    private int clearReadyCheeseFromArea(NGameUI gui, NArea area, CheeseBranch.Place place) throws InterruptedException {
        // Navigate to the area first
        NContext context = new NContext(gui);
        context.getAreaById(area.id);

        // Find all cheese racks and buffer containers in this area
        ArrayList<Gob> rackGobs = Finder.findGobs(area, new NAlias(CheeseConstants.CHEESE_RACK_RESOURCE));
        ArrayList<Gob> bufferGobs = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

        // Convert to Container objects
        ArrayList<Container> racks = new ArrayList<>();
        for (Gob rack : rackGobs) {
            racks.add(new Container(rack, CheeseConstants.RACK_CONTAINER_TYPE, area));
        }

        ArrayList<Container> buffers = new ArrayList<>();
        for (Gob buffer : bufferGobs) {
            buffers.add(new Container(buffer, NContext.contcaps.get(buffer.ngob.name), area));
        }

        // Log rack status summary using overlays
        String rackStatusSummary = CheeseRackOverlayUtils.getRackStatusSummary(rackGobs);

        // Use the new efficient action to move ready cheese and get capacity data
        MoveReadyCheeseToBuffers moveAction = new MoveReadyCheeseToBuffers(racks, buffers, place);
        MoveReadyCheeseToBuffers.ResultWithCapacity result = moveAction.runWithCapacity(gui);

        // Calculate total capacity from all racks
        int totalCapacity = 0;
        for (Integer capacity : result.rackCapacities.values()) {
            totalCapacity += capacity;
        }

        return totalCapacity;
    }
    
    /**
     * Check if all buffer containers in a specific area are empty
     * Uses the same condition as line 123 of ProcessCheeseFromBufferContainers
     * @param gui Game UI
     * @param area The specific area to check
     * @param place Place type (for logging)
     * @return true if ALL buffers are empty, false otherwise
     */
    private boolean checkBufferEmptinessForArea(NGameUI gui, NArea area, CheeseBranch.Place place) throws InterruptedException {
        // Find all buffer containers in this area
        ArrayList<Gob> bufferGobs = Finder.findGobs(area, new NAlias(new ArrayList<>(NContext.contcaps.keySet()), new ArrayList<>()));

        if (bufferGobs.isEmpty()) {
            return true;
        }

        // Check each buffer using the same condition as ProcessCheeseFromBufferContainers line 123
        for (Gob containerGob : bufferGobs) {
            // Skip checking empty containers - same condition as line 123
            if((containerGob.ngob.name.equals("gfx/terobjs/chest") || containerGob.ngob.name.equals("gfx/terobjs/cupboard")) && containerGob.ngob.getModelAttribute() == 2) {
                // This container is empty, continue checking others
                continue;
            } else {
                // Found a non-empty container, so not all buffers are empty
                return false;
            }
        }

        // All buffers passed the empty test
        return true;
    }
}