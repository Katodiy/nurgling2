package nurgling.tasks;

import haven.Gob;
import haven.LinMove;
import nurgling.NUtils;

public class IsVesselNotMoving extends NTask
{
    Gob gob;


    public IsVesselNotMoving(Gob gob) {
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
                return false;
            }
        }
        return true;
    }
}
