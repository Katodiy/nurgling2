package nurgling.tasks;

import haven.*;
import haven.res.ui.barterbox.*;
import nurgling.*;

public class FindBarterStand extends NTask
{
    public FindBarterStand()
    {

    }


    @Override
    public boolean check()
    {
        Window wnd = NUtils.getGameUI().getWindow("Barter Stand");
        if (wnd == null)
            return false;
        int count = 0;
        for (Widget w2 = wnd.lchild; w2 != null; w2 = w2.prev)
        {
            if (w2 instanceof Shopbox)
            {
                Shopbox checked = (Shopbox) w2;
                if(checked.price!=null && checked.price.spr == null)
                    return false;
                if(checked.res!=null && checked.spr == null)
                    return false;
                count++;
            }
        }
        return count == 5;
    }
}
