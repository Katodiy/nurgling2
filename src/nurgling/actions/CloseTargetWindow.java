package nurgling.actions;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import nurgling.tasks.*;

public class CloseTargetWindow implements Action
{
    Window wnd;

    public CloseTargetWindow(Window wnd)
    {
        this.wnd = wnd;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        if(wnd!=null)
        {
            wnd.wdgmsg("close");
            gui.ui.core.addTask(new WindowIsClosed(wnd));
        }
        return Results.SUCCESS();
    }
}
