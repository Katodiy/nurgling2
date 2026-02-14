package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChipperProp;
import nurgling.conf.NChopperProp;
import nurgling.tools.Finder;

public class WaitChipperState extends NTask
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

    public WaitChipperState(Gob bumling, boolean ignoreInventoryFull)
    {
        this.player = NUtils.player();
        this.bumling = bumling;
        this.prop = null;
        this.ignoreInventoryFull = ignoreInventoryFull;
    }



    Gob player;
    Gob bumling;
    NChipperProp prop;
    boolean ignoreInventoryFull = false;

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
        int space = NUtils.getGameUI().getInventory().calcFreeSpace();
        if(Finder.findGob(bumling.id)==null)
        {
            state = State.BUMLINGNOTFOUND;
        }
        else if(!ignoreInventoryFull && space <= 1 && space >= 0)
        {
            state = State.TIMEFORPILE;
        }
        else {
            if (NUtils.getEnergy() < 0.36) {
                if (prop!=null && prop.autoeat)
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