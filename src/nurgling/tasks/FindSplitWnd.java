package nurgling.tasks;

import haven.*;
import nurgling.*;

public class FindSplitWnd extends NTask
{
    public FindSplitWnd()
    {
    }

    @Override
    public boolean check()
    {
        Window wnd = NUtils.getUI().findInRoot("Split");
        if (wnd == null)
            return false;

        for (Widget w2 = wnd.lchild; w2 != null; w2 = w2.prev)
        {
            if (w2 instanceof TextEntry)
            {
                res = wnd;
                return true;
            }
        }

        return false;
    }

    Window res;

    public Window getResult()
    {
        return res;
    }
}
