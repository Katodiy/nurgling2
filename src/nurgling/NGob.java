package nurgling;

import haven.*;
import nurgling.nattrib.*;
import nurgling.overlays.*;
import nurgling.pf.*;
import nurgling.tools.*;
import static nurgling.tools.VSpec.chest_state;

import java.util.*;

public class NGob
{
    public NHitBox hitBox = null;
    public String name = null;
    public boolean isQuested = true;
    private CellsArray ca = null;
    boolean isDynamic = false;
    private boolean isGate = false;
    protected long modelAttribute = -1;
    final Gob parent;

    public Map<Class<? extends NAttrib>, NAttrib> nattr = new HashMap<Class<? extends NAttrib>, NAttrib>();
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
        if(a instanceof Following)
        {
            isDynamic = true;
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

                        if (NParser.checkName(name, new NAlias(new ArrayList<String>(Arrays.asList("minebeam", "column", "towercap", "ladder", "minesupport")), new ArrayList<String>(Arrays.asList("stump", "wrack", "log"))))) {
                            switch (name)
                            {
                                case "gfx/terobjs/ladder":
                                case "gfx/terobjs/minesupport":
                                case "gfx/terobjs/trees/towercap":
                                    parent.addcustomol(new NMiningSupport(parent, 100));
                                    break;
                                case "gfx/terobjs/minebeam":
                                    parent.addcustomol(new NMiningSupport(parent, 150));
                                    break;
                                case "gfx/terobjs/column":
                                    parent.addcustomol(new NMiningSupport(parent, 125));
                                    break;
                            }
                        }
                        if(name.contains("gfx/terobjs"))
                        {
                            switch (name)
                            {
                                case "gfx/terobjs/chest":
                                    parent.setattr(new NContainerTex(parent, NStyle.chestAlt, chest_state));
                                    break;
                                case "gfx/terobjs/cupboard":
                                    parent.setattr(new NContainerTex(parent, NStyle.cupboardAlt, chest_state));
                                    break;
                                case "gfx/terobjs/dframe":
                                    parent.setattr(new NDframeTex(parent, NStyle.dframeAlt));
                                    break;
                            }
                        }

                        if (name.equals("gfx/borka/body") && NUtils.playerID() != parent.id)
                        {
                            parent.addcustomol(new NKinRing(parent));
                            parent.setattr(new NKinTex(parent));
                        }

                        NHitBox custom = NHitBox.findCustom(name);
                        if (custom != null)
                        {
                            hitBox = custom;
                        }
                    }
                    if (hitBox != null)
                    {
                        if(ca == null) {
                            setDynamic();
                            parent.addcustomol(new NModelBox(parent));
                            if (!isDynamic)
                                ca = new CellsArray(parent);
                        }
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
                if (NUtils.getGameUI().map.player() != null && parent.id == NUtils.getGameUI().map.player().id)
                    return null;
                return new CellsArray(parent);
            }
        }
        else if (isGate)
        {
            if(modelAttribute != 2)
                return null;
        }
        else
        {
            if(ca==null && hitBox!=null)
            {
                ca = new CellsArray(parent);
            }
        }
        return ca;
    }

    public void markAsDynamic()
    {
        isDynamic = true;
    }

    public void tick(double dt)
    {
        for(NAttrib attrib : nattr.values())
        {
            attrib.tick(dt);
        }
    }

    public static Gob getDummy(Coord2d rc, double a, String resName)
    {
        Gob res = new Gob(null, rc, -1);
        if(resName!=null)
            res.ngob.hitBox = NHitBox.findCustom(resName);
        res.a = a;
        return res;
    }

    public static Gob getDummy(Coord2d rc, double a, NHitBox hb)
    {
        Gob res = getDummy(rc, a, (String) null);
        res.ngob.hitBox = hb;
        res.ngob.isDynamic = true;
        return res;
    }


}
