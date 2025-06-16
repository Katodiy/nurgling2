package nurgling;

import haven.*;
import haven.render.Location;
import haven.render.Transform;
import haven.res.gfx.fx.eq.Equed;
import haven.res.gfx.terobjs.consobj.Consobj;
import haven.res.lib.tree.TreeScale;
import haven.res.lib.vmat.Mapping;
import haven.res.lib.vmat.Materials;
import monitoring.NGlobalSearchItems;
import nurgling.gattrr.NCustomScale;
import nurgling.overlays.*;
import nurgling.pf.*;
import nurgling.tools.*;
import nurgling.widgets.NAlarmWdg;
import nurgling.widgets.NProspecting;
import nurgling.widgets.NQuestInfo;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static haven.MCache.cmaps;
import static haven.OCache.posres;

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

    public String hash;
    private final Queue<DelayedOverlayTask> delayedOverlayTasks = new ConcurrentLinkedQueue<>();

    public void changedPose(String currentPose) {
        if (name != null) {
            if (currentPose.contains("fgtidle")) {
                if (name.equals("gfx/kritter/cattle/cattle") || name.equals("gfx/kritter/boar/boar") || name.equals("gfx/kritter/goat/wildgoat") || name.equals("gfx/kritter/reindeer/reindeer") || name.equals("gfx/kritter/sheep/sheep")) {
                    parent.addcustomol(new NTexMarker(parent, new TexI(Resource.loadsimg("nurgling/hud/taiming")), () -> {
                        for (Fightview.Relation rel : NUtils.getGameUI().fv.lsrel) {
                            if (rel.gobid == parent.id) {
                                return true;
                            }
                        }
                        return false;
                    }));
                }
            }
        }
    }

    private static class DelayedOverlayTask {
        final Predicate<Gob> condition;
        final Consumer<Gob> action;

        DelayedOverlayTask(Predicate<Gob> condition, Consumer<Gob> action) {
            this.condition = condition;
            this.action = action;
        }
    }

    public NGob(Gob parent) {
        this.parent = parent;
    }

    public static Gob from(Clickable ci) {
        if (ci instanceof Gob.GobClick) {
            return ((Gob.GobClick) ci).gob;
        } else if (ci instanceof Composited.CompositeClick) {
            Gob.GobClick gi = ((Composited.CompositeClick) ci).gi;
            return gi != null ? gi.gob : null;
        }
        return null;
    }

    protected void updateMovingInfo(GAttrib a, GAttrib prev) {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            if (prev instanceof Moving) {
                NUtils.getGameUI().map.glob.oc.paths.removePath((Moving) prev);
            }
            if (a instanceof LinMove || a instanceof Homing) {
                NUtils.getGameUI().map.glob.oc.paths.addPath((Moving) a);
            }
//            if (NUtils.getGameUI() != null && (me))
//                NUtils.getGameUI().pathQueue().ifPresent(pathQueue -> pathQueue.movementChange((Gob) this, prev, a));
        }
    }

    public void checkattr(GAttrib a, long id, GAttrib prev) {

        if (a instanceof ResDrawable) {
            modelAttribute = ((ResDrawable) a).calcMarker();
        }
        if (a instanceof Following) {
            isDynamic = true;
        }

        if (a instanceof Drawable) {
            if (((Drawable) a).getres() != null) {
                name = ((Drawable) a).getres().name;

                if(name!=null) {
                    if (name.startsWith("gfx/terobjs/arch/cellardoor") || name.startsWith("gfx/terobjs/herbs/standinggrass")) {
                        return;
                    }

                    if (name.contains("bumlings")) {
                        name = name.replaceAll("\\d+$", "");
                    }

                    if (name.contains("bumlings")) {
                        name = name.replaceAll("\\d+$", "");
                    }

                    if(name.contains("palisade"))
                    {
                        if(parent.getattr(NCustomScale.class)==null)
                            parent.setattr(new NCustomScale(parent));
                    }
                }

                if (((Drawable) a).getres().getLayers() != null) {
                    if(a instanceof ResDrawable && ((ResDrawable) a).spr instanceof Consobj)
                        {
                            Consobj consobj = (Consobj) ((ResDrawable) a).spr;
                            if(consobj.built!=null && (((Session.CachedRes.Ref)consobj.built.res).res)!=null) {
                                NHitBox custom = NHitBox.findCustom(((Session.CachedRes.Ref)consobj.built.res).res.name);
                                if (custom != null) {
                                    hitBox = custom;
                                }
                                else {
                                    for (Resource.Layer lay : ((Session.CachedRes.Ref) consobj.built.res).res.getLayers()) {
                                        if (lay instanceof Resource.Neg) {
                                            hitBox = new NHitBox(((Resource.Neg) lay).ac, ((Resource.Neg) lay).bc);
                                        } else if (lay instanceof Resource.Obstacle) {
                                            hitBox = NHitBox.fromObstacle(((Resource.Obstacle) lay).p);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                Coord2d ur = null;
                                Coord2d bl = null;
                                for(Location loc: consobj.poles)
                                {
                                    if(bl == null) {
                                        bl = new Coord2d(((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[12],((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[13]);
                                    }
                                    else
                                    {
                                        bl = new Coord2d(Math.min(bl.x,((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[12]),Math.min(bl.y,((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[13]));
                                    }
                                    if(ur == null) {
                                        ur = new Coord2d(((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[12],((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[13]);
                                    }
                                    else
                                    {
                                        ur = new Coord2d(Math.max(ur.x,((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[12]),Math.max(ur.y,((Matrix4f)(((Transform.ByMatrix)loc.xf).xf)).m[13]));
                                    }
                                }
                                if(bl!=null && ur!=null) {
                                    hitBox = new NHitBox(bl, ur);
                                }
                            }
                        }
                        else {
                            for (Resource.Layer lay : ((Drawable) a).getres().getLayers()) {
                                if (lay instanceof Resource.Neg) {
                                    hitBox = new NHitBox(((Resource.Neg) lay).ac, ((Resource.Neg) lay).bc);
                                } else if (lay instanceof Resource.Obstacle) {
                                    hitBox = NHitBox.fromObstacle(((Resource.Obstacle) lay).p);
                                }
                            }
                        }
                    if (name != null) {
                        if(NStyle.iconMap.containsKey(name))
                        {
                            //TODO трюфель
                            parent.setattr(new GobIcon(parent,NStyle.iconMap.get(name),new byte[0]));
                        }



                        if (NParser.checkName(name, new NAlias("borka"))) {
                            NAlarmWdg.addBorka(parent.id);
                        }

                        if (NParser.checkName(name, new NAlias("plants"))) {
                            parent.addcustomol(new NCropMarker(parent));
                        }
                        else {
                            if (NParser.checkName(name, new NAlias(new ArrayList<String>(Arrays.asList("minebeam", "column", "towercap", "ladder", "minesupport")), new ArrayList<String>(Arrays.asList("stump", "wrack", "log"))))) {
                                switch (name) {
                                    case "gfx/terobjs/map/naturalminesupport":
                                        parent.addcustomol(new NMiningSupport(parent, 92));
                                        break;
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
                            if (name.contains("gfx/terobjs/dframe") || name.contains("gfx/terobjs/cheeserack")) {
                                customMask = true;
                            } else if (name.contains("gfx/terobjs/barrel")) {
                                customMask = true;
                                parent.addcustomol(new NBarrelOverlay(parent));
                            }

                            if (name.equals("gfx/borka/body")) {
                                delayedOverlayTasks.add(new DelayedOverlayTask(
                                        gob -> gob.pose()!=null,
                                        gob -> {
                                            String posename = gob.pose();
                                            if(!(posename.contains("knocked") || posename.contains("dead") || posename.contains("manneq") || posename.contains("skel")) || NUtils.playerID() == gob.id) {
                                                gob.addcustomol(new NKinRing(gob));
                                                gob.setattr(new NKinTex(gob));
                                            }
                                        }
                                ));
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
                    if(parent.getattr(TreeScale.class)!=null)
                    {
                        if(name!=null)
                            parent.addcustomol(new NTreeScaleOl(parent));
                    }
                }

                if (name != null && name.contains("kritter")) {
                    delayedOverlayTasks.add(new DelayedOverlayTask(
                            gob -> {
                                String pose = gob.pose();
                                boolean poseValid = pose != null && !NParser.checkName(pose, "dead", "knock");
                                boolean overlayNotExists = gob.findol(NAreaRad.class) == null;
                                nurgling.conf.NAreaRad rad = nurgling.conf.NAreaRad.get(name);
                                boolean radValid = rad != null && rad.vis;

                                return poseValid && overlayNotExists && radValid;
                            },
                            gob -> {
                                nurgling.conf.NAreaRad rad = nurgling.conf.NAreaRad.get(name);
                                gob.addcustomol(new NAreaRange(gob, rad));
                            }
                    ));
                }
            }
        }
        else if(a instanceof TreeScale)
        {
            if(name!=null)
                parent.addcustomol(new NTreeScaleOl(parent));
        }
        if (a instanceof Moving || prev instanceof Moving) {
            updateMovingInfo(a, prev);
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
                else if(hitBox!=null)
                {
                    return new CellsArray(parent);
                }
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

    public CellsArray getTrueCA() {
        return ca;
    }

    public void markAsDynamic() {
        isDynamic = true;
    }

    public void tick(double dt) {
        if(NUtils.getGameUI()!=null) {
            Iterator<DelayedOverlayTask> it = delayedOverlayTasks.iterator();
            while (it.hasNext()) {
                DelayedOverlayTask task = it.next();
                if (task.condition.test(parent)) {
                    task.action.accept(parent);
                    it.remove();
                }
            }


            if(hash==null)
            {
                Coord pltc = (new Coord2d(parent.rc.x / MCache.tilesz.x, parent.rc.y / MCache.tilesz.y)).floor();
                synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
                    if (NUtils.getGameUI().ui.sess.glob.map.grids.containsKey(pltc.div(cmaps))) {
                        MCache.Grid g = NUtils.getGameUI().ui.sess.glob.map.getgridt(pltc);
                        StringBuilder hashInput = new StringBuilder();
                        Coord coord = (parent.rc.sub(g.ul.mul(Coord2d.of(11, 11)))).floor(posres);
                        hashInput.append(name).append(g.id).append(coord.toString());
                        hash = NUtils.calculateSHA256(hashInput.toString());
                        parent.setattr(new NGlobalSearch(parent));
                    }
                }
            }


//            Gob player = NUtils.player();
//            if(player!=null && parent.id == player.id) {
//                if ((Boolean) NConfig.get(NConfig.Key.player_box)) {//9*9 around player
//                        parent.addcustomol(new NPlayerBoxOverlay(parent));
//                } else {
//                    Gob.Overlay col = parent.findol(NPlayerBoxOverlay.class);
//                    if (col != null) col.remove();
//                }
//
//                if ((Boolean) NConfig.get(NConfig.Key.player_fov)) {//FOV render
//                    parent.addcustomol(new NRenderBoxOverlay(parent));
//                } else {
//                    Gob.Overlay col = parent.findol(NRenderBoxOverlay.class);
//                    if (col != null) col.remove();
//                }
//
//                if ((Boolean) NConfig.get(NConfig.Key.gridbox)) {//grid borders
//                    parent.addcustomol(new NGridBoxOverlay(parent));
//                } else {
//                    Gob.Overlay col = parent.findol(NGridBoxOverlay.class);
//                    if (col != null) col.remove();
//                }
//            }

            int nlu = NQuestInfo.lastUpdate.get();
            if (NQuestInfo.lastUpdate.get() > lastUpdate) {


                NQuestInfo.MarkerInfo markerInfo;
                if ((markerInfo = NQuestInfo.getMarkerInfo(parent)) != null) {
                    parent.addcustomol(new NQuestGiver(parent, markerInfo));
                }
                if ((Boolean) NConfig.get(NConfig.Key.questNotified)) {
                    if (NQuestInfo.isForageTarget(name)) {
                        parent.addcustomol(new NQuestTarget(parent, false));
                    } else if (NQuestInfo.isHuntingTarget(name)) {
                        parent.addcustomol(new NQuestTarget(parent, true));
                    }
                }
                lastUpdate = nlu;
            }
            if ((Boolean) NConfig.get(NConfig.Key.lpassistent)) {
                if (name != null && name.startsWith("gfx/terobjs")) {
                    if (VSpec.object.containsKey(name))
                        if (VSpec.object.get(name).size() != NUtils.getGameUI().getCharInfo().LpExplorerGetSize(name)) {
                            parent.addcustomol(new NLPassistant(parent));
                        }
                }
            }
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
        Sprite spr = ol.spr;
        if(spr != null) {
            Resource res = spr.res;
            if(res != null) {
                if(res.name.equals("gfx/fx/dowse")) {
                    NProspecting.overlay(parent, ol);
                }
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
