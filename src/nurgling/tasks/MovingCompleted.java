package nurgling.tasks;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import static nurgling.actions.PathFinder.pfmdelta;
import nurgling.tools.*;

public class MovingCompleted extends NTask
{
    Coord2d target;

    int count = 0;
    public MovingCompleted(Coord2d target)
    {
        this.target = target;
    }

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && NUtils.getGameUI().map.player() != null)
        {
            Drawable drawable = (Drawable) NUtils.getGameUI().map.player().getattr(Drawable.class);
            count++;
            if(count>=1000)
                return true;
            if (drawable != null)
            {
                String pose;
                return (drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && !NParser.checkName(pose, "borka/walking", "borka/running", "borka/wading"));
            }
        }
        return false;
    }
}
