package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChipperProp;
import nurgling.tools.Finder;

public class WaitPlateuState extends NTask
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
        int space = NUtils.getGameUI().getInventory().calcFreeSpace();
        if(space <= 1 && space >= 0)
        {
            state = State.TIMEFORPILE;
        }
        else {
            if (NUtils.getEnergy() < 0.36) {
                if (prop.autoeat)
                    state = State.BUMLINGFOREAT;
                if (NUtils.getEnergy() < 0.23)
                    state = State.DANGER;
            }
            if (NUtils.getStamina() <= 0.45) {
                state = State.BUMLINGFORDRINK;
            }
        }
        return state!= State.WORKING;
    }

    public State getState() {
        return state;
    }
}