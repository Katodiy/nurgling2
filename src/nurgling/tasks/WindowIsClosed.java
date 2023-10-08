package nurgling.tasks;

import haven.*;
import nurgling.*;

public class WindowIsClosed implements NTask
{
    Window wnd;

    public WindowIsClosed(Window wnd)
    {
        this.wnd = wnd;
    }

    @Override
    public boolean check()
    {
        return !NUtils.getGameUI().isWindowExist(wnd);
    }
}
