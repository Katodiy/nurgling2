package nurgling.tasks;

import nurgling.*;
import nurgling.areas.*;

public class SelectArea extends NTask
{

    public SelectArea()
    {
    }

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI().map!=null )
            if(!((NMapView)NUtils.getGameUI().map).isAreaSelectionMode.get())
            {
                if (((NMapView) NUtils.getGameUI().map).areaSpace != null)
                {
                    result = ((NMapView) NUtils.getGameUI().map).areaSpace;
                    ((NMapView) NUtils.getGameUI().map).areaSpace = null;
                }
                return true;
            }
        return false;
    }

    public NArea.Space getResult()
    {
        return result;
    }

    NArea.Space result = null;
}
