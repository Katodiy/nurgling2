package nurgling.tasks;

import haven.MCache;
import nurgling.NGameUI;
import nurgling.areas.NGlobalCoord;
import nurgling.routes.RoutePoint;

public class WaitForMapLoad extends NTask {
    private final NGameUI gui;

    NGlobalCoord coord;

    public WaitForMapLoad(NGameUI gui, NGlobalCoord coord) {
        this.gui = gui;
        this.coord = coord;
    }

    @Override
    public boolean check() {
        boolean canContinue = false;
        for (MCache.Grid grid : gui.map.glob.map.grids.values()) {
            if(this.coord.getGridId()==0)
                return true;
            if (grid.id == this.coord.getGridId()) {
                for(MCache.Grid.Cut cut : grid.cuts) {
                    canContinue = cut.mesh.isReady() && cut.fo.isReady();
                }
                return canContinue;
            }
        }
        return false;
    }
}
