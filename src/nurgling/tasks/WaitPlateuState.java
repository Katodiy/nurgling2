package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChipperProp;
import nurgling.tools.Finder;

public class WaitPlateuState implements NTask
{
    public WaitPlateuState( NChipperProp prop)
    {
        this.player = NUtils.player();
        this.prop = prop;
    }


    Gob player;
    NChipperProp prop;
    public enum State
    {
        WORKING,
        BUMLINGFORDRINK,
        BUMLINGFOREAT,
        DANGER,
        TIMEFORPILE
    }

    State state = State.WORKING;
    @Override
    public boolean check()
    {
        if(NUtils.getEnergy()<0.36)
        {
            if(prop.autoeat)
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