package nurgling.tasks;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.NParser;

public class WaitButcherState implements NTask
{
    public WaitButcherState(Coord itemSize)
    {
        this.player = NUtils.player();
        this.itemSize = itemSize;
    }

    final Coord itemSize;

    Gob player;
    public enum State
    {
        WORKING,
        NOFREESPACE,
        READY
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        if (NParser.checkName(player.pose(), "gfx/borka/idle")) {
            state = State.READY;
        }
        else if (NUtils.getGameUI().getInventory().calcNumberFreeCoord(itemSize)==0) {
            state = State.NOFREESPACE;
        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}