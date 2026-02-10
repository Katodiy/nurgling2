package nurgling.tasks;

import haven.*;
import nurgling.*;
import static nurgling.actions.PathFinder.pfmdelta;

public class IsMovingBySpeed extends NTask {

    Coord2d coord;
    Gob gob;
    int count = 0;
    int th = 200;

    public IsMovingBySpeed(Coord2d coord, Gob gob) {
        this.coord = coord;
        this.gob = gob;
    }

    public IsMovingBySpeed(Coord2d coord, Gob gob, int th) {
        this.coord = coord;
        this.gob = gob;
        this.th = th;
    }

    @Override
    public boolean check() {
        count++;
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && NUtils.getGameUI().map.player() != null) {
            // Already at target
            if (NUtils.getGameUI().map.player().rc.dist(coord) <= pfmdelta)
                return true;
            // Timeout or speed > 0 means we're moving
            return count > th || gob.getv() > 0.1;
        }
        return false;
    }

    public boolean getResult() {
        return count <= th;
    }
}
