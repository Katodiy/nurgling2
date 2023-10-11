package nurgling;

import haven.*;
import nurgling.overlays.*;
import nurgling.pf.*;
import nurgling.tools.*;

import java.util.*;

public class NGob
{
    public NHitBox hitBox = null;
    public String name = null;

    final Gob parent;
    public NGob(Gob parent)
    {
        this.parent = parent;
    }

    public void checkattr(GAttrib a, long id)
    {

            if (a instanceof ResDrawable)
            {
                modelAttribute = ((ResDrawable) a).calcMarker();
            }

            if (a instanceof Drawable)
            {
                if (((Drawable) a).getres() != null)
                {
                    name = ((Drawable) a).getres().name;
                    if (((Drawable) a).getres().getLayers() != null)
                    {
                        for (Resource.Layer lay : ((Drawable) a).getres().getLayers())
                        {
                            if (lay instanceof Resource.Neg)
                            {
                                hitBox = new NHitBox(((Resource.Neg) lay).ac, ((Resource.Neg) lay).bc);
                            }
                        }
                        if (name != null)
                        {
                            if (NParser.checkName(name, new NAlias("plants")))
                            {
                                parent.addcustomol(new NCropMarker(parent));
                            }

                            NHitBox custom = NHitBox.findCustom(name);
                            if (custom != null)
                            {
                                hitBox = custom;
                            }
                        }
                        if (hitBox != null)
                        {
                            parent.addcustomol(new NModelBox(parent));
                            new CellsArray(parent);
                            // TODO пока не ясно, есть ли ситуации когда обновление объекта скажывается на изменени сетки пф
                            if (NUtils.getGameUI() != null)
                            {
                                if (NUtils.findGob(parent.id) != null)
                                {
                                    NUtils.getUI().core.pfMap.processGob(parent.id);
                                }
                            }
                        }
                    }
                }
            }
    }

    protected long modelAttribute = -1;

    public long getModelAttribute() {
        return modelAttribute;
    }
}
