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
        if(lastMsg!=null && lastMsg.contains(msg))
        {
            state = State.MSG;
        }
        else if(cpose != null && cpose.contains("gfx/borka/idle"))
        {
            state = State.MSG;
        }
        else if(NUtils.getEnergy()<0.22)
        {
            state = State.DANGER;
        }
        else if(NUtils.getStamina()<=0.45)
        {
            state = State.TIMEFORDRINK;
        }
        else if(NUtils.getGameUI().getInventory().calcFreeSpace()==0)
        {
            state = State.NOFREESPACE;
        }
        return state!= State.WORKING;
    }

    public State getState() {
        return state;
    }
}