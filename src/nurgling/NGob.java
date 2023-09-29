package nurgling;

import haven.*;

public class NGob
{
    public NHitBox hitBox = null;
    String name;

    public void checkattr(GAttrib a, long id)
    {
        if (a instanceof Drawable && ((Drawable) a).getres() != null && ((Drawable) a).getres().getLayers() != null)
        {
            name = ((Drawable) a).getres().name;
            for (Resource.Layer lay : ((Drawable) a).getres().getLayers())
            {
                if (lay instanceof Resource.Neg)
                {
                    hitBox = new NHitBox(((Resource.Neg) lay).ac, ((Resource.Neg) lay).bc);
                }
            }
            if (name != null)
            {
                NHitBox custom = NHitBox.findCustom(name);
                if (custom != null)
                {
                    hitBox = custom;
                }
            }
            if (NUtils.getGameUI()!=null && hitBox != null)
            {
                try
                {
                    NUtils.getUI().core.pfMap.processGob(id);
                }
                catch (NPFMap.NPFMapException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
