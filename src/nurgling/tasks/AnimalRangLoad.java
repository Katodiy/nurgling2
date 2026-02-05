package nurgling.tasks;

import haven.Gob;
import haven.res.gfx.hud.rosters.Rangable;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.areas.NArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

public class AnimalRangLoad extends NTask {
    private final NArea area;
    private final NAlias animal;
    private final Class<? extends Entry> animalClass;
    private final RosterWindow rosterWindow;

    public AnimalRangLoad(NArea area, NAlias animal, Class<? extends Entry> animalClass, RosterWindow rosterWindow) {
        this.area = area;
        this.animal = animal;
        this.animalClass = animalClass;
        this.rosterWindow = rosterWindow;
    }

    @Override
    public boolean check() {
        Gob gob;
        try {
            gob = Finder.findGob(area, animal);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }

        if (gob == null) {
            return false;
        }

        if (rosterWindow.roster(animalClass) == null || rosterWindow.roster(animalClass).entries == null) {
            return false;
        }
        CattleId cid = gob.getattr(CattleId.class);
        if (cid == null) {
            return false;
        }

        rosterWindow.roster(animalClass).setFilterAreaId(area);
        Entry entry = (Entry) rosterWindow.roster(animalClass).entries.get(cid.id);
        if (!(entry instanceof Rangable)) {
            return false;
        }
        return ((Rangable) entry).rang() > 0;
    }
}
