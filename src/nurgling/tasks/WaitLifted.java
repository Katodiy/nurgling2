package nurgling.tasks;

import haven.Coord2d;
import haven.Following;
import haven.Gob;
import nurgling.NUtils;

public class WaitLifted implements NTask {
    Gob gob;

    public WaitLifted(Gob gob) {
        this.gob = gob;
    }


    @Override
    public boolean check() {
        Following fl;
        return (fl = gob.getattr(Following.class))!=null && fl.tgt == NUtils.playerID();
    }

}


