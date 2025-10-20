package nurgling;

import haven.*;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

import haven.Composite;
import haven.res.ui.gobcp.Gobcopy;
import nurgling.actions.Action;
import nurgling.actions.ActionWithFinal;
import nurgling.actions.QuickActionBot;
import nurgling.actions.bots.ScenarioRunner;
import nurgling.areas.*;
import nurgling.overlays.*;
import nurgling.overlays.map.*;
import nurgling.routes.Route;
import nurgling.routes.RouteGraphManager;
import nurgling.routes.RoutePoint;
import nurgling.scenarios.Scenario;
import nurgling.tasks.WaitForMapLoadNoCoord;
import nurgling.tools.*;
import nurgling.widgets.NAreasWidget;
import nurgling.widgets.NMiniMap;

import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

public class NMapView extends MapView
{
    public static final KeyBinding kb_quickaction = KeyBinding.get("quickaction", KeyMatch.forcode(KeyEvent.VK_Q, 0));
    public static final KeyBinding kb_quickignaction = KeyBinding.get("quickignaction", KeyMatch.forcode(KeyEvent.VK_Q, 1));
    public static final KeyBinding kb_mousequickaction = KeyBinding.get("mousequickaction", KeyMatch.forcode(KeyEvent.VK_Q, KeyMatch.M));
    public static final KeyBinding kb_displaypbox = KeyBinding.get("pgridbox",  KeyMatch.nil);
    public static final KeyBinding kb_displayfov = KeyBinding.get("pfovbox",  KeyMatch.nil);
    public static final KeyBinding kb_displaygrid = KeyBinding.get("gridbox",  KeyMatch.nil);
    public static final KeyBinding kb_togglebb = KeyBinding.get("togglebb",  KeyMatch.forcode(KeyEvent.VK_N, KeyMatch.C));
    public static final KeyBinding kb_togglenature = KeyBinding.get("togglenature",  KeyMatch.forcode(KeyEvent.VK_H, KeyMatch.C));
    public static final int MINING_OVERLAY = - 1;
    public NGlobalCoord lastGC = null;

    public final List<NMiniMap.TempMark> tempMarkList = new ArrayList<NMiniMap.TempMark>();
    
    // Route point dragging state
    private RouteLabel draggedRouteLabel = null;
    private boolean isDraggingRoutePoint = false;
    private UI.Grab dragGrab = null;
    
    // Find RouteLabel at screen coordinate
    private RouteLabel getRouteLabeAt(Coord screenCoord) {
        // Check all virtual game objects for RouteLabel overlays
        for(Gob gob : routeDummys.values()) {
            for(Gob.Overlay ol : gob.ols) {
                if(ol.spr instanceof RouteLabel) {
                    RouteLabel routeLabel = (RouteLabel) ol.spr;
                    if(routeLabel.checkDragStart(screenCoord)) {
                        return routeLabel;
                    }
                }
            }
        }
        return null;
    }
    public NMapView(Coord sz, Glob glob, Coord2d cc, long plgob)
    {
        super(sz, glob, cc, plgob);
        for(int i = 0 ; i < MCache.customolssize; i++)
        toggleol("hareas", true);
        toggleol("minesup", true);
        basic.add(glob.oc.paths);
    }

    final HashMap<String, String> ttip = new HashMap<>();
    final ArrayList<String> tlays = new ArrayList<>();
    final HashMap<String, BufferedImage> cachedImages = new HashMap<>();
    long lastTooltipUpdate = 0;
    final long tooltipThrottleTime = 250; // milliseconds for throttling (increased from 100 for better performance)
    TexI oldttip = null;

    // Fast text renderer for tooltip values (much faster than RichText)
    private static final Text.Foundry tooltipFont = new Text.Foundry(Text.sans, 10);
    public AtomicBoolean isAreaSelectionMode = new AtomicBoolean(false);
    public AtomicBoolean isGobSelectionMode = new AtomicBoolean(false);
    public NArea.Space areaSpace = null;
    public Gob selectedGob = null;
    public static boolean isRecordingRoutePoint = false;

    public HashMap<Long, Gob> dummys = new HashMap<>();
    public HashMap<Long, Gob> routeDummys = new HashMap<>();

    public RouteGraphManager routeGraphManager = new RouteGraphManager();

    public static boolean hitNWidgetsInfo(Coord pc) {
        boolean isFound = false;
        for(Long gobid: ((NMapView)NUtils.getGameUI().map).dummys.keySet())
        {
            Gob gob = Finder.findGob(gobid);
            Gob.Overlay ol;
            if(gob!=null && (ol = gob.findol(NAreaLabel.class))!=null)
            {
                NAreaLabel al = (NAreaLabel) ol.spr;
                if(al.isect(pc)) {
                    isFound = true;
                    for (NArea area : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                        if(area.gid == gobid)
                        {
                            NUtils.getGameUI().areas.showPath(area.path);

                            for(NAreasWidget.AreaItem ai: NUtils.getGameUI().areas.al.items())
                            {
                                if(ai.area!=null && ai.area.gid == gobid) {
                                    NUtils.getGameUI().areas.al.sel = ai;
                                    NUtils.getGameUI().areas.select(area.id);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        for(Long gobid: ((NMapView)NUtils.getGameUI().map).routeDummys.keySet())
        {
            Gob gob = Finder.findGob(gobid);
            Gob.Overlay ol;
            if(gob!=null && (ol = gob.findol(RouteLabel.class))!=null)
            {
                RouteLabel al = (RouteLabel) ol.spr;
                if(al.isect(pc)) {
                    isFound = true;
                    NUtils.getGameUI().msg(String.valueOf(al.point.id));
                    break;
                }
            }
        }

        return isFound;
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        synchronized (dummys) {
            for (Gob dummy : dummys.values()) {
                dummy.gtick(g.out);
            }
        }
    }




    public void initDummys()
    {
        for(Integer id : glob.map.areas.keySet())
        {
            createAreaLabel(id);
        }
    }

    public void initRouteDummys(int id) {
        destroyRouteDummys();
        createRouteLabel(id);
    }

    public void createAreaLabel(Integer id) {
        NArea area = glob.map.areas.get(id);
        Pair<Coord2d,Coord2d> space = area.getRCArea();

        if(space!=null)
        {
            Coord2d pos = (space.a.add(space.b)).div(2);

            OCache.Virtual dummy = glob.oc.new Virtual(pos, 0);
            dummy.virtual = true;
            area.gid = dummy.id;
            dummy.addcustomol(new NAreaLabel(dummy, area));
            dummys.put(dummy.id, dummy);
            glob.oc.add(dummy);
        }
    }

    public void createRouteLabel(Integer id) {
        Route route = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().get(id);
        if (route == null) return;
        synchronized (route.waypoints) {
            NUtils.getGameUI().routesWidget.updateWaypoints();
            List<RoutePoint> waypointsCopy = new ArrayList<>(route.waypoints);
            for (RoutePoint point : waypointsCopy) {
                Coord2d absCoord = point.toCoord2d(glob.map);
                if (absCoord != null) {
                    OCache.Virtual dummy = glob.oc.new Virtual(absCoord, 0);
                    dummy.virtual = true;
                    dummy.addcustomol(new RouteLabel(dummy, route, point));
                    routeDummys.put(dummy.id, dummy);
                    glob.oc.add(dummy);
                }
            }
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

    public void destroyRouteDummys()
    {
        for(Gob d: routeDummys.values())
        {
            if(glob.oc.getgob(d.id)!=null)
                glob.oc.remove(d);
        }
        routeDummys.clear();
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
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTooltipUpdate < tooltipThrottleTime) {
            if(oldttip!=null)
                return oldttip;
            else
                return (super.tooltip(c, prev));
        }
        lastTooltipUpdate = currentTime;

        if (NUtils.getGameUI()!=null && !ttip.isEmpty() && NUtils.getGameUI().ui.core.isInspectMode()) {

            Collection<BufferedImage> imgs = new LinkedList<>();

            for (String key : ttip.keySet()) {
                String text = String.format("$col[128,128,255]{%s}:", key);
                BufferedImage img = cachedImages.get(text);
                if (img == null) {
                    img = RichText.render(text, 0).img;
                    cachedImages.put(text, img);
                }

                imgs.add(img);
                imgs.add(tooltipFont.render(ttip.get(key)).img);
            }
            if (ttip.get("gob") != null) {
                String labelText = "$col[128,128,255]{Gob}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("gob")).img);
            }
            {
                String labelText = "$col[128,128,255]{MouseCoord}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(getLCoord().toString()).img);
            }
            if (ttip.get("rc") != null) {
                String labelText = "$col[128,128,128]{Coord}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("rc")).img);
            }
            if (ttip.get("id") != null) {
                String labelText = "$col[255,128,255]{id}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("id")).img);
            }
            if (ttip.get("tile") != null) {
                String labelText = "$col[128,128,255]{Tile}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("tile")).img);
            }
            if (ttip.get("tags") != null) {
                String labelText = "$col[255,128,128]{Tags}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("tags")).img);
            }
            if (ttip.get("status") != null) {
                String labelText = "$col[255,128,128]{Status}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("status")).img);
            }
            if (ttip.get("HitBox") != null) {
                String labelText = "$col[255,128,255]{HitBox}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("HitBox")).img);
            }
            if (ttip.get("dist") != null) {
                String labelText = "$col[255,128,105]{dist}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("dist")).img);
            }
            if (ttip.get("isDynamic") != null) {
                String labelText = "$col[255,83,83]{isDynamic}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("isDynamic")).img);
            }
            if (ttip.get("marker") != null) {
                String labelText = "$col[255,83,83]{Marker}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("marker")).img);
            }
            if (ttip.get("cont") != null) {
                String labelText = "$col[83,255,83]{Container}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("cont")).img);
            }
            if (ttip.get("ols") != null) {
                String labelText = "$col[83,255,155]{Overlays}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("ols")).img);
            }
            if (ttip.get("pose") != null) {
                String labelText = "$col[255,145,200]{Pose}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("pose")).img);
            }
            if (ttip.get("attr") != null) {
                String labelText = "$col[155,255,83]{Attr}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("attr")).img);
            }
            if (!tlays.isEmpty() && false) {
                String labelText = "$col[155,32,176]{Layers}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                for(String s: tlays)
                {
                    imgs.add(tooltipFont.render(s).img);
                }
            }
            if (ttip.get("poses") != null) {
                String labelText = "$col[255,128,128]{Poses}:";
                BufferedImage label = cachedImages.get(labelText);
                if (label == null) {
                    label = RichText.render(labelText, 0).img;
                    cachedImages.put(labelText, label);
                }
                imgs.add(label);
                imgs.add(tooltipFont.render(ttip.get("poses")).img);
            }
            return (oldttip = new TexI((ItemInfo.catimgs(0, imgs.toArray(new BufferedImage[0])))));
        }
        oldttip = null;
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
                    Gob player = NUtils.player();
                    if (gob != null) {
                        ttip.put("gob", gob.ngob.name);
                        if(gob.ngob.hitBox!=null) {
                            ttip.put("HitBox", gob.ngob.hitBox.toString());
                            ttip.put("isDynamic", String.valueOf(gob.ngob.isDynamic));
                        }
                        if(player!=null)
                            ttip.put("dist", String.valueOf(gob.rc.dist(player.rc)));
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
                try {
                    int tile = mCache.gettile(mc.div(tilesz).floor());
                    Resource res = mCache.tilesetr(tile);
                    if (res != null) {
                        ttip.put("tile", res.name);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
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
            newArea.path = NUtils.getGameUI().areas.currentPath;
            glob.map.areas.put(id, newArea);
//            NUtils.getGameUI().areas.addArea(id, newArea.name, newArea);

            routeGraphManager.getGraph().connectAreaToRoutePoints(newArea);
            createAreaLabel(id);
        }
        return key;
    }

    public String addRoute()
    {
        String key;
        synchronized (((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes())
        {
            HashSet<String> names = new HashSet<String>();
            int id = 0;
            for(Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values())
            {
                if(route.id >= id)
                {
                    id = route.id + 1;
                }
                names.add(route.name);
            }
            key = ("New Route" + ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().size());
            while(names.contains(key))
            {
                key = key+"(1)";
            }
            Route newRoute = new Route(key);
            newRoute.id = id;
            newRoute.path = NUtils.getGameUI().routesWidget.currentPath;
            ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().put(id, newRoute);
            createRouteLabel(id);
        }
        return key;
    }

    public String addHearthFireRoute()
    {
        String key;
        synchronized (((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes())
        {
            HashSet<String> names = new HashSet<String>();
            int id = 0;
            for(Route route : ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().values())
            {
                if(route.id >= id)
                {
                    id = route.id + 1;
                }
                names.add(route.name);
            }
            key = ("New Route" + ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().size());
            while(names.contains(key))
            {
                key = key+"(1)";
            }
            Route newRoute = new Route(key);
            newRoute.id = id;
            newRoute.path = NUtils.getGameUI().routesWidget.currentPath;
            newRoute.spec.add(new Route.RouteSpecialization("HearthFires"));
            ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().put(id, newRoute);
            createRouteLabel(id);
        }
        return key;
    }


    boolean botsInit = false;

    @Override
    public void tick(double dt)
    {
        checkTempMarks();
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

        if(NConfig.botmod != null && !botsInit) {
            Scenario scenario = NUtils.getUI().core.scenarioManager.getScenarios().getOrDefault(NConfig.botmod.scenarioId, null);
            if (scenario != null || !(NUtils.getGameUI() == null)) {
                botsInit = true;
                Thread t;
                t = new Thread(() -> {
                    try {
                        NConfig.botmod = null;
                        NUtils.getUI().core.addTask(new WaitForMapLoadNoCoord(NUtils.getGameUI()));
                        ScenarioRunner runner = new ScenarioRunner(scenario);
                        runner.run(NUtils.getGameUI());

                        NUtils.getGameUI().act("lo");
                        System.exit(0);
                    } catch (InterruptedException e) {
                        System.out.println("Bot interrupted");
                    }
                });
                NUtils.getGameUI().biw.addObserve(t);
                t.start();
            }
        }
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
    public boolean mousedown(MouseDownEvent ev)
    {
        // Check for route point drag start
        if(ev.b == 1 && !isDraggingRoutePoint) { // Left mouse button
            RouteLabel clickedLabel = getRouteLabeAt(ev.c);
            if(clickedLabel != null) {
                isDraggingRoutePoint = true;
                draggedRouteLabel = clickedLabel;
                draggedRouteLabel.startDrag();
                dragGrab = ui.grabmouse(this);
                return true;
            }
        }
        
        if ( isAreaSelectionMode.get() )
        {
            if (selection == null)
            {
                selection = new NSelector(null);
            }
        }
        if ( isGobSelectionMode.get() )
        {
            getGob(ev.c);
            return false;
        }
        return super.mousedown(ev);
    }

    private Coord lastCoord = null;
    private Coord2d lastCoord2d = new Coord2d();
    @Override
    public void mousemove(MouseMoveEvent ev) {
        lastCoord = ev.c;
        
        // Handle route point dragging
        if(isDraggingRoutePoint && draggedRouteLabel != null) {
            // Update preview position immediately with screen coordinates
            draggedRouteLabel.updateDragPreview(ev.c);
            
            // Capture the reference to avoid race conditions with async Hittest
            final RouteLabel currentDraggedLabel = draggedRouteLabel;
            
            // Check if coordinates are within valid bounds before attempting Hittest
            if(ev.c.x >= 0 && ev.c.y >= 0 && ev.c.x < sz.x && ev.c.y < sz.y) {
                try {
                    // Convert screen coordinate to world coordinate using Hittest
                    new Hittest(ev.c) {
                        public void hit(Coord pc, Coord2d mc, ClickData inf) {
                            if(mc != null && currentDraggedLabel != null) {
                                currentDraggedLabel.updatePosition(mc);
                            }
                        }
                        
                        protected void nohit(Coord pc) {
                            // Ignore if no hit - mouse outside valid map area
                        }
                    }.run();
                } catch (Exception e) {
                    // Ignore hittest errors when mouse is outside valid area
                }
            }
            return;
        }
        
        super.mousemove(ev);
    }
    
    @Override
    public boolean mouseup(MouseUpEvent ev) {
        if(isDraggingRoutePoint) {
            if(ev.b == 1) {
                // Left mouse button - finalize drag
                isDraggingRoutePoint = false;
                if(dragGrab != null) {
                    dragGrab.remove();
                    dragGrab = null;
                }
                if(draggedRouteLabel != null) {
                    draggedRouteLabel.finalizeDrag();
                    draggedRouteLabel = null;
                }
                return true;
            } else if(ev.b == 3) {
                // Right mouse button - cancel drag
                isDraggingRoutePoint = false;
                if(dragGrab != null) {
                    dragGrab.remove();
                    dragGrab = null;
                }
                if(draggedRouteLabel != null) {
                    draggedRouteLabel.cancelDrag();
                    draggedRouteLabel = null;
                }
                return true;
            }
        }
        return super.mouseup(ev);
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
    public boolean keyup(KeyUpEvent ev) {
        if(ev.code == 16) {
            shiftPressed = false;
            ttip.clear();
        }
        return super.keyup(ev);
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
        if(ev.code == 16) {
            shiftPressed = true;
            inspect(lastCoord);
        }
        if(kb_quickaction.key().match(ev) || kb_quickignaction.key().match(ev) || kb_mousequickaction.key().match(ev)) {
            Thread t;
            (t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if(kb_quickaction.key().match(ev))
                            new QuickActionBot(false, false).run(NUtils.getGameUI());
                        else if(kb_quickignaction.key().match(ev))
                            new QuickActionBot(true, false).run(NUtils.getGameUI());
                        else if(kb_mousequickaction.key().match(ev))
                            new QuickActionBot(false, true).run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().msg("quick action error" + ":" + "STOPPED");
                    }
                }
            }, "quick action")).start();


        }

        if(kb_displaypbox.key().match(ev) ){
            boolean val = (Boolean) NConfig.get(NConfig.Key.player_box);
            NConfig.set(NConfig.Key.player_box, !val);
            NUtils.getGameUI().msg("Player gridbox: " + !val);
        }
        if(kb_displayfov.key().match(ev) ){
            boolean val = (Boolean) NConfig.get(NConfig.Key.player_fov);
            NConfig.set(NConfig.Key.player_fov, !val);
            NUtils.getGameUI().msg("Player vofbox: " + !val);
        }
        if(kb_displaygrid.key().match(ev) ){
            boolean val = (Boolean) NConfig.get(NConfig.Key.gridbox);
            NConfig.set(NConfig.Key.gridbox, !val);
            NUtils.getGameUI().msg("Gridbox: " + !val);
        }
        if(kb_togglebb.key().match(ev)) {
            boolean val = (Boolean) NConfig.get(NConfig.Key.showBB);
            NConfig.set(NConfig.Key.showBB, !val);
            NUtils.getGameUI().msg("Bounding Boxes: " + (!val ? "enabled" : "disabled"));
            
            if (NUtils.getGameUI() != null && NUtils.getGameUI().opts != null && NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                OptWnd.NSettingsPanel panel = (OptWnd.NSettingsPanel) NUtils.getGameUI().opts.nqolwnd;
                if (panel.settingsWindow != null && panel.settingsWindow.qol != null) {
                    panel.settingsWindow.qol.syncShowBB();
                }
            }
        }
        if(kb_togglenature.key().match(ev)) {
            boolean val = (Boolean) NConfig.get(NConfig.Key.hideNature);
            NConfig.set(NConfig.Key.hideNature, !val);
            NUtils.getGameUI().msg("Hide Nature: " + (!val ? "enabled" : "disabled"));
            NUtils.showHideNature();
            
            // Sync with mini map
            if (NUtils.getGameUI() != null && NUtils.getGameUI().mmapw != null) {
                NUtils.getGameUI().mmapw.natura.a = !(!val);
            }
            
            // Sync with QoL panel
            if (NUtils.getGameUI() != null && NUtils.getGameUI().opts != null && NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                OptWnd.NSettingsPanel panel = (OptWnd.NSettingsPanel) NUtils.getGameUI().opts.nqolwnd;
                if (panel.settingsWindow != null && panel.settingsWindow.qol != null) {
                    panel.settingsWindow.qol.syncHideNature();
                }
            }
            
            // Sync with World settings panel
            if (NUtils.getGameUI() != null && NUtils.getGameUI().opts != null && NUtils.getGameUI().opts.nqolwnd instanceof OptWnd.NSettingsPanel) {
                OptWnd.NSettingsPanel panel = (OptWnd.NSettingsPanel) NUtils.getGameUI().opts.nqolwnd;
                if (panel.settingsWindow != null && panel.settingsWindow.world != null) {
                    panel.settingsWindow.world.setNatureStatus(!val);
                }
            }
        }
        return super.keydown(ev);
    }

    public class NSelector extends Selector
    {
        public NSelector(Coord max) {
            super(max);
        }

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
                glob.map.areas.remove(area.id);
                Gob dummy = dummys.get(area.gid);
                if(dummy != null) {
                    glob.oc.remove(dummy);
                    dummys.remove(area.gid);
                }
                NUtils.getGameUI().areas.removeArea(area.id);

                routeGraphManager.getGraph().deleteAreaFromRoutePoints(area.id);

                break;
            }
        }
    }

    public void disableArea(String name, String path, boolean val) {
        for(NArea area : glob.map.areas.values())
        {
            if(area.name.equals(name) && area.path.equals(path))
            {
                area.hide = val;
                NConfig.needAreasUpdate();
                return;
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
                    routeGraphManager.getGraph().deleteAreaFromRoutePoints(area.id);
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

    public void changeRouteName(Integer id, String new_name)
    {
        ((NMapView) NUtils.getGameUI().map).routeGraphManager.getRoutes().get(id).name = new_name;
        NConfig.needRoutesUpdate();
    }

    void getGob(Coord c) {
        new Hittest(c) {
            @Override
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                if (inf != null) {
                    Gob gob = Gob.from(inf.ci);
                    if (gob != null) {
                        selectedGob = gob;
                    }
                    isGobSelectionMode.set(false);
                }
            }
        }.run();
    }
//
//    @Override
//    public boolean drop(final Coord cc, Coord ul) {
//        if(!ui.modctrl) {
//            new Hittest(cc) {
//                public void hit(Coord pc, Coord2d mc, ClickData inf) {
//                    click(mc, 1, ui.mc, mc.floor(posres), 1, ui.modflags());
//                }
//            }.run();
//            return true;
//        }
//        new Hittest(cc) {
//            public void hit(Coord pc, Coord2d mc, ClickData inf) {
//                wdgmsg("drop", pc, mc.floor(posres), ui.modflags());
//            }
//        }.run();
//        return(true);
//    }
//
//    public void click(Coord2d mc, int button, Object... args) {
////        boolean send = true;
////        if(button == 1 ) {
////            if(ui.modmeta) {
////                args[3] = 0;
////                send = NUtils.getGameUI().pathQueue.add(mc);
////            } else {
////                if(NUtils.isIdleCurs())
////                    NUtils.getGameUI().pathQueue.start(mc);
////            }
////        }
////        if(button == 3){
////            if(NUtils.getGameUI().pathQueue.size()<=1)
////                NUtils.getGameUI().pathQueue.clear();
////        }
////        if(send && !NUtils.getGameUI().nomadMod)
//            wdgmsg("click", args);
//    }


    void checkTempMarks() {
        if ((Boolean) NConfig.get(NConfig.Key.tempmark)) {
            final Coord2d cmap = new Coord2d(cmaps);
            if (NUtils.player() != null) {
                Coord2d pl = NUtils.player().rc;
                final List<NMiniMap.TempMark> marks = new ArrayList<>(tempMarkList);
                long currenttime = System.currentTimeMillis();
                for (NMiniMap.TempMark cm : marks) {
                    Gob g = Finder.findGob(cm.id);
                    if (g == null) {

                        if (currenttime - cm.start > (Integer) NConfig.get(NConfig.Key.temsmarktime) * 1000 * 60) {
                            tempMarkList.remove(cm);
                        } else {
                            if(currenttime - cm.lastupdate > 1000) {
                                cm.lastupdate = currenttime;
                                if (!cm.rc.isect(pl.sub(cmap.mul((Integer) NConfig.get(NConfig.Key.temsmarkdist)).mul(tilesz)), pl.add(cmap.mul((Integer) NConfig.get(NConfig.Key.temsmarkdist)).mul(tilesz)))) {
                                    tempMarkList.remove(cm);
                                } else {
                                    if (((NMiniMap) ui.gui.mmap).checktemp(cm, pl)) {
                                        tempMarkList.remove(cm);
                                    }
                                }
                            }
                        }
                    } else {
                        cm.start = currenttime;
                        cm.lastupdate = cm.start;
                        cm.rc = g.rc;
                        cm.gc = g.rc.floor(tilesz).add(ui.gui.mmap.sessloc.tc);
                    }
                }
            }
        }
    }

    // Extended Plob class with bounding box support
    public class NPlob extends Plob {
        private NModelBox boundingBox;

        public NPlob(Indir<Resource> res, Message sdt) {
            super(res, sdt);
            // Add bounding box support for temporal objects
            addPlobBoundingBox(res, sdt);
        }

        // Add bounding box support for Plob objects using Gobcopy hitbox
        private void addPlobBoundingBox(Indir<Resource> res, Message sdt)
        {
            // Get the Gob copy that will be placed to extract its hitbox
            ResDrawable drawable = getattr(ResDrawable.class);
            if (drawable != null && drawable.spr instanceof Gobcopy)
            {
                Gobcopy gobcopy = (Gobcopy) drawable.spr;
                Gob targetGob = gobcopy.gob;

                // Check if the target Gob has a hitbox
                if (targetGob != null && targetGob.ngob != null && targetGob.ngob.hitBox != null)
                {
                    // Add NModelBox overlay using the existing hitbox from the target Gob
                    boundingBox = new NModelBox(targetGob);
                    addcustomol(boundingBox);
                }
            }
        }
    }

    // Override uimsg to use NPlob instead of Plob
    @Override
    public void uimsg(String msg, Object... args) {
        if(msg.equals("place")) {
            Loader.Future<Plob> placing = this.placing;
            if(placing != null) {
                if(!placing.cancel()) {
                    Plob ob = placing.get();
                    synchronized(ob) {
                        ob.slot.remove();
                    }
                }
                this.placing = null;
            }
            int a = 0;
            Indir<Resource> res = ui.sess.getresv(args[a++]);
            Message sdt;
            if((args.length > a) && (args[a] instanceof byte[]))
                sdt = new MessageBuf((byte[])args[a++]);
            else
                sdt = Message.nil;
            int oa = a;
            // Use NPlob instead of Plob
            this.placing = glob.loader.defer(new Supplier<Plob>() {
                int a = oa;
                Plob ret = null;
                public Plob get() {
                    if(ret == null)
                        ret = new NPlob(res, new MessageBuf(sdt)); // Use NPlob here
                    while(a < args.length) {
                        int a2 = a;
                        Indir<Resource> ores = ui.sess.getresv(args[a2++]);
                        Message odt;
                        if((args.length > a2) && (args[a2] instanceof byte[]))
                            odt = new MessageBuf((byte[])args[a2++]);
                        else
                            odt = Message.nil;
                        ret.addol(ores, odt);
                        a = a2;
                    }
                    ret.place();
                    return(ret);
                }
            });
        } else {
            // For all other messages, use the parent implementation
            super.uimsg(msg, args);
        }
    }

}
