package nurgling.tasks;

import haven.Gob;
import haven.res.ui.croster.CattleId;
import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;

public class AnimalInRoster extends NTask {
    Gob animal;
    Class<? extends Entry> cattleRoster;
    RosterWindow rw;
    boolean done = false;

    public AnimalInRoster(Gob animal, Class<? extends Entry> cattleRoster, RosterWindow rw) {
        this.animal = animal;
        this.cattleRoster = cattleRoster;
        this.rw = rw;
        this.maxCounter = 500;
    }

    @Override
    public boolean check() {
        if (counter++ >= maxCounter) {
            return true;
        }
        if (rw == null || rw.roster(cattleRoster) == null || rw.roster(cattleRoster).entries == null) {
            return true;
        }
        CattleId cid = animal.getattr(CattleId.class);
        if (cid == null) {
            return false;
        }
        Entry entry = (Entry) rw.roster(cattleRoster).entries.get(cid.id);
        if (entry == null) {
            return false;
        }
        if (entry.getClass() == cattleRoster) {
            done = true;
            return true;
        }
        return false;
    }

    public boolean getResult() {
        return done;
    }
}
