package nurgling.tasks;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.DynamicPf;
import nurgling.tools.NParser;

public class WaitSound implements NTask
{
    String name;
    long count;
    boolean done = false;
    public WaitSound(String name)
    {
        NUtils.dropLastSfx();
        count = NUtils.getTickId();
    }

    @Override
    public boolean check()
    {
        count++;
        if(count>500)
        {
            done = false;
            return true;
        }
        String lastSfx = NUtils.getUI().root.lastSfx;
        done = lastSfx!=null && lastSfx.equals(name);
        return done;
    }

    public boolean getResult() {
        return done;
    }
}
