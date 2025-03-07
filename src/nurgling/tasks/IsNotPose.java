package nurgling.tasks;

import haven.Composite;
import haven.Coord2d;
import haven.Drawable;
import haven.Gob;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class IsNotPose extends NTask
{
    Gob gob;
    NAlias poses;

    public IsNotPose(Gob gob, NAlias poses) {
        this.gob = gob;
        this.poses = poses;
    }

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && gob != null)
        {
            Drawable drawable = (Drawable) gob.getattr(Drawable.class);
            if (drawable != null)
            {
                String pose;
                return (drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && !NParser.checkName(pose, poses));
            }
        }
        return false;
    }
}
