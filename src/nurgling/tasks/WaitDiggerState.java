package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChopperProp;
import nurgling.tools.Finder;

public class WaitDiggerState implements NTask
{
    public WaitDiggerState(String msg)
    {
        this.player = NUtils.player();
        this.msg = msg;
    }

    String msg;
    Gob player;
    public enum State
    {
        WORKING,
        NOFREESPACE,
        MSG,
        TIMEFORDRINK,
        DANGER
    }

    State state = State.WORKING;
    @Override
    public boolean check()
    {
        String lastMsg = NUtils.getUI().getLastError();
        String cpose = NUtils.player().pose();
        int space = NUtils.getGameUI().getInventory().calcFreeSpace();
        state = State.WORKING;
        if(lastMsg!=null && lastMsg.contains(msg))
        {
            state = State.MSG;
        }
        else if(cpose != null && cpose.contains("gfx/borka/idle"))
        {
            state = State.MSG;
        }
        else if(space>=0 && space <=1)
        {
            state = State.NOFREESPACE;
        }
        else if(NUtils.getEnergy()<0.22)
        {
            state = State.DANGER;
        }
        else if(NUtils.getStamina()<=0.45)
        {
            state = State.TIMEFORDRINK;
        }
        return state!= State.WORKING;
    }

    public State getState() {
        return state;
    }
}