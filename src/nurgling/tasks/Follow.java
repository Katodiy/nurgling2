package nurgling.tasks;

import haven.Following;
import haven.Gob;

public class Follow implements NTask
{
    public Follow(Gob gob)
    {
        this.gob = gob;
    }


    Gob gob;


    @Override
    public boolean check()
    {
        return gob.getattr(Following.class) != null;
    }
}