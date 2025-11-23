package nurgling;

import haven.*;
import haven.render.RenderTree;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

import haven.Composite;
import haven.res.ui.gobcp.Gobcopy;
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
    public static final KeyBinding kb_cyclebbmode = KeyBinding.get("cyclebbmode",  KeyMatch.forcode(KeyEvent.VK_N, KeyMatch.C | KeyMatch.S));
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
    final long tooltipThrottleTime = 100; // milliseconds for throttling
    TexI oldttip = null;
    public AtomicBoolean isAreaSelectionMode = new AtomicBoolean(false);
    public AtomicBoolean isGobSelectionMode = new AtomicBoolean(false);
    public NArea.Space areaSpace = null;
    public Pair<Coord, Coord> currentSelectionCoords = null;  // Current selection coords during dragging
    public boolean rotationRequested = false;  // Flag to request rotation during area selection
    public Gob selectedGob = null;
    public static boolean isRecordingRoutePoint = false;

    public HashMap<Long, Gob> dummys = new HashMap<>();
    public HashMap<Long, Gob> routeDummys = new HashMap<>();

    public RouteGraphManager routeGraphManager = new RouteGraphManager();

    // Track if overlays have been initialized to avoid repeated initialization checks
    private boolean overlaysInitialized = false;

    // Directional vectors for triangulation (fixed position, not following player)
    public java.util.List<nurgling.tools.DirectionalVector> directionalVectors = new java.util.ArrayList<>();

    // Marker line system (lines to selected marker icon - follows player)
    public MiniMap.DisplayMarker selectedMarker = null;
    public Coord selectedMarkerTileCoords = null;
    public NMarkerLineOverlay markerLineOverlay = null;
    private RenderTree.Slot markerLineSlot = null;

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
        // Initialize overlays only once on first draw (when GameUI is ready)
        if (!overlaysInitialized) {
            getRockTileOverlay(); // Initialize rock tile highlighting overlay
            // getShortWallCapOverlay(); // No longer needed - NCaveTile renders caps directly
            overlaysInitialized = true;
        }

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

    public static NRockTileHighlightOverlay getRockTileOverlay()
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            synchronized (NUtils.getGameUI().map)
            {
                NRockTileHighlightOverlay overlay = (NRockTileHighlightOverlay) NUtils.getGameUI().map.nols.get(NRockTileHighlightOverlay.ROCK_TILE_OVERLAY);
                if (overlay == null)
                {
                    NUtils.getGameUI().map.addCustomOverlay(NRockTileHighlightOverlay.ROCK_TILE_OVERLAY, new NRockTileHighlightOverlay());
                }
                overlay = (NRockTileHighlightOverlay) NUtils.getGameUI().map.nols.get(NRockTileHighlightOverlay.ROCK_TILE_OVERLAY);
                return overlay;
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
        if(id == NRockTileHighlightOverlay.ROCK_TILE_OVERLAY)
        {
            return NUtils.getGameUI().map.nols.get(NRockTileHighlightOverlay.ROCK_TILE_OVERLAY)!=null;
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

        // Check if any inspect mode is active
        boolean debugMode = NUtils.getGameUI() != null && NUtils.getGameUI().ui.core.debug && NUtils.getGameUI().ui.core.isinspect;
        boolean simpleInspect = NUtils.getGameUI() != null && NUtils.getGameUI().ui.core.isinspect && (Boolean) NConfig.get(NConfig.Key.simpleInspect);
        boolean isInspecting = debugMode || simpleInspect;
        
        if (NUtils.getGameUI()!=null && !ttip.isEmpty() && isInspecting) {

            Collection<BufferedImage> imgs = new LinkedList<>();

            // For simple inspect, only show gob and tile
            if (simpleInspect && !debugMode) {
                if (ttip.get("gob") != null) {
                    BufferedImage gob = RichText.render(String.format("$col[128,128,255]{%s}:", "Gob"), 0).img;
                    imgs.add(gob);
                    imgs.add(RichText.render(ttip.get("gob"), 0).img);
                }
                if (ttip.get("tile") != null) {
                    BufferedImage tile = RichText.render(String.format("$col[128,128,255]{%s}:", "Tile"), 0).img;
                    imgs.add(tile);
                    imgs.add(RichText.render(ttip.get("tile"), 0).img);
                }
            } else {
                // Debug mode - show all info
                for (String key : ttip.keySet()) {
                    String text = String.format("$col[128,128,255]{%s}:", key);
                    BufferedImage img = cachedImages.get(text);
                    if (img == null) {
                        img = RichText.render(text, 0).img;
                        cachedImages.put(text, img);
                    }

                    imgs.add(img);
                    imgs.add(RichText.render(ttip.get(key), 0).img);
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

    void inspectSimple(Coord c) {
        new Hittest(c) {
            @Override
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                ttip.clear();
                tlays.clear();
                // Show resource name if gob exists
                if (inf != null) {
                    Gob gob = Gob.from(inf.ci);
                    if (gob != null) {
                        ttip.put("gob", gob.ngob.name);
                    }
                }
                
                // Show tile resource
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
        // Update marker line overlay (follows player)
        if(markerLineOverlay != null) {
            markerLineOverlay.tick();
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
        // Alt+Ctrl+LMB activates area selection
        if(ev.b == 1 && ui.modmeta && ui.modctrl) {
            if(!isAreaSelectionMode.get()) {
                isAreaSelectionMode.set(true);
            }
            // Don't consume the event, let it pass through to start selection
            // return true;
        }
        
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
        
        // Ctrl+MMB to toggle ring setting for clicked object
        if (ev.b == 2 && ui.modctrl) { // Middle mouse button + Ctrl
            new Click(ev.c, ev.b) {
                @Override
                protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                    if (inf != null && inf.ci instanceof Gob.GobClick) {
                        Gob clickedGob = ((Gob.GobClick) inf.ci).gob;
                        if (clickedGob != null) {
                            toggleRingForGob(clickedGob);
                        }
                    }
                }
            }.run();
            return true;
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

        // Handle R key for rotation during area selection
        if(ev.code == 82 && isAreaSelectionMode.get()) {  // R key
            rotationRequested = true;
            return true;
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
        if(kb_cyclebbmode.key().match(ev)) {
            String currentMode = (String) NConfig.get(NConfig.Key.bbDisplayMode);
            if (currentMode == null) currentMode = "FILLED";
            
            String newMode;
            switch (currentMode) {
                case "FILLED":
                    newMode = "FILLED_ALWAYS";
                    break;
                case "FILLED_ALWAYS":
                    newMode = "OUTLINE";
                    break;
                case "OUTLINE":
                    newMode = "OUTLINE_ALWAYS";
                    break;
                case "OUTLINE_ALWAYS":
                    newMode = "OFF";
                    break;
                default:
                    newMode = "FILLED";
                    break;
            }
            
            NConfig.set(NConfig.Key.bbDisplayMode, newMode);
            
            // Display user-friendly message
            String displayMsg;
            switch (newMode) {
                case "FILLED":
                    displayMsg = "Filled (depth-aware)";
                    break;
                case "FILLED_ALWAYS":
                    displayMsg = "Filled (always visible)";
                    break;
                case "OUTLINE":
                    displayMsg = "Outline (depth-aware)";
                    break;
                case "OUTLINE_ALWAYS":
                    displayMsg = "Outline (always visible)";
                    break;
                case "OFF":
                    displayMsg = "Off";
                    break;
                default:
                    displayMsg = newMode;
                    break;
            }
            NUtils.getGameUI().msg("Bounding Box Mode: " + displayMsg);
            
            // If BB is disabled and user switches to a visible mode, enable it
            if (!newMode.equals("OFF") && !(Boolean) NConfig.get(NConfig.Key.showBB)) {
                NConfig.set(NConfig.Key.showBB, true);
            }
            // If user switches to OFF mode, disable BB
            else if (newMode.equals("OFF")) {
                NConfig.set(NConfig.Key.showBB, false);
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
        
        @Override
        public void mmousemove(Coord mc) {
            super.mmousemove(mc);
            // Update current selection coords for live ghost preview
            if (sc != null) {
                Coord tc = getec(mc);
                Coord c1 = new Coord(Math.min(tc.x, sc.x), Math.min(tc.y, sc.y));
                Coord c2 = new Coord(Math.max(tc.x, sc.x), Math.max(tc.y, sc.y));
                currentSelectionCoords = new Pair<>(c1, c2.add(1, 1));
            }
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
                    
                    // Send area to chat if it was activated via Alt+Ctrl+LMB
                    sendAreaToChat(areaSpace);
                    
                    currentSelectionCoords = null;
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
    
    /**
     * Send selected area to chat in @Area format
     * Format: @Area(grid:x,y;grid:x,y) - two corner points (upper-left and bottom-right)
     */
    private void sendAreaToChat(NArea.Space space) {
        if(space == null || space.space.isEmpty())
            return;
            
        try {
            // Find the overall bounding box across all grids
            Coord minWorldTile = null;
            Coord maxWorldTile = null;
            
            for(Map.Entry<Long, NArea.VArea> entry : space.space.entrySet()) {
                long gridId = entry.getKey();
                Area area = entry.getValue().area;
                
                // Get grid to calculate world tile coordinates
                MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(gridId);
                if(grid == null) continue;
                
                // Convert local grid tile coords to world tile coords
                // grid.gc is grid coordinate, area.ul/br are tile coords within the grid
                Coord worldULTile = grid.gc.mul(MCache.cmaps).add(area.ul);
                Coord worldBRTile = grid.gc.mul(MCache.cmaps).add(area.br);
                
                if(minWorldTile == null) {
                    minWorldTile = worldULTile;
                    maxWorldTile = worldBRTile;
                } else {
                    minWorldTile = new Coord(
                        Math.min(minWorldTile.x, worldULTile.x),
                        Math.min(minWorldTile.y, worldULTile.y)
                    );
                    maxWorldTile = new Coord(
                        Math.max(maxWorldTile.x, worldBRTile.x),
                        Math.max(maxWorldTile.y, worldBRTile.y)
                    );
                }
            }
            
            if(minWorldTile == null || maxWorldTile == null)
                return;
                
            // Convert world tile coords back to grid:local format for both corners
            Coord minGrid = minWorldTile.div(MCache.cmaps);
            Coord maxGrid = maxWorldTile.div(MCache.cmaps);
            
            Coord minLocal = minWorldTile.mod(MCache.cmaps);
            Coord maxLocal = maxWorldTile.mod(MCache.cmaps);
            
            MCache.Grid minGridObj = NUtils.getGameUI().map.glob.map.grids.get(minGrid);
            MCache.Grid maxGridObj = NUtils.getGameUI().map.glob.map.grids.get(maxGrid);
            
            if(minGridObj == null || maxGridObj == null)
                return;
            
            // Format: @Area(grid:x,y;grid:x,y)
            String areaStr = String.format("@Area(%d:%d,%d;%d:%d,%d)",
                minGridObj.id, minLocal.x, minLocal.y,
                maxGridObj.id, maxLocal.x, maxLocal.y);
            
            // Send to chat
            GameUI gui = NUtils.getGameUI();
            if(gui != null && gui.chat != null) {
                ChatUI.Channel chat = gui.chat.sel;
                if(chat instanceof ChatUI.EntryChannel) {
                    if(!chat.getClass().getName().contains("Realm")) {
                        ((ChatUI.EntryChannel)chat).send(areaStr);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
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

    /**
     * Adds a directional vector for triangulation
     * @param originTileCoords Tile coordinates where vector starts (segment-relative)
     * @param targetTileCoords Tile coordinates of the target (segment-relative)
     * @param targetName Name of the target
     * @param targetGobId Gob ID of the target (-1 if none)
     */
    public void addDirectionalVector(Coord originTileCoords, Coord targetTileCoords, String targetName, long targetGobId) {
        // Skip if origin and target are the same
        if(originTileCoords.equals(targetTileCoords)) {
            return;
        }

        nurgling.tools.DirectionalVector vector = new nurgling.tools.DirectionalVector(
            originTileCoords, targetTileCoords, targetName, targetGobId
        );
        directionalVectors.add(vector);
    }

    /**
     * Clears all directional vectors
     */
    public void clearDirectionalVectors() {
        directionalVectors.clear();
    }

    /**
     * Sets the selected marker for line drawing (called from minimap icon clicks)
     * Creates gold line on map and 3D line in world that follows player
     * @param marker The selected marker
     * @param tileCoords Tile coordinates of the marker, or null to clear
     */
    public void setSelectedMarker(MiniMap.DisplayMarker marker, Coord tileCoords) {
        this.selectedMarker = marker;
        this.selectedMarkerTileCoords = tileCoords;

        // Update 3D line overlay
        if(tileCoords == null) {
            // Clear selection
            setMarkerTarget(null);
        } else {
            // Set selection (calculate world position from tile coords)
            NGameUI gui = NUtils.getGameUI();
            if(gui != null && gui.mmap != null && gui.mmap.sessloc != null) {
                Coord2d worldPos = tileCoords.sub(gui.mmap.sessloc.tc).mul(MCache.tilesz).add(MCache.tilesz.div(2));
                setMarkerTarget(worldPos);
            }
        }
    }

    /**
     * Sets the marker target for 3D line overlay drawing
     * @param targetPos World position of the marker, or null to clear
     */
    public void setMarkerTarget(Coord2d targetPos) {
        if(targetPos == null) {
            // Clear the overlay
            if(markerLineSlot != null) {
                markerLineSlot.remove();
                markerLineSlot = null;
            }
            markerLineOverlay = null;
        } else {
            // Create or update the overlay
            if(markerLineOverlay == null) {
                markerLineOverlay = new NMarkerLineOverlay(() -> player());
                markerLineSlot = basic.add(markerLineOverlay);
            }
            markerLineOverlay.setTarget(targetPos);
        }
    }

    /**
     * Toggles ring display for a clicked gob
     * - If gob has GobIcon: saves to settings and updates all matching gobs
     * - If gob has no GobIcon: temporary ring (session-only)
     */
    private void toggleRingForGob(Gob clickedGob) {
        if (clickedGob == null) return;
        
        // Get the gob's icon attribute
        GobIcon icon = clickedGob.getattr(GobIcon.class);
        if (icon == null) {
            // No GobIcon - use temporary ring
            toggleTempRingForGob(clickedGob);
            return;
        }
        
        // Get the settings configuration
        NGameUI gui = NUtils.getGameUI();
        if (gui == null || gui.iconconf == null || gui.iconRingConfig == null) return;
        
        // Get icon instance and create setting ID
        GobIcon.Icon iconInstance = icon.icon();
        GobIcon.Setting.ID settingId = new GobIcon.Setting.ID(iconInstance.res.name, iconInstance.id());
        
        // Get setting using the proper get() method that handles creation
        GobIcon.Setting setting = gui.iconconf.get(iconInstance);
        if (setting == null) return;
        
        // Toggle the ring value
        setting.ring = !setting.ring;
        
        // Save to local config
        String iconResName = iconInstance.res.name;
        gui.iconRingConfig.setRing(iconResName, setting.ring);
        
        // Update all gobs with this icon setting (add or remove rings)
        try {
            synchronized(ui.sess.glob.oc) {
                for(Gob gob : ui.sess.glob.oc) {
                    GobIcon gobIcon = gob.getattr(GobIcon.class);
                    if(gobIcon != null) {
                        try {
                            // Create ID for this gob's icon to compare
                            GobIcon.Icon gobIconInstance = gobIcon.icon();
                            GobIcon.Setting.ID gobSettingId = new GobIcon.Setting.ID(gobIconInstance.res.name, gobIconInstance.id());
                            
                            // Compare by ID instead of object reference
                            if(gobSettingId.equals(settingId)) {
                                // Remove existing ring
                                Gob.Overlay existingRing = gob.findol(NGobIconRing.class);
                                if(existingRing != null) {
                                    existingRing.remove();
                                }
                                
                                // Add new ring if enabled
                                if(setting.ring) {
                                    NGobIconRing ring = NGobIconRing.createAutoSize(gob);
                                    if(ring != null) {
                                        gob.addcustomol(ring);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip this gob if there's an error
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during ring update
        }
        
        // Show feedback message
        String iconName = icon.icon().name();
        gui.msg("Ring " + (setting.ring ? "enabled" : "disabled") + " for " + iconName);
    }
    
    /**
     * Toggles temporary ring for objects without GobIcon
     * These rings are session-only and not saved to config
     * Applies to ALL objects with the same resource name
     */
    private void toggleTempRingForGob(Gob clickedGob) {
        if (clickedGob == null) return;
        
        NGameUI gui = NUtils.getGameUI();
        if (gui == null) return;
        
        // Get resource name
        String resName = clickedGob.ngob != null ? clickedGob.ngob.name : null;
        if (resName == null) {
            gui.msg("Cannot add ring - object has no resource name");
            return;
        }
        
        // Toggle state in temp config
        boolean currentState = gui.tempRingResources.getOrDefault(resName, false);
        boolean newState = !currentState;
        gui.tempRingResources.put(resName, newState);
        
        // Update all gobs with this resource name
        try {
            synchronized(ui.sess.glob.oc) {
                for(Gob gob : ui.sess.glob.oc) {
                    if (gob.ngob == null || gob.ngob.name == null) continue;
                    
                    if (gob.ngob.name.equals(resName)) {
                        // Remove existing temp ring
                        Gob.Overlay existingRing = gob.findol(nurgling.overlays.NGobTempRing.class);
                        if (existingRing != null) {
                            existingRing.remove();
                        }
                        
                        // Add new ring if enabled
                        if (newState) {
                            nurgling.overlays.NGobTempRing ring = nurgling.overlays.NGobTempRing.createAutoSize(gob);
                            if (ring != null) {
                                gob.addcustomol(ring);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during ring update
        }
        
        // Show feedback message
        gui.msg("Temporary ring " + (newState ? "enabled" : "disabled") + " for " + resName);
    }

}
