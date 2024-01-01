package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;

public class WaitPos implements NTask {
    Coord2d pos;
    Gob gob;
    public WaitPos(Gob gob, Coord2d pos) {
        this.gob = gob;
        this.pos = pos;
    }

    protected WaitPos(Gob gob) {
        this.gob = gob;
    }


    @Override
    public boolean check() {
        return Math.abs(gob.rc.dist(pos))<0.11;
    }


}


