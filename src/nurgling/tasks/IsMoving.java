package nurgling.tasks;

import haven.*;
import static haven.OCache.posres;
import nurgling.*;
import static nurgling.actions.PathFinder.pfmdelta;
import nurgling.tools.*;

public class IsMoving implements NTask
{

    Coord2d coord;

    int count = 0;
    public IsMoving(Coord2d coord)
    {
        this.coord = coord;
    }

    int th = 200;
    public IsMoving(Coord2d coord, int th)
    {
        this.coord = coord;
        this.th = th;
    }

    @Override
    public boolean check()
    {
        count++;
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && NUtils.getGameUI().map.player() != null)
        {
            if (NUtils.getGameUI().map.player().rc.dist(coord) <= pfmdelta)
                return true;
            Drawable drawable = (Drawable) NUtils.getGameUI().map.player().getattr(Drawable.class);
            if (drawable != null)
            {
                String pose;
                // Экстренный выход если движение так и началось ( 200 попыток )
                return count > th || drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && NParser.checkName(pose, "borka/walking", "borka/running", "borka/wading");
            }
        }
        return false;
    }
    public boolean getResult()
    {
        return count <= th;
    }
}