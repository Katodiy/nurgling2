package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tools.Finder;

public class WaitCollectState extends NTask
{
    public WaitCollectState(Gob target,Coord targetCoord)
    {
        this.player = NUtils.player();
        this.target = target;
        this.targetCoord = targetCoord;
    }


    Gob player;
    Gob target;
    Coord targetCoord;
    public enum State
    {
        WORKING,
        NOFREESPACE,
        NOITEMSFORCOLLECT
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        String cpose = NUtils.player().pose();
        if (cpose != null && cpose.contains("gfx/borka/idle")) {
            state = State.NOITEMSFORCOLLECT;
        } else if (NUtils.getGameUI().getInventory().calcNumberFreeCoord(targetCoord) == 0) {
            state = State.NOFREESPACE;
        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}