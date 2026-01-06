package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.navigation.ChunkNavManager;

import java.util.Map;

/**
 * Navigates to a specified area using the ChunkNav system.
 * This bot is designed for use in scenarios only.
 */
public class GotoArea implements Action {
    private Integer areaId = null;

    public GotoArea() {
    }

    public GotoArea(Map<String, Object> settings) {
        if (settings != null && settings.containsKey("areaId")) {
            this.areaId = (Integer) settings.get("areaId");
        }
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (areaId == null) {
            return Results.ERROR("No area selected");
        }

        NArea area = gui.map.glob.map.areas.get(areaId);
        if (area == null) {
            return Results.ERROR("Area not found: " + areaId);
        }

        ChunkNavManager chunkNav = ((NMapView) gui.map).getChunkNavManager();
        if (chunkNav == null || !chunkNav.isInitialized()) {
            return Results.ERROR("ChunkNav not initialized");
        }

        return chunkNav.navigateToArea(area, gui);
    }
}
