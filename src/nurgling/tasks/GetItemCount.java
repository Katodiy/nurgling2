package nurgling.tasks;

import haven.*;
import haven.res.ui.tt.cn.*;
import nurgling.iteminfo.*;

public class GetItemCount extends NTask
{

    WItem item;

    public GetItemCount(WItem item)
    {
        this.item = item;
    }

    float res = -1;

    @Override
    public boolean check()
    {
        if (item.item.info != null)
        {
            for (ItemInfo inf : item.item.info)
            {
                if (inf instanceof CustomName)
                {
                    if ((res = ((CustomName) inf).count) != -1)
                    {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public float getResult()
    {
        return res;
    }
}
