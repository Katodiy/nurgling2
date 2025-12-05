package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.areas.NArea;

import java.awt.Color;
import java.util.*;

/**
 * Overlay for displaying areas shared via chat (@Area format)
 * Uses MCache.Overlay like Selector to properly render areas
 */
public class NChatAreaOverlay {
    private final List<MCache.Overlay> overlays = new ArrayList<>();
    private final long startTime;
    private static final long DURATION_MS = 12000; // 12 seconds
    
    // Yellow semi-transparent material like Selector
    private static final MCache.OverlayInfo areaol = new MCache.OverlayInfo() {
        final Material mat = new Material(new BaseColor(255, 255, 0, 48), States.maskdepth);
        
        public Collection<String> tags() {
            return Arrays.asList("show");
        }
        
        public Material mat() {
            return mat;
        }
    };
    
    public NChatAreaOverlay(MCache map, NArea.Space space) {
        this.startTime = System.currentTimeMillis();
        
        // Create overlay for each grid area in the space
        for(Map.Entry<Long, NArea.VArea> entry : space.space.entrySet()) {
            long gridId = entry.getKey();
            Area area = entry.getValue().area;
            if(area != null) {
                try {
                    // Find the grid
                    MCache.Grid grid = map.findGrid(gridId);
                    if(grid == null) continue;
                    
                    // Convert local grid tile coordinates to world tile coordinates
                    // area.ul and area.br are in local grid tile coords (0-99)
                    // grid.gc is the grid coordinate
                    Coord worldUL = grid.gc.mul(MCache.cmaps).add(area.ul);
                    Coord worldBR = grid.gc.mul(MCache.cmaps).add(area.br);
                    
                    // Create world area
                    Area worldArea = new Area(worldUL, worldBR);
                    
                    MCache.Overlay ol = map.new Overlay(worldArea, areaol);
                    overlays.add(ol);
                } catch(Exception e) {
                    // Skip if overlay creation fails
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void destroy() {
        for(MCache.Overlay ol : overlays) {
            try {
                ol.destroy();
            } catch(Exception e) {
                // Ignore
            }
        }
        overlays.clear();
    }
    
    public boolean isExpired() {
        long elapsed = System.currentTimeMillis() - startTime;
        if(elapsed >= DURATION_MS) {
            destroy();
            return true;
        }
        return false;
    }
}
