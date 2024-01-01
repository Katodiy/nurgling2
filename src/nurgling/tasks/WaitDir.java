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

    protected WaitDir(Gob gob) {
        this.gob = gob;
    }


    @Override
    public boolean check() {
        return Math.abs(gob.a - angle) < Math.PI/9;
    }

    public static class WaitDirTrol extends WaitDir {
        public WaitDirTrol(Gob gob, double old) {
            super(gob);
            angle = old;
        }

        @Override
        public boolean check() {
            return gob.a != angle;
        }
    }
}


