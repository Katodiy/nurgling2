package nurgling.tasks;

import nurgling.NFlowerMenu;
import nurgling.NUtils;

public class FindNFlowerMenuT extends NTask
{
    int count = 0;
    NFlowerMenu res = null;

    public FindNFlowerMenuT()
    {
    }

    @Override
    public boolean check()
    {
        count++;
        res = (NFlowerMenu) NUtils.getUI().findInRoot(NFlowerMenu.class);
        return res != null || count > 40;
    }

    public NFlowerMenu getResult(){
        return res;
    }
}
