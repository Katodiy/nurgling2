package nurgling.tasks;

import haven.Widget;
import nurgling.NInventory;

public class GetTotalSpace implements NTask
{
    NInventory target;

    public GetTotalSpace(NInventory inv)
    {
        this.target = inv;
    }

    @Override
    public boolean check()
    {

        totalSpace = target.calcTotalSpace();

        return totalSpace >=0;
    }

    private int totalSpace = -1;

    public int result()
    {
        return totalSpace;
    }
}
