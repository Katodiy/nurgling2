package nurgling.tasks;

import haven.res.gfx.hud.rosters.Rangable;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.areas.NArea;

/**
 * Waits until every roster entry for the given area has rang loaded (rang() > 0).
 * Used after MemorizeAnimalsAction so that KillAnimalsAction does not treat newly memorized animals as worst (rang 0) and add them to forkill.
 */
public class WaitAllAnimalsRangLoaded extends NTask {
    private final NArea area;
    private final Class<? extends Entry> animalClass;
    private final RosterWindow rosterWindow;

    public WaitAllAnimalsRangLoaded(NArea area, Class<? extends Entry> animalClass, RosterWindow rosterWindow) {
        this.area = area;
        this.animalClass = animalClass;
        this.rosterWindow = rosterWindow;
        this.maxCounter = 500;
    }

    @Override
    public boolean check() {
        if (rosterWindow == null || rosterWindow.roster(animalClass) == null || rosterWindow.roster(animalClass).entries == null) {
            return false;
        }

        for (Object o : rosterWindow.roster(animalClass).entries.values()) {
            Entry entry = (Entry) o;
            if (entry.areaId != area.id) {
                continue;
            }
            if (entry instanceof Rangable && ((Rangable) entry).rang() <= 0) {
                return false;
            }
        }
        return true;
    }
}
