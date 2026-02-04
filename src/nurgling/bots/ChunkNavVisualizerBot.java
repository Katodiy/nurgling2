package nurgling.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.widgets.ChunkNavVisualizerWindow;

/**
 * Bot that opens the ChunkNav Visualizer UI window.
 * Shows a visual representation of all recorded chunks, portals, and paths.
 */
public class ChunkNavVisualizerBot implements Action {

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui == null || gui.ui == null) {
            return Results.FAIL();
        }

        // Open the visualizer window
        ChunkNavVisualizerWindow window = new ChunkNavVisualizerWindow();
        gui.add(window, new Coord(gui.sz.x / 2 - window.sz.x / 2, gui.sz.y / 2 - window.sz.y / 2));

        gui.msg("ChunkNav Visualizer opened");
        return Results.SUCCESS();
    }
}
