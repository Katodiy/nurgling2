package nurgling.tasks;


import haven.Coord;
import nurgling.NUtils;

public class CheckGrid extends NTask {
    // Short timeout for grid checks in map upload - prevents queue buildup
    private static final int GRID_CHECK_MAX_FRAMES = 100; // ~0.5 sec at 200fps
    
    Coord current;
    boolean aborted = false;
    private int frameCount = 0;

    public CheckGrid(Coord current) {
        if(current.x == 0 && current.y == 0) {
            int a = 0;
        }
        this.current = current;
    }

    @Override
    public boolean check() {
        // Quick timeout to prevent blocking on unavailable grids
        if (frameCount++ >= GRID_CHECK_MAX_FRAMES) {
            aborted = true;
            return true;
        }
        
        boolean res = NUtils.getUI().sess.glob.map.checkGrid(current);
        if(res)
            return true;
        else
        {
            res = NUtils.getUI().sess.glob.map.inReq(current);
            if(res) {
                aborted = true;
                return true;
            }
        }
        return false;
    }

    public boolean status()
    {
        return aborted;
    }
}
