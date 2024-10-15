package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;
import nurgling.conf.NPrepBoardsProp;
import nurgling.tools.Finder;

public class WaitPrepBoardsState implements NTask
{
    public WaitPrepBoardsState(Gob log, NPrepBoardsProp prop)
    {
        this.player = NUtils.player();
        this.log = log;
        this.prop = prop;
    }


    Gob player;
    Gob log;
    NPrepBoardsProp prop;
    public enum State
    {
        WORKING,
        LOGNOTFOUND,
        TIMEFORDRINK,
        DANGER,
        NOFREESPACE
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        int space = NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(4, 1));
        if (Finder.findGob(log.id) == null) {
            state = State.LOGNOTFOUND;
        } else if (NUtils.getEnergy() < 0.22) {
            state = State.DANGER;
        } else if (NUtils.getStamina() <= 0.45) {
            state = State.TIMEFORDRINK;
        } else if (space <= 1 && space>=0) {
            if(NUtils.getGameUI().getInventory().calcFreeSpace()<=4 || space==0)
                state = State.NOFREESPACE;
        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}