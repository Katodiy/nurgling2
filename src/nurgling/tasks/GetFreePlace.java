package nurgling.tasks;

import haven.*;
import nurgling.*;

public class GetFreePlace implements NTask
{
    public GetFreePlace(NInventory inv, Coord size)
    {
        this.inv = inv;
        this.size = size;
    }

    public GetFreePlace(NInventory inv, GItem item)
    {
        this.inv = inv;
        this.item = item;
    }

    public GetFreePlace(NInventory inv, WItem item)
    {
        this(inv,item.item);
    }

    NInventory inv;
    Coord size;

    GItem item = null;

    @Override
    public boolean check()
    {
        if(item!=null)
            if(item.spr == null)
                return false;
            else
                size = item.spr.sz().div(UI.scale(32));
        freeCoord = inv.findFreeCoord(size);
        return freeCoord!=null;
    }

    private Coord freeCoord = null;

    public Coord result()
    {
        return freeCoord;
    }
}
