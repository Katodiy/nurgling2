package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Results;
import nurgling.actions.Action;

/**
 * Boat hopper that moves east.
 */
public class BoatHopperEast implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // East direction = 0 radians (0 degrees)
        return new BoatHopAction(0, "East").run(gui);
    }
}