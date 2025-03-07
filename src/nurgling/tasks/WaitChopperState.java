package nurgling.tasks;

import haven.Gob;
import nurgling.NUtils;
import nurgling.conf.NChopperProp;
import nurgling.tools.Finder;

public class WaitChopperState extends NTask
{
    public WaitChopperState(Gob tree, NChopperProp prop)
    {
        this.player = NUtils.player();
        this.tree = tree;
        this.prop = prop;
    }


    Gob player;
    Gob tree;
    NChopperProp prop;
    public enum State
    {
        WORKING,
        TREENOTFOUND,
        TIMEFORDRINK,
        TIMEFOREAT,
        DANGER
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        if (Finder.findGob(tree.id) == null) {
            state = State.TREENOTFOUND;
        } else {
            if (NUtils.getEnergy() < 0.36) {
                if (prop.autoeat)
                    state = State.TIMEFOREAT;
                if (NUtils.getEnergy() < 0.23)
                    state = State.DANGER;
            }
            if (NUtils.getStamina() <= 0.45) {
                state = State.TIMEFORDRINK;
            }
        }


        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }
}