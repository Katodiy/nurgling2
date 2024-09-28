package nurgling.tasks;

import haven.Composite;
import haven.Coord2d;
import haven.Drawable;
import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import static nurgling.actions.PathFinder.pfmdelta;

public class IsPoseMov implements NTask
{

    Coord2d coord;
    Gob gob;
    NAlias poses;

    int count = 0;

    public IsPoseMov(Coord2d coord, Gob gob, NAlias poses) {
        this.coord = coord;
        this.gob = gob;
        this.poses = poses;
    }

    public IsPoseMov(Coord2d coord, Gob gob, NAlias poses, int th) {
        this.coord = coord;
        this.gob = gob;
        this.poses = poses;
        this.th = th;
    }

    int th = 200;
    @Override
    public boolean check()
    {
        count++;
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && gob != null)
        {
            if (gob.rc.dist(coord) <= pfmdelta)
                return true;
            Drawable drawable = (Drawable) gob.getattr(Drawable.class);
            if (drawable != null)
            {
                String pose;
                // Экстренный выход если движение так и началось ( 200 попыток )
                return count > th || drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && NParser.checkName(pose, poses);
            }
        }
        return false;
    }
    public boolean getResult()
    {
        return count <= th;
    }
}