package nurgling.tasks;

import haven.Gob;

public class WaitGobModelAttrChange extends NTask {
    Gob gob;
    long currentAttribute;
    public WaitGobModelAttrChange(Gob gob, long currentAttribute) {
        this.gob = gob;
        this.currentAttribute = currentAttribute;
    }


    @Override
    public boolean check() {
        return ((gob.ngob!=null) && (gob.ngob.getModelAttribute())!=this.currentAttribute);
    }
}
