package nurgling.tasks;

import haven.MCache;
import nurgling.NGameUI;
import nurgling.routes.RoutePoint;

public class WaitForMapLoadPF extends NTask {
    private final RoutePoint routePoint;
    private final NGameUI gui;

    public WaitForMapLoadPF(RoutePoint routePoint, NGameUI gui) {
        this.routePoint = routePoint;
        this.gui = gui;
    }

    @Override
    public boolean check() {
        if(this.routePoint == null) {
            return true;
        }

        if (this.routePoint.toCoord2d(gui.map.glob.map) == null) {
            return false;
        } else {
            boolean canContinue = false;
            for (MCache.Grid grid : gui.map.glob.map.grids.values()) {
                if (grid.id == this.routePoint.gridId) {
                    for(MCache.Grid.Cut cut : grid.cuts) {
                        canContinue = cut.mesh.isReady() && cut.fo.isReady();
                    }
                    return canContinue;
                }
            }

            return true;
        }
    }
}
