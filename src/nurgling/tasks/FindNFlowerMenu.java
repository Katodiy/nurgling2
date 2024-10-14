package nurgling.tasks;

import nurgling.*;

public class FindNFlowerMenu implements NTask
{
    final long startTime;
    final long startFrame;

    public FindNFlowerMenu()
    {
        startTime = System.currentTimeMillis();
        startFrame = NUtils.getTickId();
    }

    @Override
    public boolean check()
    {
        res = (NFlowerMenu) NUtils.getUI().findInRoot(NFlowerMenu.class);
        return res != null || ( NUtils.getTickId()- startFrame) > 240 || System.currentTimeMillis() - startTime > 4000;
    }

    NFlowerMenu res = null;

    public NFlowerMenu getResult(){
        return res;
    }
}
