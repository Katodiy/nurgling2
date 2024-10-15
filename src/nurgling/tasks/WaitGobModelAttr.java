package nurgling.tasks;

import haven.Following;
import haven.Gob;
import nurgling.NUtils;

public class WaitGobModelAttr implements NTask {
    Gob gob;
    int flag;
    int count =0;
    public WaitGobModelAttr(Gob gob, int flag) {
        this.gob = gob;
        this.flag = flag;
    }


    @Override
    public boolean check() {
        count++;
        return (((gob.ngob!=null) && (gob.ngob.getModelAttribute()&flag)!=0)) || count>100;

    }

}


