package nurgling.tasks;

import haven.*;
import haven.res.ui.stackinv.ItemStack;
import nurgling.*;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.tools.StackSupporter;
import nurgling.tools.VSpec;

import java.util.ArrayList;
import java.util.HashSet;

public class GetNumberFreeCoord extends NTask
{
    private NInventory inv;
    private Coord size;
    private GItem item = null;
    String name = null;
    public GetNumberFreeCoord(NInventory inv, Coord size)
    {
        this.inv = inv;
        this.size = size;
    }


    public GetNumberFreeCoord(NInventory inv, GItem item)
    {
        this.inv = inv;
        this.item = item;
        this.name = ((NGItem)item).name();
    }

    public GetNumberFreeCoord(NInventory inv, WItem item)
    {
        this(inv,item.item);
    }

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
