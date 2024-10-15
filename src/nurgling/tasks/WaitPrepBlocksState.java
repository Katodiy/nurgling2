package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChopperProp;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tools.Finder;

public class WaitPrepBlocksState implements NTask
{
    public WaitPrepBlocksState(Gob log, NPrepBlocksProp prop)
    {
        this.player = NUtils.player();
        this.log = log;
        this.prop = prop;
    }


    Gob player;
    Gob log;
    NPrepBlocksProp prop;
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
        int space = NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(1, 2));
        if (Finder.findGob(log.id) == null) {
            state = State.LOGNOTFOUND;
        } else if (NUtils.getEnergy() < 0.22) {
            state = State.DANGER;
        } else if (NUtils.getStamina() <= 0.45) {
            state = State.TIMEFORDRINK;
        } else if (space <= 1 && space >=0) {
            state = State.NOFREESPACE;
        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}