package nurgling.tasks;

import haven.Following;
import haven.Gob;
import nurgling.NUtils;

public class WaitGobModelAttr implements NTask {
    Gob gob;
    int flag;
    public WaitGobModelAttr(Gob gob, int flag) {
        this.gob = gob;
        this.flag = flag;
    }


    @Override
    public boolean check() {
        return ((gob.ngob!=null) && (gob.ngob.getModelAttribute()&flag)!=0);
    }

}


