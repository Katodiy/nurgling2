package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;

public class WaitDirTrol implements NTask {
    double angle;
    Gob gob;
    public WaitDirTrol(Gob gob, double old) {
        this.gob = gob;
        angle = old;
    }


    @Override
    public boolean check() {
        return gob.a != angle;
    }
}
