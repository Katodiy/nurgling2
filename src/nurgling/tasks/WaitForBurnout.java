package nurgling.tasks;

import haven.Gob;

import java.util.ArrayList;

/*
 * Waits until ALL of the gobs in the list have a set of attributes which diverges from ALL of the flags passed as a mask
 * Typically used to wait until a group of utilities have all finished burning through their respective tasks before setting them all up again together
 */
public class WaitForBurnout extends NTask
{
    ArrayList<Gob> gobs;
    int flame_flag;

    public WaitForBurnout(ArrayList<Gob> gobs, int flag)
    {
        this.gobs = gobs;
        this.flame_flag = flag;
    }

    @Override
    public boolean check()
    {
        for (Gob gob: gobs)
        {
            if((gob.ngob.getModelAttribute() & flame_flag) != 0)
            {
                return false;
            }
        }
        return true;
    }
}
