package nurgling.tasks;

import haven.Coord2d;
import haven.Gob;
import haven.res.ui.croster.RosterWindow;
import nurgling.NUI;
import nurgling.NUtils;

public class WaitRosterLoad implements NTask {

    public WaitRosterLoad() {
    }

    @Override
    public boolean check() {
        RosterWindow rw = (RosterWindow)NUtils.getGameUI().getWindow("Cattle Roster");
        if(rw == null)
            return false;
        return rw.allLoaded();
    }
}


