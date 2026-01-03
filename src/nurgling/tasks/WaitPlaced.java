package nurgling.tasks;

import haven.Following;
import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.Finder;

public class WaitPlaced extends NTask {
    Long gobid;

    public WaitPlaced(Long gob) {
        this.gobid = gob;
    }


    @Override
    public boolean check() {
        Gob gob = Finder.findGob(gobid);
        return gob!=null && gob.getattr(Following.class)==null;
    }

}


