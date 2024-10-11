package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChipperProp;
import nurgling.conf.NChopperProp;
import nurgling.tools.Finder;

public class WaitChipperState implements NTask
{
    public WaitChipperState(Gob bumling, NChipperProp prop)
    {
        this.player = NUtils.player();
        this.bumling = bumling;
        this.prop = prop;
    }

    public WaitChipperState(Gob bumling)
    {
        this.player = NUtils.player();
        this.bumling = bumling;
        this.prop = null;
    }



    Gob player;
    Gob bumling;
    NChipperProp prop;
    public enum State
    {
        WORKING,
        BUMLINGNOTFOUND,
        BUMLINGFORDRINK,
        BUMLINGFOREAT,
        DANGER,
        TIMEFORPILE
    }

    State state = State.WORKING;
    @Override
    public boolean check()
    {
        if(Finder.findGob(bumling.id)==null)
        {
            state = State.BUMLINGNOTFOUND;
        }
        else if(NUtils.getEnergy()<0.36)
        {
            if(prop!=null && prop.autoeat)
                state = State.BUMLINGFOREAT;
            if(NUtils.getEnergy()<0.22)
                state = State.DANGER;
        }
        else if(NUtils.getStamina()<=0.45)
        {
            state = State.BUMLINGFORDRINK;
        }
        else if(NUtils.getGameUI().getInventory().calcFreeSpace() == 0)
        {
            state = State.TIMEFORPILE;
        }
        return state!= State.WORKING;
    }

    public State getState() {
        return state;
    }
}