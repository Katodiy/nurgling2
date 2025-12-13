package nurgling.actions.bots;

import haven.Coord;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.widgets.NCombatDistanceTool;

public class CombatDistanceTool implements Action {

    private static NCombatDistanceTool currentTool = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (currentTool != null && currentTool.parent != null) {
            currentTool.stopTool();
            currentTool.reqdestroy();
            currentTool = null;
            return Results.SUCCESS();
        }

        currentTool = new NCombatDistanceTool(gui);
        Coord center = new Coord(gui.sz.x / 2 - currentTool.sz.x / 2, gui.sz.y / 2 - currentTool.sz.y / 2 - 200);
        gui.add(currentTool, center);
        currentTool.start();

        return Results.SUCCESS();
    }
}
