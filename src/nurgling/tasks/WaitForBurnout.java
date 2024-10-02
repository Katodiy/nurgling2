package nurgling.tasks;

import haven.GItem;
import haven.Gob;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NISBox;
import nurgling.NInventory;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class WaitForBurnout implements NTask
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
