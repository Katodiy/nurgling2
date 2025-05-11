package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import nurgling.NUtils;

public class WaitDistance extends NTask {
    Coord2d last;
    double dist;

    public WaitDistance(Coord2d last, double dist) {
        this.last = last;
        this.dist = dist;
    }

    @Override
    public boolean check() {
        Gob player = NUtils.player();
        if (player == null)
            return false;

        return player.rc.dist(last) >= dist;
    }
}
