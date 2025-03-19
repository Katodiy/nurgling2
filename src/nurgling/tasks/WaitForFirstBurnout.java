package nurgling.tasks;

import haven.Gob;

import java.util.ArrayList;

/*
 * Waits until ANY of the gobs in the list has a set of attributes which diverges from ANY of the flags passed as a mask
 * This is useful for waiting for the first gob which is not active across all of multiple possible flags
 * e.g. waiting until some cauldron is either not burning or not full of water so it can be addressed without waiting for all of them to be out of both fuel and water
 * Contrast with WaitForBurnout which waits for all gobs to not match all of the passed flags
 */

public class WaitForFirstBurnout extends NTask
{
    ArrayList<Gob> gobs;
    int flame_flag;

    public WaitForFirstBurnout(ArrayList<Gob> gobs, int flag)
    {
        this.gobs = gobs;
        this.flame_flag = flag;
    }

    @Override
    public boolean check()
    {
        for (Gob gob: gobs)
        {
            if((gob.ngob.getModelAttribute() & flame_flag) != flame_flag)
            {
                return true;
            }
        }
        return false;
    }
}
