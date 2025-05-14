package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;

public class WaitForNoGobWithHash extends NTask {

    private final String hash;

    public WaitForNoGobWithHash(String hash) {
        this.hash = hash;
    }

    @Override
    public boolean check() {
        if(hash == null) {
            return true;
        }

        Gob gob = Finder.findGob(hash);

        return gob == null;
    }
}