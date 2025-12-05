package nurgling.tasks;

import haven.*;
import nurgling.*;

public class WaitBlueprintPlacement extends NTask {
    private final BlueprintPlob blueprintPlob;
    
    public WaitBlueprintPlacement(BlueprintPlob blueprintPlob) {
        this.blueprintPlob = blueprintPlob;
    }
    
    @Override
    public boolean check() {
        NMapView mapView = (NMapView) NUtils.getGameUI().map;
        Coord mc = mapView.ui.mc;
        
        if (mc != null && mc.isect(mapView.rootpos(), mapView.sz)) {
            final Coord pc = mc.sub(mapView.rootpos());
            
            // Use Maptest to convert screen coordinates to world coordinates
            // This is how Plob does it
            mapView.new Maptest(pc) {
                public void hit(Coord hitPc, Coord2d worldCoord) {
                    blueprintPlob.adjustPosition(hitPc, worldCoord);
                }
            }.run();
        }
        
        // Check if blueprint was placed or cancelled
        return blueprintPlob.isPlaced() || !blueprintPlob.isActive();
    }
}
