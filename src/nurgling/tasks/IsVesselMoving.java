package nurgling.tasks;

import haven.Composite;
import haven.Drawable;
import haven.Gob;
import haven.LinMove;
import nurgling.NUtils;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class IsVesselMoving implements NTask
{
    Gob gob;


    public IsVesselMoving(Gob gob) {
        this.gob = gob;
    }

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null && gob != null)
        {
            LinMove lm = gob.getattr(LinMove.class);
            if (lm != null)
            {
                return true;
            }
        }
        return false;
    }
}
