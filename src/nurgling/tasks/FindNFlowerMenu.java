package nurgling.tasks;

import nurgling.*;

public class FindNFlowerMenu implements NTask
{


    public FindNFlowerMenu()
    {
    }

    @Override
    public boolean check()
    {
        res = (NFlowerMenu) NUtils.getUI().findInRoot(NFlowerMenu.class);
        return res != null;
    }

    NFlowerMenu res = null;

    public NFlowerMenu getResult(){
        return res;
    }
}
