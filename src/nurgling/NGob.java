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
    private CellsArray ca = null;
    private boolean isDynamic = false;
    private boolean isGate = false;
    private boolean isPlayer = false;
    protected long modelAttribute = -1;
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

                            setDynamic();

                            NHitBox custom = NHitBox.findCustom(name);
                            if (custom != null)
                            {
                                hitBox = custom;
                            }
                        }
                        if (hitBox != null)
                        {
                            parent.addcustomol(new NModelBox(parent));
                            if(!isDynamic)
                                ca = new CellsArray(parent);
                        }
                    }
                }
            }
    }

    private void setDynamic()
    {
        isDynamic = (NParser.checkName(name, new NAlias("kritter", "borka", "vehicle")));
        isGate = (NParser.checkName(name, new NAlias("gate")));
    }

    public long getModelAttribute() {
        return modelAttribute;
    }
    public CellsArray getCA()
    {
        if(isDynamic)
        {
            if(NUtils.getGameUI().map!=null)
            {
                if(NUtils.getGameUI().map.player()!=null && parent.id == NUtils.getGameUI().map.player().id)
                    return null;
            }
            ca = new CellsArray(parent);
        } else if (isGate)
        {
            if(modelAttribute == 2)
                return ca;
            else
                return null;
        }
        return ca;
    }
}
