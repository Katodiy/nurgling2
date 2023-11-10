package nurgling;

import haven.*;
import static haven.MCache.tilesz;
import nurgling.areas.*;
import nurgling.tools.*;
import org.json.*;

import java.awt.image.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

public class NMapView extends MapView
{
    public NMapView(Coord sz, Glob glob, Coord2d cc, long plgob)
    {
        super(sz, glob, cc, plgob);
        olsinf.put("minesup", new NOverlayGobInfo(Resource.remote().loadwait("map/overlay/minesup-o").flayer(MCache.ResOverlay.class),false));
        olsinf.put("hareas", new NOverlayInfo(Resource.remote().loadwait("map/overlay/areash-o").flayer(MCache.ResOverlay.class),false));
        for(int i = 0 ; i < MCache.customolssize; i++)
            olsinf.put("areas"+String.valueOf(i), new NOverlayInfo(Resource.remote().loadwait("map/overlay/areas-o"+String.valueOf(i)).flayer(MCache.ResOverlay.class),false));
//        toggleol("areas", true);
        toggleol("hareas", true);
        toggleol("minesup", true);

        if(new File(NConfig.current.path_areas).exists())
        {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_areas), StandardCharsets.UTF_8))
            {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }
            catch (IOException ignore)
            {
            }

            if (!contentBuilder.toString().isEmpty())
            {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = (JSONArray) main.get("areas");
                for (int i = 0; i < array.length(); i++)
                {
                    NArea a;
                    areas.add(a = new NArea((JSONObject) array.get(i)));
                    NUtils.getGameUI().areas.addArea(a.name);
                }
            }
        }
    }

    final HashMap<String, String> ttip = new HashMap<>();

    private final Map<MCache.OverlayInfo, Overlay> custom_ols = new HashMap<>();
    public HashMap<String, NOverlayInfo> olsinf = new HashMap<>();

    public AtomicBoolean isAreaSelectionMode = new AtomicBoolean(false);
    public NArea.Space areaSpace = null;
    public Object tooltip(Coord c, Widget prev) {
        if (!ttip.isEmpty() && NUtils.getGameUI().ui.core.isInspectMode()) {

            Collection<BufferedImage> imgs = new LinkedList<BufferedImage>();
            if (ttip.get("gob") != null) {
                BufferedImage gob = RichText.render(String.format("$col[128,128,255]{%s}:", "Gob"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("gob"), 0).img);
            }
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
                if (inf != null) {
                    Gob gob = Gob.from(inf.ci);
                    if (gob != null) {
                        ttip.put("gob", gob.ngob.name);
                        if(gob.ngob.hitBox!=null)
                            ttip.put("HitBox", gob.ngob.hitBox.toString());
                        ttip.put("rc" , gob.rc.toString());
                        if(!gob.ols.isEmpty()) {
                            StringBuilder ols = new StringBuilder();
                            boolean isPrinted = false;
                            for (Gob.Overlay ol : gob.ols) {
                                if (ol.spr != null) {
                                    isPrinted = true;
                                    String res = ol.spr.getClass().toString();
                                    if(!res.contains("$"))
                                        ols.append(res + " ");
                                }
                            }
                            if(isPrinted)
                                ttip.put("ols", ols.toString());
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

    final LinkedList<NArea> areas = new LinkedList<>();

    public String addArea(NArea.Space result)
    {
        String key;
        synchronized (areas)
        {
            HashSet<String> names = new HashSet<String>();
            for(NArea area : areas)
            {
                names.add(area.name);
            }
            key = ("New Area" + String.valueOf(areas.size()));
            while(names.contains(key))
            {
                key = key+"(1)";
            }
            NArea newArea = new NArea(key);
            newArea.space = result;
            areas.add(newArea);
            NUtils.getGameUI().areas.addArea(newArea.name);
        }
        return key;
    }

    public class NOverlayInfo
    {
        public MCache.OverlayInfo id;
        boolean needUpdate = false;

        public NOverlayInfo(MCache.OverlayInfo flayer, boolean b) {
            this.id = flayer;
            this.needUpdate = b;
        }
    }

    public class NOverlayGobInfo extends NOverlayInfo
    {
        public NOverlayGobInfo(MCache.OverlayInfo flayer, boolean b) {
            super(flayer,b);
        }
        HashMap<Long, ArrayList<NMiningSafeMap.History>> gobs = new HashMap<>();
    }


    @Override
    public void tick(double dt)
    {
        synchronized (areas)
        {
            for (NArea area : areas)
            {
                area.tick(dt);
            }
        }
        super.tick(dt);
    }

    @Override
    protected void oltick()
    {
        if (terrain.area != null)
        {
            for (NOverlayInfo olinf : olsinf.values())
            {
                if ((olinf.needUpdate && (!(olinf instanceof NOverlayGobInfo) || !((NOverlayGobInfo)olinf).gobs.isEmpty())) && custom_ols.get(olinf.id) != null)
                {
                    synchronized (NUtils.getGameUI().map.glob.map.grids)
                    {
                        for (MCache.Grid grid : NUtils.getGameUI().map.glob.map.grids.values())
                        {
                            for (int i = 0; i < grid.cuts.length; i++)
                            {

                                try
                                {
                                    MCache.Grid.Deferred<MapMesh> mesh = grid.cuts[i].mesh;
                                    if (mesh == null)
                                        return;
                                    grid.cuts[i].ols.put(olinf.id, mesh.get().makeol(olinf.id));
                                    grid.cuts[i].olols.put(olinf.id, mesh.get().makeolol(olinf.id));
                                }
                                catch (Loading l)
                                {
                                    l.boostprio(2);
                                }
                            }
                        }
                        olinf.needUpdate = false;
                    }
                }
                Overlay ol = custom_ols.get(olinf.id);
                if (ol == null)
                {
                    try
                    {
                        basic.add(ol = new Overlay(olinf.id));
                        custom_ols.put(olinf.id, ol);
                    }
                    catch (Loading l)
                    {
                        l.boostprio(2);
                    }
                }
            }
        }
        super.oltick();
        if (terrain.area != null)
            for (NOverlayInfo colinf : olsinf.values())
                if(colinf instanceof NMapView.NOverlayGobInfo)
                {
                    NMapView.NOverlayGobInfo olinf = (NMapView.NOverlayGobInfo)colinf;
                    for (Iterator<Map.Entry<Long, ArrayList<NMiningSafeMap.History>>> iter = olinf.gobs.entrySet().iterator(); iter.hasNext(); )
                    {
                        Map.Entry<Long, ArrayList<NMiningSafeMap.History>> item = iter.next();
                        Long gobid = item.getKey();
                        if (NUtils.getGameUI().map.glob.oc.getgob(gobid) == null && placing == null)
                        {
                            for (NMiningSafeMap.History h : olinf.gobs.get(gobid))
                            {
                                for (int i = 0; i < h.g.ols.length; i++)
                                {
                                    if (h.g.ols[i].get().layer(MCache.ResOverlay.class) == olinf.id)
                                    {
                                        h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] = false;
                                    }
                                }
                            }
                            iter.remove();
                            olinf.needUpdate = true;
                        }
                    }
                }
    }

    public void setStatus(MCache.OverlayInfo id, boolean status){
        for(NOverlayInfo inf: olsinf.values()){
            if(inf.id == id){
                inf.needUpdate = status;
                return;
            }
        }
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
        for(NArea area : areas)
        {
            areasNames.add(area.name);
        }
        return areasNames;
    }

    public void selectArea(String name)
    {
        for(NArea area : areas)
        {
            if(area.isHighlighted)
            {
                area.isHighlighted = false;
                area.wasHighlighted = true;
            }
        }

        for(NArea area : areas)
        {
            if(area.name.equals(name))
            {
                area.isHighlighted = true;
            }
        }
    }

    public NArea findArea(String name)
    {
        for(NArea area : areas)
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
        for(NArea area : areas)
        {
            if(area.name.equals(name))
            {
                area.inWork = true;
                area.clearOverlayArea();
                areas.remove(area);
                NUtils.getGameUI().areas.removeArea(area.name);
                break;
            }
        }
    }
    public void changeArea(String name)
    {
        for(NArea area : areas)
        {
            if(area.name.equals(name))
            {
                area.inWork = true;
                area.clearOverlayArea();
                NAreaSelector.changeArea(area);
                break;
            }
        }
    }

    public void changeAreaName(String old, String new_name)
    {
        for(NArea area : areas)
        {
            if(area.name.equals(old))
            {
                area.name = new_name;
                NConfig.needAreasUpdate();
                NUtils.getGameUI().areas.adrop.change(new_name);
                NUtils.getGameUI().areas.removeArea(old);
                NUtils.getGameUI().areas.addArea(new_name);
                break;
            }
        }
    }
}
