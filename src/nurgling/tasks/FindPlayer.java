package nurgling.tasks;

import haven.*;
import nurgling.*;

public class FindPlayer implements NTask
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