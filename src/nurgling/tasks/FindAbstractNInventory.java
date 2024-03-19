package nurgling.tasks;

import haven.Widget;
import haven.Window;
import nurgling.NInventory;
import nurgling.NUtils;

import java.util.ArrayList;

public class FindAbstractNInventory implements NTask
{
    public FindAbstractNInventory(ArrayList<String> candidates)
    {
        this.candidates = candidates;
    }

    ArrayList<String> candidates;

    @Override
    public boolean check()
    {
        Window wnd = null;
        for(String name : candidates) {
            wnd = NUtils.getGameUI().getWindow(name);
            if(wnd!=null) {
                resname = name;
                break;
            }
        }
        if(wnd == null)
            return false;
        for(Widget w2 = wnd.lchild ; w2 !=null ; w2= w2.prev )
        {
            if ( w2 instanceof NInventory ) {
                inv = (NInventory) w2;
                return true;
            }
        }
        return false;
    }

    String resname;
    NInventory inv;

    public String getCap() {
        return resname;
    }

    public NInventory getInv() {
        return inv;
    }
}
