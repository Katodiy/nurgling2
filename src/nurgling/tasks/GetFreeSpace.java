package nurgling.tasks;

import haven.*;
import nurgling.*;

public class GetFreeSpace implements NTask
{
    Widget target;

    public GetFreeSpace(Widget inv)
    {
        this.target = inv;
    }

    @Override
    public boolean check()
    {
        if(target instanceof NInventory)
        {
            freeSpace = ((NInventory)target).calcFreeSpace();
        }
        else if(target instanceof NISBox)
        {
            freeSpace = ((NISBox)target).calcFreeSpace();
        }
        return freeSpace>=0;
    }

    private int freeSpace = -1;

    public int result()
    {
        return freeSpace;
    }
}
