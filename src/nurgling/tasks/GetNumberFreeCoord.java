package nurgling.tasks;

import haven.*;
import nurgling.*;

public class GetNumberFreeCoord implements NTask
{
    public GetNumberFreeCoord(NInventory inv, Coord size)
    {
        this.inv = inv;
        this.size = size;
    }

    public GetNumberFreeCoord(NInventory inv, GItem item)
    {
        this.inv = inv;
        this.item = item;
    }

    public GetNumberFreeCoord(NInventory inv, WItem item)
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
            else {
                Coord lc = item.spr.sz().div(UI.scale(32));
                size = new Coord(lc.y,lc.x);
            }
        freeCoord = inv.calcNumberFreeCoord(size);
        return freeCoord>=0;
    }

    private int freeCoord = -1;

    public int result()
    {
        return freeCoord;
    }
}
