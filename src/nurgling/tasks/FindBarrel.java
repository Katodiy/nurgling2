package nurgling.tasks;

import haven.Widget;
import haven.Window;
import haven.res.ui.barterbox.Shopbox;
import haven.res.ui.relcnt.RelCont;
import nurgling.NUtils;

public class FindBarrel implements NTask
{
    public FindBarrel()
    {

    }


    @Override
    public boolean check()
    {
        Window wnd = NUtils.getGameUI().getWindow("Barrel");
        if (wnd == null)
            return false;
        int count = 0;
        for (Widget w2 = wnd.lchild; w2 != null; w2 = w2.prev)
        {
            if (w2 instanceof RelCont)
            {
                return true;
            }
        }
        return false;
    }
}
