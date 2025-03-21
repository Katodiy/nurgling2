package nurgling.tasks;

import haven.*;
import nurgling.*;

public class FindPlayer extends NTask
{
    public FindPlayer()
    {
    }


    @Override
    public boolean check()
    {
        return NUtils.player()!=null;
    }
}