package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NFishingSettings;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tools.Finder;
import nurgling.tools.NParser;

public class FishingTask implements NTask
{
    public FishingTask(NFishingSettings prop)
    {
        this.player = NUtils.player();
        this.prop = prop;
    }


    Gob player;
    NFishingSettings prop;

    public enum State
    {
        NEEDREP,
        NOFREESPACE,
        SPINWND,
        NOFISH,
        WORKING
    }


    State state = State.WORKING;
    @Override
    public boolean check() {
        int space = NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(2, 3));
        String err;
        if (space == 0) {
            state = State.NOFREESPACE;
        } else if ((err = NUtils.getUI().getLastError()) != null || NParser.checkName(player.pose(), "gfx/borka/idle")) {
            if (err!=null && err.contains("around"))
                state = State.NOFISH;
            else
                state = State.NEEDREP;
        } else if (NUtils.getGameUI().getWindow("This is bait") != null) {
            state = State.SPINWND;
        }

        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}