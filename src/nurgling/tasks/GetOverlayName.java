package nurgling.tasks;

import haven.*;

public class GetOverlayName extends NTask
{

    Gob gob;

    String name = null;

    public GetOverlayName(Gob gob)
    {
        this.gob = gob;
    }

    @Override
    public boolean check()
    {
        for(Gob.Overlay ol : gob.ols)
        {
            if(ol.spr instanceof StaticSprite)
            {
                return ((StaticSprite)ol.spr).res!=null && !(name = ((StaticSprite)ol.spr).res.name).isEmpty();
            }
        }
        return true;
    }

    public String getName()
    {
        return name;
    }
}
