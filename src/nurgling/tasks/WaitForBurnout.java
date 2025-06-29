package nurgling.tasks;

import haven.Gob;
import nurgling.tools.Finder;

import java.util.ArrayList;

/*
 * Waits until ALL of the gobs in the list have a set of attributes which diverges from ALL of the flags passed as a mask
 * Typically used to wait until a group of utilities have all finished burning through their respective tasks before setting them all up again together
 */
public class WaitForBurnout extends NTask
{
    ArrayList<Long> gobs;
    int flame_flag;

    public WaitForBurnout(ArrayList<Long> gobs, int flag)
    {
        this.gobs = gobs;
        this.flame_flag = flag;
    }

    @Override
    public boolean check()
    {
        for (Long gobid : gobs) {
            Gob gob = Finder.findGob(gobid);
            if((gob.ngob.getModelAttribute() & flame_flag) != 0)
            {
                return false;
            }
        }
        return true;
    }
}
