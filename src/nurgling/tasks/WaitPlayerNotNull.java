package nurgling.tasks;

import nurgling.NUtils;

public class WaitPlayerNotNull extends NTask {

    @Override
    public boolean check()
    {
        return NUtils.player() != null;
    }

}
