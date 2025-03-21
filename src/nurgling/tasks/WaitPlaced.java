package nurgling.tasks;

import haven.Following;
import haven.Gob;
import nurgling.NUtils;

public class WaitPlaced extends NTask {
    Gob gob;

    public WaitPlaced(Gob gob) {
        this.gob = gob;
    }


    @Override
    public boolean check() {
        return gob.getattr(Following.class)==null;
    }

}


