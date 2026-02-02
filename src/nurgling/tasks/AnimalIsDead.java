package nurgling.tasks;

import haven.Gob;
import haven.MCache;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NParser;

public class AnimalIsDead extends NTask {
    long gobid;

    public AnimalIsDead(long gobid) {
        this.gobid = gobid;
        this.maxCounter = 500;
        this.infinite = false;
    }

    @Override
    public boolean check() {
        Gob animal = Finder.findGob(gobid);
        if (animal != null && animal.pose() != null && NParser.checkName(animal.pose(), "knock")) {
            done = true;
            return true;
        }
        return (NUtils.getGameUI().prog == null || NUtils.getGameUI().prog.prog < 0) && animal.rc.dist(NUtils.player().rc) > MCache.tilesz.len() * 2;
    }
}
