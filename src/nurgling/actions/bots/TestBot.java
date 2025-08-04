package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Results;
import nurgling.actions.Action;

/**
 * Test bot that demonstrates keyboard input handling.
 * Runs 10 iterations with user input waiting on each step.
 */
public class TestBot implements Action {
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        gui.msg("Test bot started! Press N to proceed to the next iteration.");
        
        for (int i = 1; i <= 10; i++) {
            gui.msg("Iteration " + i + " of 10");
            
            // Wait for debug input (N key press)
            NUtils.waitForDebugInput();
            
            gui.msg("Iteration " + i + " completed. Press N to continue...");
        }
        
        gui.msg("Test bot finished working!");
        return Results.SUCCESS();
    }
}
