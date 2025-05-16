package nurgling.tasks;

import haven.Widget;
import haven.Window;
import nurgling.NInventory;
import nurgling.NUtils;

public class FindNInventoryCont extends NTask
{
    public FindNInventoryCont(String name)
    {
        this.name = name;
    }

    String name;

    @Override
    public boolean check()
    {
        Window wnd = NUtils.getGameUI().getWindowContains(name);
        if(wnd == null)
            return false;
        for(Widget w2 = wnd.lchild ; w2 !=null ; w2= w2.prev )
        {
            if ( w2 instanceof NInventory ) {
                return true;
            }
        }
        return false;
    }
}
