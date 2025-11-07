package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Results;

/**
 * Simple swill transfer action.
 * For the initial implementation, this provides a basic framework.
 */
public class SwillTransferAction implements Action {

    public SwillTransferAction() {
        // Simple constructor
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        gui.msg("Swill transfer - manual feeding recommended");
        return Results.SUCCESS();
    }
}