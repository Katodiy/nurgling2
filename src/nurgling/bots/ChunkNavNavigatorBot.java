package nurgling.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.widgets.ChunkNavNavigatorWindow;

/**
 * Bot that opens the ChunkNav Navigator UI window.
 * This allows testing the chunk-based navigation system interactively.
 */
public class ChunkNavNavigatorBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui == null || gui.ui == null) {
            return Results.FAIL();
        }

        // Open the navigator window
        ChunkNavNavigatorWindow window = new ChunkNavNavigatorWindow();
        gui.add(window, new Coord(gui.sz.x / 2 - window.sz.x / 2, gui.sz.y / 2 - window.sz.y / 2));

        // The bot completes immediately - the window handles navigation
        gui.msg("ChunkNav Navigator opened");
        return Results.SUCCESS();
    }
}
