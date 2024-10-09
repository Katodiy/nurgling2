package nurgling;

import haven.*;
import haven.render.sl.InstancedUniform;
import haven.res.gfx.fx.eq.Equed;
import haven.res.lib.vmat.Mapping;
import haven.res.lib.vmat.Materials;
import nurgling.nattrib.*;
import nurgling.overlays.*;
import nurgling.pf.*;
import nurgling.tools.*;
import nurgling.widgets.NQuestInfo;

import java.util.*;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class NGob {
    public NHitBox hitBox = null;
    public String name = null;
    public boolean isQuested = true;
    public boolean customMask = false;
    public int mask = -1;
    private CellsArray ca = null;
    boolean isDynamic = false;
    private boolean isGate = false;
    protected long modelAttribute = -1;
    final Gob parent;
    public long seq;
    public int lastUpdate = 0;
    public Map<Class<? extends NAttrib>, NAttrib> nattr = new HashMap<Class<? extends NAttrib>, NAttrib>();

    public NGob(Gob parent) {
        this.parent = parent;
    }

    public void checkattr(GAttrib a, long id) {

        if (a instanceof ResDrawable) {
            modelAttribute = ((ResDrawable) a).calcMarker();
        }
        if (a instanceof Following) {
            isDynamic = true;
        }

        if (a instanceof GobIcon)
        {
            GobIcon gi = (GobIcon) a;
//            String name = gi.icon().name();
        }

        if (a instanceof Drawable) {
            if (((Drawable) a).getres() != null) {
                name = ((Drawable) a).getres().name;
                if (((Drawable) a).getres().getLayers() != null) {
                    for (Resource.Layer lay : ((Drawable) a).getres().getLayers()) {
                        if (lay instanceof Resource.Neg) {
                            hitBox = new NHitBox(((Resource.Neg) lay).ac, ((Resource.Neg) lay).bc);
                        }
                        else if(lay instanceof Resource.Obstacle)
                        {
                            hitBox = NHitBox.fromObstacle(((Resource.Obstacle) lay).p);
                        }
                    }
                    if (name != null) {
                        if(NStyle.iconMap.containsKey(name))
                        {
                            //TODO трюфель
                            parent.setattr(new GobIcon(parent,NStyle.iconMap.get(name),new byte[0]));
                        }

                        if (NParser.checkName(name, new NAlias("plants"))) {
                            parent.addcustomol(new NCropMarker(parent));
                        }
                        else {
                            if (NParser.checkName(name, new NAlias(new ArrayList<String>(Arrays.asList("minebeam", "column", "towercap", "ladder", "minesupport")), new ArrayList<String>(Arrays.asList("stump", "wrack", "log"))))) {
                                switch (name) {
                                    case "gfx/terobjs/ladder":
                                    case "gfx/terobjs/minesupport":
                                    case "gfx/terobjs/trees/towercap":
                                    case "gfx/terobjs/map/naturalminesupport":
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
                            if (name.contains("gfx/terobjs/dframe") || name.contains("gfx/terobjs/cheeserack")) {
                                customMask = true;
                            } else if (name.contains("gfx/terobjs/barrel")) {
                                customMask = true;
                                parent.addcustomol(new NBarrelOverlay(parent));
                            }

                            if (NUtils.playerID() != -1 && name.equals("gfx/borka/body") && NUtils.playerID() != parent.id) {
                                parent.addcustomol(new NKinRing(parent));
                                parent.setattr(new NKinTex(parent));
                            }

                            NHitBox custom = NHitBox.findCustom(name);
                            if (custom != null) {
                                hitBox = custom;
                            }
                        }
                    }
                    if (hitBox != null) {
                        if (NParser.checkName(name, new NAlias("gfx/terobjs/moundbed"))) {
                            hitBox = null;
                        }
                        else {
                            if (ca == null) {
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
    }


    private void setDynamic()
    {
        isDynamic = (NParser.checkName(name, new NAlias("kritter", "borka", "vehicle")));
        isGate = (NParser.checkName(name, new NAlias("gate")));
    }

    public long getModelAttribute() {
        return modelAttribute;
    }

    public CellsArray getCA() {
        if (isDynamic) {
            if (NUtils.getGameUI().map != null) {
                if (NUtils.getGameUI().map.player() != null && parent.id == NUtils.getGameUI().map.player().id)
                    return null;
                return new CellsArray(parent);
            }
        } else if (isGate) {
            if (modelAttribute != 2)
                return null;
        } else {
            if(ca==null && hitBox!=null)
            {
                ca = new CellsArray(parent);
            }
        }
        return ca;
    }

    public void markAsDynamic() {
        isDynamic = true;
    }

    public void tick(double dt) {
        for (NAttrib attrib : nattr.values()) {
            attrib.tick(dt);
        }
        int nlu = NQuestInfo.lastUpdate.get();
        if (NQuestInfo.lastUpdate.get() > lastUpdate)
        {
            NQuestInfo.MarkerInfo markerInfo;
            if((markerInfo = NQuestInfo.getMarkerInfo(parent))!=null)
            {
                parent.addcustomol(new NQuestGiver(parent, markerInfo));
            }
            if(NQuestInfo.isForageTarget(name))
            {
                parent.addcustomol(new NQuestTarget(parent,false));
            }
            else if(NQuestInfo.isHuntingTarget(name))
            {
                parent.addcustomol(new NQuestTarget(parent,true));
            }
            lastUpdate = nlu;
        }
    }

    public static Gob getDummy(Coord2d rc, double a, String resName) {
        Gob res = new Gob(null, rc, -1);
        if (resName != null)
            res.ngob.hitBox = NHitBox.findCustom(resName);
        res.a = a;
        return res;
    }

    public static Gob getDummy(Coord2d rc, double a, NHitBox hb) {
        Gob res = getDummy(rc, a, (String) null);
        res.ngob.hitBox = hb;
        res.ngob.isDynamic = true;
        return res;
    }

    public Materials mats(Mapping mapping) {
        Material mat = null;
        if(mapping instanceof Materials)
        {
            mat = ((Materials)mapping).mats.get(0);
        }
        if(name!=null)
        {
            MaterialFactory.Status status = MaterialFactory.getStatus(name,customMask? mask ():(int)getModelAttribute());
            if(status == MaterialFactory.Status.NOTDEFINED)
                return null;
            if(!altMats.containsKey(status))
            {
                Map<Integer,Material> mats = MaterialFactory.getMaterials(name, status, mat);
                if(mats!=null)
                    altMats.put(status,new Materials(parent,mats));
            }
            return altMats.get(status);
        }
        return null;
    }

    HashMap<MaterialFactory.Status,Materials> altMats = new HashMap<>();


    public void addol(Gob.Overlay ol) {
        if (name != null)
            if (name.equals("gfx/terobjs/dframe") || name.equals("gfx/terobjs/barrel")) {
                if(ol.spr instanceof StaticSprite) {
                    ResDrawable dr = ((ResDrawable) parent.getattr(Drawable.class));
                    parent.setattr(new ResDrawable(parent, dr.res, dr.sdt, false));
                }
            }
    }

    public void removeol(Gob.Overlay ol) {
        if (name != null)
            if (name.equals("gfx/terobjs/dframe") || name.equals("gfx/terobjs/barrel") ) {
                if(ol.spr instanceof StaticSprite) {
                    ResDrawable dr = ((ResDrawable) parent.getattr(Drawable.class));
                    parent.setattr(new ResDrawable(parent, dr.res, dr.sdt, false));
                }
            }
    }

    public int mask() {
        if(name.equals("gfx/terobjs/dframe")) {
            for (Gob.Overlay ol : parent.ols) {
                if (ol.spr instanceof StaticSprite) {
                    if (!NParser.isIt(ol, new NAlias("-blood", "-fishraw", "-windweed")) || NParser.isIt(ol, new NAlias("-windweed-dry"))) {
                        return 2;
                    } else {
                        return 1;
                    }
                }
            }
            return 0;
        }
        else if (name.equals("gfx/terobjs/barrel"))
        {
            for (Gob.Overlay ol : parent.ols) {
                if (ol.spr instanceof StaticSprite) {
                    return 4;
                }
            }
            return 0;
        }
        else if (name.equals("gfx/terobjs/cheeserack")) {
            int counter = 0;
            for (Gob.Overlay ol : parent.ols) {
                if (ol.spr instanceof Equed) {
                    counter++;
                }
            }
            if (counter == 3)
                return 2;
            else if (counter != 0)
                return 1;
            return 0;
        }
        return -1;
    }
}
