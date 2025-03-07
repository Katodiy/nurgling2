package nurgling.tasks;

import haven.Gob;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.areas.NArea;

public class SelectGob extends NTask
{

    public SelectGob()
    {
    }

    @Override
    public boolean check()
    {
        if (NUtils.getGameUI().map!=null )
            if(!((NMapView)NUtils.getGameUI().map).isGobSelectionMode.get())
            {
                if (((NMapView) NUtils.getGameUI().map).selectedGob != null)
                {
                    result = ((NMapView) NUtils.getGameUI().map).selectedGob;
                    ((NMapView) NUtils.getGameUI().map).selectedGob = null;
                }
                return true;
            }
        return false;
    }

    public Gob getResult()
    {
        return result;
    }

    Gob result = null;
}
