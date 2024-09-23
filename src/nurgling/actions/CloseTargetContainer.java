package nurgling.actions;

import haven.Window;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.tasks.WindowIsClosed;
import nurgling.tools.Container;

public class CloseTargetContainer implements Action
{
    Container cnt;

    public CloseTargetContainer(Container cnt)
    {
        this.cnt = cnt;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(cnt!=null)
        {
            cnt.update();
            Window wnd = NUtils.getGameUI().getWindow(cnt.cap);
            if(wnd!=null) {
                wnd.wdgmsg("close");
                gui.ui.core.addTask(new WindowIsClosed(wnd));
            }
        }
        return Results.SUCCESS();
    }
}
