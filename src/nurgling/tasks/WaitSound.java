package nurgling.tasks;

import nurgling.NUtils;

public class WaitSound extends NTask
{
    String name;
    public WaitSound(String name)
    {
        NUtils.dropLastSfx();
        this.name = name;
        this.infinite = false;
    }

    @Override
    public boolean check()
    {
        String lastSfx = NUtils.getUI().root.lastSfx;
        done = lastSfx!=null && lastSfx.equals(name);
        return done;
    }
}
