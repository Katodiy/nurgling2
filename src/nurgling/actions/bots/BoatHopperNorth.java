package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Results;
import nurgling.actions.Action;

/**
 * Boat hopper that moves north.
 */
public class BoatHopperNorth implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // North direction = -PI/2 radians (-90 degrees)
        return new BoatHopAction(-Math.PI / 2, "North").run(gui);
    }
}