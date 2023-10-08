package nurgling.tasks;

import nurgling.*;

public class NFlowerMenuIsClosed implements NTask
{

    public NFlowerMenuIsClosed()
    {
    }

    @Override
    public boolean check()
    {
        return NUtils.getUI().findInRoot(NFlowerMenu.class) == null;
    }
}
