package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.List;

public class WaitForGobsWithNAlias extends NTask {

    private final NAlias alias;

    public WaitForGobsWithNAlias(NAlias alias) {
        this.alias = alias;
    }

    @Override
    public boolean check() {
        if(alias == null) {
            return true;
        }

        List<Gob> gobs = Finder.findGobs(alias);

        return !gobs.isEmpty();
    }
}
