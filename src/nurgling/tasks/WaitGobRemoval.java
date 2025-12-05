package nurgling.tasks;

import nurgling.tools.Finder;

public class WaitGobRemoval extends NTask {
    private final long gobId;

    public WaitGobRemoval(long gobId) {
        this.gobId = gobId;
    }

    @Override
    public boolean check() {
        return Finder.findGob(gobId) == null;
    }
}
