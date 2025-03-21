package nurgling.tasks;

import haven.Composite;
import haven.Coord2d;
import haven.Drawable;
import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import static nurgling.actions.PathFinder.pfmdelta;

public class IsPoseMov extends NTask
{

    Coord2d coord;
    Gob gob;
    NAlias poses;


    public IsPoseMov(Coord2d coord, Gob gob, NAlias poses) {
        this.coord = coord;
        this.gob = gob;
        this.poses = poses;
    }

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && gob != null)
        {
            if (gob.rc.dist(coord) <= pfmdelta)
                return true;
            Drawable drawable = (Drawable) gob.getattr(Drawable.class);
            if (drawable != null)
            {
                String pose;
                // Экстренный выход если движение так и началось ( 200 попыток )
                return  drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && NParser.checkName(pose, poses);
            }
        }
        return false;
    }
    public boolean getResult()
    {
        return true;
    }
}