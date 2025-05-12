package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;

public class WaitForGobWithHash extends NTask {

    private final String hash;

    public WaitForGobWithHash(String hash) {
        this.hash = hash;
    }

    @Override
    public boolean check() {
        if(hash == null) {
            return true;
        }

        Gob gob = Finder.findGob(hash);

        if (gob != null) {
            return true;
        }

        return false;
    }
}