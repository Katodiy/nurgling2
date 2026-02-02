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
    private final Integer areaId;
    private final NArea targetArea;

    public GotoArea(NArea targetArea) {
        if (targetArea == null) {
            throw new IllegalArgumentException("NArea must not be null");
        }
        this.targetArea = targetArea;
        this.areaId = null;
    }

    public GotoArea(Map<String, Object> settings) {
        if (settings != null && settings.containsKey("areaId")) {
            this.areaId = (Integer) settings.get("areaId");
            this.targetArea = null;
        } else {
            throw new IllegalArgumentException("Missing 'areaId' in settings");
        }
    }

    private NArea resolveArea(NGameUI gui) {
        if (targetArea != null) {
            return targetArea;
        }
        if (areaId != null) {
            NArea area = gui.map.glob.map.areas.get(areaId);
            if (area == null) {
                throw new IllegalStateException("Area not found: " + areaId);
            }
            return area;
        }
        throw new IllegalStateException("Neither areaId nor NArea provided");
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea area;
        try {
            area = resolveArea(gui);
        } catch (Exception e) {
            return Results.ERROR("Failed to resolve area: " + e.getMessage());
        }

        ChunkNavManager chunkNav = ((NMapView) gui.map).getChunkNavManager();
        if (chunkNav == null || !chunkNav.isInitialized()) {
            return Results.ERROR("ChunkNav not initialized");
        }

        return chunkNav.navigateToArea(area, gui);
    }
}
