package nurgling.tasks;

import haven.*;
import nurgling.*;

public class FindNInventory implements NTask
{
    public FindNInventory(String name)
    {
        this.name = name;
    }

    String name;

    @Override
    public boolean check()
    {
        Window wnd = NUtils.getGameUI().getWindow(name);
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
