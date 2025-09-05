package nurgling.tasks;

import nurgling.NUtils;

public class WaitSound extends NTask
{
    String name;
    long count = 0;
    boolean done = false;
    public WaitSound(String name)
    {
        NUtils.dropLastSfx();
        this.name = name;
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
