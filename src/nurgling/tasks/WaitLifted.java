package nurgling.tasks;

import haven.Coord2d;
import haven.Following;
import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.Finder;

public class WaitLifted extends NTask {
    private final long gobid;

    public WaitLifted(Gob gob) {
        this.gobid = (gob == null) ? -1 : gob.id;
        this.maxCounter = 50;
    }

    @Override
    public boolean check() {
        if (gobid < 0)
            return false;
        Gob gob = Finder.findGob(gobid);
        if (gob == null)
            return false;
        Following fl = gob.getattr(Following.class);
        return (fl != null) && (fl.tgt == NUtils.playerID());
    }
}


