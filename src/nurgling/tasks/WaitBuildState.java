package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tools.Finder;
import nurgling.tools.NParser;

public class WaitBuildState implements NTask
{
    public WaitBuildState()
    {
        this.player = NUtils.player();
    }


    Gob player;
    public enum State
    {
        WORKING,
        TIMEFORDRINK,
        DANGER,
        READY
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        if (NUtils.getEnergy() < 0.22) {
            state = State.DANGER;
        } else if (NUtils.getStamina() <= 0.45) {
            state = State.TIMEFORDRINK;
        } else if (NParser.checkName(player.pose(), "gfx/borka/idle")) {
            state = State.READY;
        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}