package nurgling.tasks;

import haven.*;
import nurgling.*;

public class MovingCompletedBySpeed extends NTask {

    Gob gob;

    public MovingCompletedBySpeed(Gob gob) {
        this.gob = gob;
    }

    @Override
    public boolean check() {
        if (NUtils.getGameUI() != null && gob != null) {
            // Speed is 0 or very close to 0 means we've stopped
            return gob.getv() < 0.1;
        }
        return false;
    }
}
