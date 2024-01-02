package nurgling.tasks;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.DynamicPf;
import nurgling.tools.NParser;

public class WaitPath implements NTask
{
    DynamicPf.WorkerPf wpf;

    public WaitPath(DynamicPf.WorkerPf wpf)
    {
        this.wpf = wpf;
        (t = new Thread(wpf)).start();
    }

    Thread t;

    @Override
    public boolean check() {

        if (wpf.ready.get()) {
            if (wpf.path != null)
                return true;
            else {
                wpf.ready.set(false);
                (t = new Thread(wpf)).start();
            }
        }
        return false;
    }
}
