package nurgling.tasks;

import haven.Following;
import haven.Gob;
import nurgling.NUtils;

public class ChangeModelAtrib extends NTask {
    Gob gob;
    long old;

    public ChangeModelAtrib(Gob gob, long old) {
        this.gob = gob;
        this.old = old;
    }


    @Override
    public boolean check() {
        return gob.ngob.getModelAttribute()!=old;
    }

}


