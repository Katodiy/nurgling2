package nurgling;

import haven.*;
import static haven.MCache.tilesz;

import haven.render.*;
import nurgling.actions.ActionWithFinal;
import nurgling.actions.QuickActionBot;
import nurgling.areas.*;
import nurgling.overlays.NTexLabel;
import nurgling.overlays.map.*;
import nurgling.tools.*;

import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class NMapView extends MapView
{
    public static final KeyBinding kb_quickaction = KeyBinding.get("quickaction", KeyMatch.forchar('q',0));
    public static final KeyBinding kb_quickignaction = KeyBinding.get("quickignaction", KeyMatch.forchar('q',1));
    public static final int MINING_OVERLAY = - 1;
    public Coord lastGC = null;
    public NMapView(Coord sz, Glob glob, Coord2d cc, long plgob)
    {
        super(sz, glob, cc, plgob);
        for(int i = 0 ; i < MCache.customolssize; i++)
        toggleol("hareas", true);
        toggleol("minesup", true);
    }

    final HashMap<String, String> ttip = new HashMap<>();
    final ArrayList<String> tlays = new ArrayList<>();

    public AtomicBoolean isAreaSelectionMode = new AtomicBoolean(false);
    public NArea.Space areaSpace = null;

    public HashMap<Long, Gob> dummys = new HashMap<>();

    @Override
    public void draw(GOut g) {
        super.draw(g);
        for(Gob dummy : dummys.values())
        {
            dummy.gtick(g.out);
        }
    }



    long gid = Long.MIN_VALUE+1;

    public void initDummys()
    {
        gid = Long.MIN_VALUE+1;
        for(Integer id : glob.map.areas.keySet())
        {
            createAreaLabel(id);
        }
    }

    public void createAreaLabel(Integer id) {
        NArea area = glob.map.areas.get(id);
        Pair<Coord2d,Coord2d> space = area.getRCArea();

        if(space!=null)
        {
            Coord2d pos = (space.a.add(space.b)).div(2);

            Gob dummy = new Gob(glob,pos, gid);
            dummy.virtual = true;
            area.gid = gid;
            NTexLabel notl = new NTexLabel(dummy);
            notl.label = new TexI(NStyle.openings.render(area.name).img);
            dummy.addcustomol(notl);
            dummys.put(gid++, dummy);
            glob.oc.add(dummy);
        }
    }

    public void destroyDummys()
    {
        for(Gob d: dummys.values())
        {
            if(glob.oc.getgob(d.id)!=null)
                glob.oc.remove(d);
        }
        dummys.clear();
    }

    public static NMiningOverlay getMiningOl()
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            synchronized (NUtils.getGameUI().map)
            {
                NMiningOverlay mo = (NMiningOverlay) NUtils.getGameUI().map.nols.get(MINING_OVERLAY);
                if (mo == null)
                {
                    NUtils.getGameUI().map.addCustomOverlay(MINING_OVERLAY, new NMiningOverlay());
                }
                mo = (NMiningOverlay) NUtils.getGameUI().map.nols.get(MINING_OVERLAY);
                return mo;
            }
        }
        return null;
    }

    public static boolean isCustom(Integer id)
    {
        if(id == MINING_OVERLAY)
        {
            return NUtils.getGameUI().map.nols.get(MINING_OVERLAY)!=null;
        }
        return false;
    }

    public Object tooltip(Coord c, Widget prev) {
        if (NUtils.getGameUI()!=null && !ttip.isEmpty() && NUtils.getGameUI().ui.core.isInspectMode()) {

            Collection<BufferedImage> imgs = new LinkedList<BufferedImage>();
            if (ttip.get("gob") != null) {
                BufferedImage gob = RichText.render(String.format("$col[128,128,255]{%s}:", "Gob"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("gob"), 0).img);
            }
                BufferedImage mc = RichText.render(String.format("$col[128,128,255]{%s}:", "MouseCoord"), 0).img;
                imgs.add(mc);
                imgs.add(RichText.render(getLCoord().toString(), 0).img);
            if (ttip.get("rc") != null) {
                BufferedImage gob = RichText.render(String.format("$col[128,128,128]{%s}:", "Coord"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("rc"), 0).img);
            }
            if (ttip.get("id") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,255]{%s}:", "id"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("id"), 0).img);
            }
            if (ttip.get("tile") != null) {
                BufferedImage tile = RichText.render(String.format("$col[128,128,255]{%s}:", "Tile"), 0).img;
                imgs.add(tile);
                imgs.add(RichText.render(ttip.get("tile"), 0).img);
            }
            if (ttip.get("tags") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,128]{%s}:", "Tags"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("tags"), 0).img);
            }
            if (ttip.get("status") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,128]{%s}:", "Status"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("status"), 0).img);
            }
            if (ttip.get("HitBox") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,255]{%s}:", "HitBox"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("HitBox"), 0).img);
            }
            if (ttip.get("dist") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,105]{%s}:", "dist"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("dist"), 0).img);
            }
            if (ttip.get("isDynamic") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,83,83]{%s}:", "isDynamic"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("isDynamic"), 0).img);
            }
            if (ttip.get("marker") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,83,83]{%s}:", "Marker"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("marker"), 0).img);
            }
            if (ttip.get("cont") != null) {
                BufferedImage gob = RichText.render(String.format("$col[83,255,83]{%s}:", "Container"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("cont"), 0).img);
            }
            if (ttip.get("ols") != null) {
                BufferedImage gob = RichText.render(String.format("$col[83,255,155]{%s}:", "Overlays"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("ols"), 0).img);
            }
            if (ttip.get("pose") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,145,200]{%s}:", "Pose"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("pose"), 0).img);
            }
            if (ttip.get("attr") != null) {
                BufferedImage gob = RichText.render(String.format("$col[155,255,83]{%s}:", "Attr"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("attr"), 0).img);
            }
            if (!tlays.isEmpty() && false) {
                BufferedImage gob = RichText.render(String.format("$col[155,32,176]{%s}:", "Layers"), 0).img;
                imgs.add(gob);
                for(String s: tlays)
                {
                    imgs.add(RichText.render(s, 0).img);
                }
            }
            if (ttip.get("poses") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,128]{%s}:", "Poses"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("poses"), 0).img);
            }
            return new TexI((ItemInfo.catimgs(0, imgs.toArray(new BufferedImage[0]))));
        }
        return (super.tooltip(c, prev));
    }

    public static Collection<String> camlist(){
        return camtypes.keySet();
    }
    static {camtypes.put("northo", NOrthoCam.class);}
    
    public static String defcam(){
        return Utils.getpref("defcam", "ortho");
    }
    public static void defcam(String name) {
        Utils.setpref("defcam", name);
    }

    void inspect(Coord c) {
        new Hittest(c) {
            @Override
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                ttip.clear();
                tlays.clear();
                if (inf != null) {
                    Gob gob = Gob.from(inf.ci);
                    if (gob != null) {
                        ttip.put("gob", gob.ngob.name);
                        if(gob.ngob.hitBox!=null) {
                            ttip.put("HitBox", gob.ngob.hitBox.toString());
                            ttip.put("isDynamic", String.valueOf(gob.ngob.isDynamic));
                        }
                        ttip.put("dist", String.valueOf(gob.rc.dist(NUtils.player().rc)));
                        ttip.put("Seg", String.valueOf(gob.ngob.seq));
                        ttip.put("rc" , gob.rc.toString());
                        if(!gob.ols.isEmpty()) {
                            StringBuilder ols = new StringBuilder();
                            boolean isPrinted = false;
                            for (Gob.Overlay ol : gob.ols) {
//                                if (ol.spr != null) {
                                    isPrinted = true;
                                    String res = ol.spr.getClass().toString();
                                    if(!res.contains("$"))
                                        ols.append(res + " ");
//                                }
                            }
                            if(isPrinted)
                                ttip.put("ols", ols.toString());
                        }
                        if(!gob.attr.isEmpty()) {
                            StringBuilder attrs = new StringBuilder();
                            boolean isPrinted = false;
                            for (GAttrib attr : gob.attr.values()) {

                                if (attr instanceof Drawable) {
                                    if (((Drawable) attr).getres() != null) {
                                        Drawable drawable = ((Drawable) attr);
                                        if(drawable instanceof Composite)
                                        {
                                            ttip.put("pose", ((Composite) drawable).current_pose);
                                        }
                                        if (((Drawable) attr).getres().getLayers() != null) {
                                            isPrinted = true;
                                            for (Resource.Layer lay : ((Drawable) attr).getres().getLayers()) {
                                                String res = lay.getClass().toString();
                                                tlays.add(res.replace("$","_") + " ");
                                            }
                                        }
                                    }
                                }

//                                if (ol.spr != null) {
                                isPrinted = true;
                                String res = attr.getClass().toString();
                                if(!res.contains("$"))
                                    attrs.append(res + " ");
//                                }
                            }
                            if(isPrinted)
                                ttip.put("attr", attrs.toString());
                        }

                        ttip.put("id", String.valueOf(gob.id));

                        if (gob.ngob.getModelAttribute()!=-1) {
                            ttip.put("marker", String.valueOf(gob.ngob.getModelAttribute()));
                        }

//                        if(gob.getattr(Drawable.class)!=null && gob.getattr(Drawable.class) instanceof Composite && ((Composite)gob.getattr(Drawable.class)).oldposes!=null)
//                        {
//                            StringBuilder poses = new StringBuilder();
//                            Iterator<ResData> pose = ((Composite)gob.getattr(Drawable.class)).oldposes.iterator();
//                            while (pose.hasNext()) {
//                                poses.append(pose.next().res.get().name);
//                                if (pose.hasNext())
//                                    poses.append(", ");
//                            }
//                            ttip.put("poses", poses.toString());
//                        }

                    }
                }
                MCache mCache = ui.sess.glob.map;
                int tile = mCache.gettile(mc.div(tilesz).floor());
                Resource res = mCache.tilesetr(tile);
                if (res != null) {
                    ttip.put("tile", res.name);
                }
            }

            @Override
            protected void nohit(Coord pc) {
                ttip.clear();
            }
        }.run();
    }


    public String addArea(NArea.Space result)
    {
        String key;
        synchronized (glob.map.areas)
        {
            HashSet<String> names = new HashSet<String>();
            int id = 1;
            for(NArea area : glob.map.areas.values())
            {
                if(area.id >= id)
                {
                    id = area.id + 1;
                }
                names.add(area.name);
            }
            key = ("New Area" + String.valueOf(glob.map.areas.size()));
            while(names.contains(key))
            {
                key = key+"(1)";
            }
            NArea newArea = new NArea(key);
            newArea.id = id;
            newArea.space = result;
            newArea.grids_id.addAll(newArea.space.space.keySet());
            glob.map.areas.put(id, newArea);
            NUtils.getGameUI().areas.addArea(id, newArea.name, newArea);
            createAreaLabel(id);
        }
        return key;
    }


    @Override
    public void tick(double dt)
    {
        synchronized (glob.map.areas)
        {
            for (NArea area : glob.map.areas.values())
            {
                area.tick(dt);
            }
        }
        ArrayList<Long> forRemove = new ArrayList<>();
//        for(Gob dummy : dummys.values())
//        {
//            if(NUtils.findGob(dummy.id)==null)
//            {
//                forRemove.add(dummy.id);
//                for (NArea area : glob.map.areas.values())
//                {
//                    if(area.gid == dummy.id)
//                        createAreaLabel(area.id);
//                }
//
//            }
//        }
//        for(Long id : forRemove)
//            dummys.remove(id);
        super.tick(dt);
    }

    @Override
    protected void oltick()
    {
        super.oltick();
        for(NOverlay ol : nols.values())
            ol.tick();
    }

    public void toggleol(String tag, boolean a)
    {
        if (a)
            enol(tag);
        else
            disol(tag);
    }

    @Override
    public boolean mousedown(Coord c, int button)
    {
        if ( isAreaSelectionMode.get() )
        {
            if (selection == null)
            {
                selection = new NSelector();
            }
        }
        return super.mousedown(c, button);
    }

    private Coord lastCoord = null;
    private Coord2d lastCoord2d = new Coord2d();
    @Override
    public void mousemove(Coord c) {
        lastCoord = c;
        super.mousemove(c);
    }

    public Coord2d getLCoord() {
        new Maptest(lastCoord){
            public void hit(Coord pc, Coord2d mc) {
                lastCoord2d.x = mc.x;
                lastCoord2d.y = mc.y;
            }
        }.run();
        return lastCoord2d;
    }

    public boolean shiftPressed = false;

    @Override
    public boolean keyup(KeyEvent ev) {
        if(ev.getKeyCode() == 16)
            shiftPressed = false;
        return super.keyup(ev);
    }

    @Override
    public boolean keydown(KeyEvent ev) {
        if(ev.getKeyCode() == 16)
            shiftPressed = true;
        if(kb_quickaction.key().match(ev) || kb_quickignaction.key().match(ev)) {
            Thread t;
            (t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if(kb_quickaction.key().match(ev))
                            new QuickActionBot(false).run(NUtils.getGameUI());
                        else
                            new QuickActionBot(true).run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().msg("quick action error" + ":" + "STOPPED");
                    }
                }
            }, "quick action")).start();


        }
        return super.keydown(ev);
    }

    public class NSelector extends Selector
    {
        public boolean mmouseup(Coord mc, int button)
        {
            synchronized (NMapView.this)
            {
                if (sc != null)
                {
                    Coord ec = mc.div(MCache.tilesz2);
                    xl.mv = false;
                    tt = null;
                    areaSpace = new NArea.Space(sc,ec);
                    ol.destroy();
                    mgrab.remove();
                    sc = null;
                    destroy();
                    selection = null;
                    isAreaSelectionMode.set(false);
                }
                return (true);
            }
        }
    }

    public Collection<String> areas(){
        LinkedList<String> areasNames = new LinkedList<>();
        for(NArea area : glob.map.areas.values())
        {
            areasNames.add(area.name);
        }
        return areasNames;
    }

    public NArea findArea(String name)
    {
        for(NArea area : glob.map.areas.values())
        {
            if(area.name.equals(name))
            {
                return area;
            }
        }
        return null;
    }

    public void removeArea(String name)
    {
        for(NArea area : glob.map.areas.values())
        {
            if(area.name.equals(name))
            {
                area.inWork = true;
//                area.clearOverlayArea();
                glob.map.areas.remove(area.id);
                Gob dummy = dummys.get(area.gid);
                if(dummy != null) {
                    glob.oc.remove(dummy);
                    dummys.remove(area.gid);
                }
                NUtils.getGameUI().areas.removeArea(area.id);

                break;
            }
        }
    }
    public void changeArea(String name)
    {
        for(NArea area : glob.map.areas.values())
        {
            if(area.name.equals(name))
            {
                area.inWork = true;
                if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
                {
                    NOverlay nol = NUtils.getGameUI().map.nols.get(area.id);
                    if (nol != null)
                        nol.remove();
                    Gob dummy = dummys.get(area.gid);
                    if(dummy != null) {
                        glob.oc.remove(dummy);
                        dummys.remove(area.gid);
                    }
                    NUtils.getGameUI().map.nols.remove(area.id);
                }
                NAreaSelector.changeArea(area);
                break;
            }
        }
    }

    public void changeAreaName(Integer id, String new_name)
    {
        glob.map.areas.get(id).name = new_name;
        NConfig.needAreasUpdate();
    }
}
