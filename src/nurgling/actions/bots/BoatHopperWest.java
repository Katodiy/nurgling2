package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Results;
import nurgling.actions.Action;

/**
 * Boat hopper that moves west.
 */
public class BoatHopperWest implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // West direction = PI radians (180 degrees)
        return new BoatHopAction(Math.PI, "West").run(gui);
    }
}