package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import haven.res.gfx.terobjs.roastspit.Roastspit;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tools.Finder;

public class WaitCarveState extends NTask
{
    public WaitCarveState(Gob pow)
    {
        this.pow = pow;
    }


    Gob pow;
    public enum State
    {
        WORKING,
        NOFREESPACE,
        NOCONTENT
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        Gob.Overlay ol = (pow.findol(Roastspit.class));
        String content = ((Roastspit) ol.spr).getContent();
        if (content == null) {
            state = State.NOCONTENT;
        } else if(NUtils.getGameUI().getInventory().calcFreeSpace()==0)
        {
            state = State.NOFREESPACE;
        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}