package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import nurgling.tasks.NTask;

public class WaitDir implements NTask {
    double angle;
    Gob gob;
    public WaitDir(Gob gob, Coord2d dir) {
        this.gob = gob;
        angle = dir.curAngle();
    }


    @Override
    public boolean check() {
        return Math.abs(gob.a - angle) < Math.PI/9;
    }
}
