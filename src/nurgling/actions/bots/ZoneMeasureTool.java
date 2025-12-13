package nurgling.actions.bots;

import haven.Coord;
import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.widgets.NZoneMeasureTool;

public class ZoneMeasureTool implements Action {
    private static NZoneMeasureTool currentTool = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Toggle behavior: close if already open
        if (currentTool != null && currentTool.parent != null) {
            currentTool.wdgmsg("close");
            currentTool = null;
            return Results.SUCCESS();
        }

        // Create and display new tool window
        currentTool = new NZoneMeasureTool(gui);

        // Center on screen, offset up slightly
        Coord center = new Coord(
            gui.sz.x / 2 - currentTool.sz.x / 2,
            gui.sz.y / 2 - currentTool.sz.y / 2 - 100
        );
        gui.add(currentTool, center);

        return Results.SUCCESS();
    }
}
