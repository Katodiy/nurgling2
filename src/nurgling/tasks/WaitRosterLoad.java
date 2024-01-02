package nurgling.tasks;

import haven.res.ui.croster.Entry;
import haven.res.ui.croster.RosterWindow;
import nurgling.NUtils;

public class WaitRosterLoad implements NTask {
    Class<? extends Entry> cattleRoster;
    public WaitRosterLoad(Class<? extends Entry> cattleRoster) {
        this.cattleRoster = cattleRoster;
    }

    @Override
    public boolean check() {
        RosterWindow rw = (RosterWindow)NUtils.getGameUI().getWindow("Cattle Roster");
        if(rw == null)
            return false;
        return rw.isLoaded(cattleRoster);
    }
}


