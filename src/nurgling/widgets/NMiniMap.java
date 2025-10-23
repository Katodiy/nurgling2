package nurgling.widgets;

import haven.*;
import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.LocalizedResourceTimer;
import nurgling.NGameUI;
import nurgling.overlays.map.MinimapClaimRenderer;
import nurgling.tools.FogArea;

import java.awt.*;
import java.awt.image.BufferedImage;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class NMiniMap extends MiniMap {
    public int scale = 1;
    public static final Coord _sgridsz = new Coord(100, 100);
    public static final Coord VIEW_SZ = UI.scale(_sgridsz.mul(9).div(tilesz.floor()));
    public static final Color VIEW_FOG_COLOR = new Color(255, 255, 0 , 120);
    public static final Color VIEW_BG_COLOR = new Color(255, 255, 255, 60);
    public static final Color VIEW_BORDER_COLOR = new Color(0, 0, 0, 128);
    public final FogArea fogArea = new FogArea(this);

    private String currentTerrainName = null;

    // Cache for fish icon textures to avoid reloading every frame
    private final java.util.HashMap<String, TexI> fishIconCache = new java.util.HashMap<>();

    // Cache for tree icon textures to avoid reloading every frame
    private final java.util.HashMap<String, TexI> treeIconCache = new java.util.HashMap<>();

    // Selected quest giver for line drawing
    private MiniMap.DisplayMarker selectedQuestGiver = null;
    private Coord selectedMarkerTileCoords = null; // Store tile coords for recalculation

    private static final Coord2d sgridsz = new Coord2d(new Coord(100,100));
    public NMiniMap(Coord sz, MapFile file) {
        super(sz, file);
    }

    public NMiniMap(MapFile file) {
        super(file);
    }

    public boolean checktemp(TempMark cm, Coord2d pl) {
        if(dloc!=null) {
            Coord rc = p2c(pl.floor(sgridsz).sub(4, 4).mul(sgridsz).add(22, 22));
            int zmult = 1 << zoomlevel;
            Coord viewsz = VIEW_SZ.div(zmult).mul(scale).sub(22, 22);
            Coord gc = p2c(cm.gc.sub(sessloc.tc).mul(tilesz));
            if (gc.isect(rc, viewsz)) {
                return true;
            }
        }
        return false;
    }

    public static class TempMark {
        public String name;
        public long start;
        public long lastupdate;
        public final long id;
        public Coord2d rc;
        public Coord gc;
        public TexI icon;

        public MiniMap.Location loc;

        public TempMark(String name, MiniMap.Location loc, long id, Coord2d rc, Coord gc, BufferedImage icon) {
            start = System.currentTimeMillis();
            lastupdate = start;
            this.name = name;
            this.id = id;
            this.rc = rc;
            this.gc = gc;
            this.icon = new TexI(icon);
            this.loc = loc;
        }
    }

    @Override
    public void drawparts(GOut g) {
        if(NUtils.getGameUI()==null)
            return;
        drawmap(g);

        // Render claim overlays (personal, village, realm)
        MinimapClaimRenderer.renderClaims(this, g);

        boolean playerSegment = (sessloc != null) && ((curloc == null) || (sessloc.seg.id == curloc.seg.id));
        if(zoomlevel <= 2 && (Boolean) NConfig.get(NConfig.Key.showGrid)) {drawgrid(g);}
        if(playerSegment && zoomlevel <= 1 && (Boolean)NConfig.get(NConfig.Key.showView)) {drawview(g);}

        if((Boolean) NConfig.get(NConfig.Key.fogEnable)) {
            g.chcolor(VIEW_FOG_COLOR);
            for (FogArea.Rectangle rect : fogArea.getCoveredAreas()) {
                if (rect!=null && curloc.seg.id == rect.seg_id && rect.ul != null && rect.br != null) {
                    g.frect2( p2c(rect.ul.sub(sessloc.tc).mul(tilesz)), p2c(rect.br.sub(sessloc.tc).mul(tilesz)));
                }
            }
            g.chcolor();
        }
        drawmarkers(g);
        if(dlvl == 0)
            drawicons(g);
        drawparty(g);


        drawtempmarks(g);
        drawterrainname(g);
        drawResourceTimers(g);
        drawFishLocations(g);
        drawTreeLocations(g);
        drawQueuedWaypoints(g);  // Draw waypoint visualization
        drawQuestGiverLine(g);   // Draw line to selected quest giver
    }

    // Draw queued waypoints visualization
    protected void drawQueuedWaypoints(GOut g) {
        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.waypointMovementService == null) return;

        synchronized(gui.waypointMovementService.movementQueue) {
            if((gui.waypointMovementService.movementQueue.isEmpty() &&
                gui.waypointMovementService.currentTarget == null) || sessloc == null || dloc == null)
                return;

            java.util.List<Location> allWaypoints = new java.util.ArrayList<>();
            if(gui.waypointMovementService.currentTarget != null)
                allWaypoints.add(gui.waypointMovementService.currentTarget);
            allWaypoints.addAll(gui.waypointMovementService.movementQueue);

            // Get player's current position on the map for drawing the line
            Coord playerScreenPos = null;
            try {
                if(ui != null && ui.gui != null && ui.gui.map != null) {
                    Coord2d playerWorld = new Coord2d(ui.gui.map.getcc());
                    playerScreenPos = p2c(playerWorld);
                }
            } catch(Loading l) {
                // Fall back to sessloc if player position not available
                playerScreenPos = xlate(sessloc);
            }

            // Draw lines connecting waypoints, starting from player position
            g.chcolor(0, 255, 255, 200); // Cyan color for waypoint paths
            Coord prevC = playerScreenPos;
            for(Location waypoint : allWaypoints) {
                if(waypoint.seg.id != sessloc.seg.id)
                    continue;

                Coord waypointC = xlate(waypoint);

                if(prevC != null && waypointC != null) {
                    g.line(prevC, waypointC, 2);
                }
                prevC = waypointC;
            }

            // Draw markers at each waypoint
            int num = 1;
            for(Location waypoint : allWaypoints) {
                if(waypoint.seg.id != sessloc.seg.id)
                    continue;

                Coord c = xlate(waypoint);
                if(c != null) {
                    // Draw circle
                    g.chcolor(255, 255, 0, 220); // Yellow marker
                    int radius = UI.scale(5);
                    g.fellipse(c, new Coord(radius, radius));

                    // Draw number
                    g.chcolor(0, 0, 0, 255);
                    Text numText = Text.render(String.valueOf(num));
                    g.aimage(numText.tex(), c, 0.5, 0.5);
                    numText.dispose();
                }
                num++;
            }
            g.chcolor();
        }
    }

    // Draw line from player to selected quest giver
    protected void drawQuestGiverLine(GOut g) {
        if(selectedQuestGiver == null || sessloc == null || dloc == null) return;

        // Get player's current position on the minimap
        Coord playerScreenPos = null;
        try {
            if(ui != null && ui.gui != null && ui.gui.map != null) {
                Coord2d playerWorld = new Coord2d(ui.gui.map.getcc());
                playerScreenPos = p2c(playerWorld);
            }
        } catch(Loading l) {
            // Fall back to sessloc if player position not available
            playerScreenPos = xlate(sessloc);
        } catch(Exception e) {
            // Handle any other errors
            return;
        }

        if(playerScreenPos == null) return;

        // Get quest giver position on minimap (works across segments)
        Coord hsz = sz.div(2);
        Coord questGiverScreenPos = selectedQuestGiver.m.tc.sub(dloc.tc).div(scalef()).add(hsz);

        // Draw line from player to quest giver
        g.chcolor(255, 215, 0, 220); // Gold color for quest giver path
        g.line(playerScreenPos, questGiverScreenPos, 3); // Thicker line for visibility
        g.chcolor();
    }

    void drawview(GOut g) {
        if(ui.gui.map==null)
            return;
        int zmult = 1 << zoomlevel;
        Coord2d sgridsz = new Coord2d(_sgridsz);
        Gob player = ui.gui.map.player();
        if(player != null) {
            Coord rc = p2c(player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz));
            Coord viewsz = VIEW_SZ.div(zmult).mul(scale);
            g.chcolor(VIEW_BG_COLOR);
            g.frect(rc, viewsz);
            g.chcolor(VIEW_BORDER_COLOR);
            g.rect(rc, viewsz);
            g.chcolor();
        }
    }

    void drawgrid(GOut g) {
        int zmult = 1 << zoomlevel;
        Coord offset = sz.div(2).sub(dloc.tc.div(scalef()));
        Coord zmaps = cmaps.div( (float)zmult).mul(scale);

        double width = UI.scale(1f);
        Color col = g.getcolor();
        g.chcolor(Color.RED);
        for (int x = dgext.ul.x * zmult; x < dgext.br.x * zmult; x++) {
            Coord a = UI.scale(zmaps.mul(x, dgext.ul.y * zmult)).add(offset);
            Coord b = UI.scale(zmaps.mul(x, dgext.br.y * zmult)).add(offset);
            if(a.x >= 0 && a.x <= sz.x) {
                a.y = Utils.clip(a.y, 0, sz.y);
                b.y = Utils.clip(b.y, 0, sz.y);
                g.line(a, b, width);
            }
        }
        for (int y = dgext.ul.y * zmult; y < dgext.br.y * zmult; y++) {
            Coord a = UI.scale(zmaps.mul(dgext.ul.x * zmult, y)).add(offset);
            Coord b = UI.scale(zmaps.mul(dgext.br.x * zmult, y)).add(offset);
            if(a.y >= 0 && a.y <= sz.y) {
                a.x = Utils.clip(a.x, 0, sz.x);
                b.x = Utils.clip(b.x, 0, sz.x);
                g.line(a, b, width);
            }
        }
        g.chcolor(col);
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(ui.gui.map==null)
            return;

        // Update 3D line target when session location changes (e.g., after teleport)
        if(selectedMarkerTileCoords != null && sessloc != null) {
            NGameUI gui = NUtils.getGameUI();
            if(gui != null && gui.map instanceof NMapView) {
                // Recalculate world position based on current session location
                Coord2d worldPos = selectedMarkerTileCoords.sub(sessloc.tc).mul(MCache.tilesz).add(MCache.tilesz.div(2));
                ((NMapView)gui.map).setQuestGiverTarget(worldPos);
            }
        }

        if((Boolean) NConfig.get(NConfig.Key.fogEnable)) {
            if ((sessloc != null) && ((curloc == null) || (sessloc.seg.id == curloc.seg.id))) {
                fogArea.tick(dt);
                Gob player = ui.gui.map.player();
                if (player != null && dloc != null) {
                    Coord ul = player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz).floor(tilesz).add(sessloc.tc);
                    Coord unscaledViewSize = _sgridsz.mul(9).div(tilesz.floor());
                    Coord br = ul.add(unscaledViewSize);
                    fogArea.addWithoutOverlaps(ul, br, curloc.seg.id);
                }
            }
        }

        // Process waypoint movement queue through the centralized service
        NGameUI gui = NUtils.getGameUI();
        if(gui != null && gui.waypointMovementService != null) {
            gui.waypointMovementService.processMovementQueue(file, sessloc);
        }
    }

    private void drawtempmarks(GOut g) {
        if((Boolean)NConfig.get(NConfig.Key.tempmark)) {
            Gob player = NUtils.player();
            if (player != null) {
                double zmult = 1 << zoomlevel;
                Coord rc = p2c(player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz));
                Coord viewsz = VIEW_SZ.div(zmult).mul(scale);

                synchronized (((NMapView)ui.gui.map).tempMarkList)
                {
                for (TempMark cm : ((NMapView)ui.gui.map).tempMarkList) {
                    if (cm.loc!=null && ui.gui.mmap.curloc.seg.id == cm.loc.seg.id) {
                        if (cm.icon != null) {
                            if (!cm.gc.equals(Coord.z)) {
                                Coord gc = p2c(cm.gc.sub(sessloc.tc).mul(tilesz));

                                int dsz = Math.max(cm.icon.sz().y, cm.icon.sz().x);
                                if (!gc.isect(rc, viewsz)) {
                                    g.aimage(cm.icon, gc, 0.5, 0.5, UI.scale(18 * cm.icon.sz().x / dsz, 18 * cm.icon.sz().y / dsz));
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    private void drawterrainname(GOut g) {
        if((Boolean)NConfig.get(NConfig.Key.showTerrainName) && currentTerrainName != null && !currentTerrainName.isEmpty()) {
            Text.Foundry fnd = new Text.Foundry(Text.dfont, 10);
            Text terrainText = fnd.render(currentTerrainName, Color.WHITE);
            Coord textPos = new Coord((sz.x - terrainText.sz().x) / 2, 5);
            g.chcolor(0, 0, 0, 180);
            g.frect(textPos.sub(2, 1), terrainText.sz().add(4, 2));
            g.chcolor();
            g.image(terrainText.tex(), textPos);
        }
    }



    @Override
    public void mousemove(MouseMoveEvent ev) {
        super.mousemove(ev);
        if((Boolean)NConfig.get(NConfig.Key.showTerrainName)) {
            updateCurrentTerrainName(ev.c);
        }
    }

    @Override
    public boolean mousewheel(MouseWheelEvent ev) {
        if(ev.a > 0) {
            if(scale > 1) {
                scale--;
            } else
            if(allowzoomout())
                zoomlevel = Math.min(zoomlevel + 1, dlvl + 1);
        } else {
            if(zoomlevel == 0 && scale < 4) {
                scale++;
            }
            zoomlevel = Math.max(zoomlevel - 1, 0);
        }
        return(true);
    }

    protected boolean allowzoomout() {
        if(zoomlevel >= 5)
            return(false);
        return(super.allowzoomout());
    }

    @Override
    public float scalef() {
        return(UI.unscale((float)(1 << dlvl))/scale);
    }

    @Override
    public Coord st2c(Coord tc) {
        return(UI.scale(tc.add(sessloc.tc).sub(dloc.tc).div(1 << dlvl)).mul(scale).add(sz.div(2)));
    }

    @Override
    public void drawmap(GOut g) {
        Coord hsz = sz.div(2);
        for(Coord c : dgext) {
            Coord ul = UI.scale(c.mul(cmaps).mul(scale)).sub(dloc.tc.div(scalef())).add(hsz);
            DisplayGrid disp = display[dgext.ri(c)];
            if(disp == null)
                continue;
            drawgrid(g, ul, disp);
        }
    }

    public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
        try {
            Tex img = disp.img();
            if(img != null)
                g.image(img, ul, UI.scale(img.sz()).mul(scale));
        } catch(Loading l) {
        }
    }

    @Override
    public void drawmarkers(GOut g) {
        Coord hsz = sz.div(2);

        // Get search pattern from NMapWnd if we're inside one
        String searchPattern = null;
        Widget parentWidget = this.parent;
        while(parentWidget != null) {
            if(parentWidget instanceof NMapWnd) {
                searchPattern = ((NMapWnd) parentWidget).searchPattern;
                break;
            }
            parentWidget = parentWidget.parent;
        }

        for(Coord c : dgext) {
            DisplayGrid dgrid = display[dgext.ri(c)];
            if(dgrid == null)
                continue;
            for(DisplayMarker mark : dgrid.markers(true)) {
                // First check the normal filter (marker config, etc.)
                if(filter(mark))
                    continue;

                // Then check search pattern filter
                if(searchPattern != null && !searchPattern.trim().isEmpty()) {
                    String markerName = mark.m.nm;
                    if(markerName == null) {
                        continue; // Hide markers with no name when searching
                    }
                    // Show only markers that contain the search pattern (case-insensitive)
                    if(!markerName.toLowerCase().contains(searchPattern.toLowerCase())) {
                        continue; // Hide markers that don't match
                    }
                }

                mark.draw(g, mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz));
            }
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if(dloc != null && sessloc != null) {
            Coord hsz = sz.div(2);

            // Check for tree location tooltip first (check in screen space)
            NGameUI gui = NUtils.getGameUI();
            if(gui != null && gui.treeLocationService != null) {
                // Check if markers are hidden (respect "Hide Markers" button)
                MapWnd mapwnd = gui.mapfile;
                boolean markersHidden = (mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall));

                if(!markersHidden) {
                    // Get search pattern (if any) for filtering
                    String searchPattern = null;
                    Widget parentWidget = this.parent;
                    while(parentWidget != null) {
                        if(parentWidget instanceof NMapWnd) {
                            searchPattern = ((NMapWnd) parentWidget).searchPattern;
                            break;
                        }
                        parentWidget = parentWidget.parent;
                    }

                    java.util.List<nurgling.TreeLocation> treeLocations = gui.treeLocationService.getTreeLocationsForSegment(sessloc.seg.id);
                    int threshold = UI.scale(10); // Screen pixels

                    for(nurgling.TreeLocation loc : treeLocations) {
                        // Apply search pattern filter (same as drawing)
                        if(searchPattern != null && !searchPattern.trim().isEmpty()) {
                            String treeName = loc.getTreeName();
                            if(treeName == null || !treeName.toLowerCase().contains(searchPattern.toLowerCase())) {
                                continue; // Skip trees that don't match search
                            }
                        }

                        Coord screenPos = loc.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

                        if(c.dist(screenPos) < threshold) {
                            return Text.render(loc.getTreeName());
                        }
                    }
                }
            }

            // Check for fish location tooltip (check in screen space)
            if(gui != null && gui.fishLocationService != null) {
                // Check if markers are hidden (respect "Hide Markers" button)
                MapWnd mapwnd = gui.mapfile;
                boolean markersHidden = (mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall));

                if(!markersHidden) {
                    // Get search pattern from NMapWnd if we're inside one
                    String searchPattern = null;
                    Widget parentWidget = this.parent;
                    while(parentWidget != null) {
                        if(parentWidget instanceof NMapWnd) {
                            searchPattern = ((NMapWnd) parentWidget).searchPattern;
                            break;
                        }
                        parentWidget = parentWidget.parent;
                    }

                    java.util.List<nurgling.FishLocation> locations = gui.fishLocationService.getFishLocationsForSegment(sessloc.seg.id);
                    int threshold = UI.scale(10); // Screen pixels

                    for(nurgling.FishLocation loc : locations) {
                        // Apply search pattern filter (same as drawing)
                        if(searchPattern != null && !searchPattern.trim().isEmpty()) {
                            String fishName = loc.getFishName();
                            if(fishName == null) {
                                continue; // Skip fish with no name when searching
                            }
                            // Show only fish that contain the search pattern (case-insensitive)
                            if(!fishName.toLowerCase().contains(searchPattern.toLowerCase())) {
                                continue; // Skip fish that don't match
                            }
                        }

                        // Convert segment-relative coordinates to screen coordinates (same as drawing)
                        Coord screenPos = loc.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

                        if(c.dist(screenPos) < threshold) {
                            // Simple tooltip with just the fish name
                            return Text.render(loc.getFishName());
                        }
                    }
                }
            }

            Coord tc = c.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
            DisplayMarker mark = markerat(tc);
            if(mark != null) {
                return(mark.tip);
            }

            // Get terrain type tooltip
            String terrainInfo = getTerrainTooltip(c);
            if(terrainInfo != null) {
                return(Text.render(terrainInfo));
            }
        }
        return(super.tooltip(c, prev));
    }
    
    private String getTerrainTooltip(Coord c) {
        // Only show terrain tooltip when Shift is pressed
        if(ui == null || !ui.modshift) {
            return null;
        }
        return getTerrainNameAtCoord(c);
    }
    
    private void updateCurrentTerrainName(Coord c) {
        String terrainName = getTerrainNameAtCoord(c);
        if(terrainName != null && !terrainName.equals(currentTerrainName)) {
            currentTerrainName = terrainName;
        } else if(terrainName == null) {
            currentTerrainName = null;
        }
    }
    
    private String getTerrainNameAtCoord(Coord c) {
        if(dloc == null || display == null || dgext == null) {
            return null;
        }
        
        try {
            // Convert screen coordinates to tile coordinates  
            Coord tc = c.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
            
            // Find which DisplayGrid contains this coordinate
            Coord zmaps = cmaps.mul(1 << dlvl);
            Coord gridCoord = tc.div(zmaps);
            
            // Check if this grid coordinate is in our display extent
            if(!dgext.contains(gridCoord)) {
                return null;
            }
            
            // Get the DisplayGrid
            DisplayGrid dgrid = display[dgext.ri(gridCoord)];
            if(dgrid == null) {
                return null;
            }
            
            // Get the DataGrid from the DisplayGrid
            MapFile.DataGrid grid = dgrid.gref.get();
            if(grid == null) {
                return null;
            }
            
            // Calculate coordinates within the grid (0-99 range)
            Coord localTC = tc.sub(gridCoord.mul(zmaps));
            Coord tileCoord = localTC.div(1 << dlvl);
            
            // Ensure coordinates are within grid bounds
            if(tileCoord.x < 0 || tileCoord.x >= cmaps.x || tileCoord.y < 0 || tileCoord.y >= cmaps.y) {
                return null;
            }
            
            // Get the tile type ID
            int tileId = grid.gettile(tileCoord);
            if(tileId < 0 || tileId >= grid.tilesets.length) {
                return null;
            }
            
            // Get the TileInfo for this tile
            MapFile.TileInfo tileInfo = grid.tilesets[tileId];
            if(tileInfo == null || tileInfo.res == null) {
                return null;
            }
            
            // Format the terrain name for display
            String resName = tileInfo.res.name;
            String terrainName = formatTerrainName(resName);
            
            return terrainName;
            
        } catch(Exception e) {
            // Silently handle any exceptions
            return null;
        }
    }

    private String formatTerrainName(String resName) {
        if(resName == null) {
            return "Unknown";
        }
        
        // Remove "gfx/tiles/" prefix if present
        String name = resName;
        if(name.startsWith("gfx/tiles/")) {
            name = name.substring("gfx/tiles/".length());
        }

        // Capitalize first letter and replace underscores with spaces
        name = name.replace("_", " ");
        if(name.length() > 0) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    private void drawResourceTimers(GOut g) {
        if(dloc == null) return;

        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.localizedResourceTimerService == null) return;

        java.util.List<LocalizedResourceTimer> timers = gui.localizedResourceTimerService.getTimersForSegment(dloc.seg.id);

        Coord hsz = sz.div(2);

        // Create bordered text furnaces for timer display (like barrel names and character nicknames)
        Text.Furnace readyTimerFurnace = new PUtils.BlurFurn(
            new Text.Foundry(Text.dfont, UI.scale(9), Color.GREEN).aa(true),
            2, 1, Color.BLACK
        );
        Text.Furnace activeTimerFurnace = new PUtils.BlurFurn(
            new Text.Foundry(Text.dfont, UI.scale(9), Color.WHITE).aa(true),
            2, 1, Color.BLACK
        );

        for(LocalizedResourceTimer timer : timers) {
            // Calculate screen position for the timer
            Coord screenPos = timer.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

            // Only draw if on screen
            if(screenPos.x >= 0 && screenPos.x <= sz.x &&
               screenPos.y >= 0 && screenPos.y <= sz.y) {

                String timeText = timer.getFormattedRemainingTime();

                // Use appropriate furnace based on timer state
                Text.Furnace furnace = timer.isExpired() ? readyTimerFurnace : activeTimerFurnace;
                Text timerDisplay = furnace.render(timeText);

                // Position text slightly below the resource icon
                Coord textPos = screenPos.add(-timerDisplay.sz().x / 2, 15);

                // Draw timer text with black border (no background needed)
                g.image(timerDisplay.tex(), textPos);
            }
        }
    }

    private void drawFishLocations(GOut g) {
        if(sessloc == null || dloc == null) return;

        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.fishLocationService == null) return;

        // Check if markers are hidden (respect "Hide Markers" button)
        MapWnd mapwnd = gui.mapfile;
        if(mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall)) {
            return; // Don't draw fish locations when markers are hidden
        }

        // Get search pattern from NMapWnd if we're inside one
        String searchPattern = null;
        Widget parentWidget = this.parent;
        while(parentWidget != null) {
            if(parentWidget instanceof NMapWnd) {
                searchPattern = ((NMapWnd) parentWidget).searchPattern;
                break;
            }
            parentWidget = parentWidget.parent;
        }

        // Use sessloc.seg.id like waypoints and markers do
        java.util.List<nurgling.FishLocation> fishLocations = gui.fishLocationService.getFishLocationsForSegment(sessloc.seg.id);

        Coord hsz = sz.div(2);

        for(nurgling.FishLocation fishLoc : fishLocations) {
            // Apply search pattern filter to fish names
            if(searchPattern != null && !searchPattern.trim().isEmpty()) {
                String fishName = fishLoc.getFishName();
                if(fishName == null) {
                    continue; // Hide fish with no name when searching
                }
                // Show only fish that contain the search pattern (case-insensitive)
                if(!fishName.toLowerCase().contains(searchPattern.toLowerCase())) {
                    continue; // Hide fish that don't match
                }
            }

            // Convert segment-relative coordinates to screen coordinates
            // Same approach as markers: mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz)
            Coord screenPos = fishLoc.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

            // Only draw if on screen
            if(screenPos.x >= 0 && screenPos.x <= sz.x &&
               screenPos.y >= 0 && screenPos.y <= sz.y) {

                try {
                    String fishResource = fishLoc.getFishResource();
                    TexI tex = fishIconCache.get(fishResource);

                    // Load and cache if not already cached
                    if(tex == null) {
                        Resource fishRes = Resource.remote().loadwait(fishResource);
                        BufferedImage icon = fishRes.layer(Resource.imgc).img;
                        tex = new TexI(icon);
                        fishIconCache.put(fishResource, tex);
                    }

                    // Draw scaled fish icon
                    int dsz = Math.max(tex.sz().y, tex.sz().x);
                    int targetSize = UI.scale(18);
                    g.aimage(tex, screenPos, 0.5, 0.5, UI.scale(targetSize * tex.sz().x / dsz, targetSize * tex.sz().y / dsz));

                } catch (Exception e) {
                    // Fallback: draw colored dot if icon fails
                    g.chcolor(0, 150, 255, 200); // Blue for fish
                    g.fellipse(screenPos, new Coord(UI.scale(4), UI.scale(4)));
                    g.chcolor();
                }
            }
        }
    }

    private void drawTreeLocations(GOut g) {
        if(sessloc == null || dloc == null) return;

        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.treeLocationService == null) return;

        // Check if markers are hidden (respect "Hide Markers" button)
        MapWnd mapwnd = gui.mapfile;
        if(mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall)) {
            return; // Don't draw tree locations when markers are hidden
        }

        // Get search pattern from NMapWnd if we're inside one
        String searchPattern = null;
        Widget parentWidget = this.parent;
        while(parentWidget != null) {
            if(parentWidget instanceof NMapWnd) {
                searchPattern = ((NMapWnd) parentWidget).searchPattern;
                break;
            }
            parentWidget = parentWidget.parent;
        }

        // Use sessloc.seg.id like waypoints and markers do
        java.util.List<nurgling.TreeLocation> treeLocations = gui.treeLocationService.getTreeLocationsForSegment(sessloc.seg.id);

        Coord hsz = sz.div(2);

        for(nurgling.TreeLocation treeLoc : treeLocations) {
            // Apply search pattern filter to tree names
            if(searchPattern != null && !searchPattern.trim().isEmpty()) {
                String treeName = treeLoc.getTreeName();
                if(treeName == null) {
                    continue; // Hide trees with no name when searching
                }
                // Show only trees that contain the search pattern (case-insensitive)
                if(!treeName.toLowerCase().contains(searchPattern.toLowerCase())) {
                    continue; // Hide trees that don't match
                }
            }

            // Convert segment-relative coordinates to screen coordinates
            Coord screenPos = treeLoc.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

            // Only draw if on screen
            if(screenPos.x >= 0 && screenPos.x <= sz.x &&
               screenPos.y >= 0 && screenPos.y <= sz.y) {

                try {
                    String treeResource = treeLoc.getTreeResource();

                    // Convert tree/bush resource path to minimap icon path
                    // "gfx/terobjs/trees/oak" -> "gfx/terobjs/mm/trees/oak"
                    // "gfx/terobjs/bushes/arrowwood" -> "gfx/terobjs/mm/bushes/arrowwood"
                    String mmResource = treeResource
                        .replace("gfx/terobjs/trees/", "gfx/terobjs/mm/trees/")
                        .replace("gfx/terobjs/bushes/", "gfx/terobjs/mm/bushes/");

                    TexI tex = treeIconCache.get(mmResource);

                    // Load and cache if not already cached
                    if(tex == null) {
                        Resource treeRes = Resource.remote().loadwait(mmResource);
                        BufferedImage icon = treeRes.layer(Resource.imgc).img;
                        tex = new TexI(icon);
                        treeIconCache.put(mmResource, tex);
                    }

                    // Draw scaled tree icon (same size as fish icons)
                    int dsz = Math.max(tex.sz().y, tex.sz().x);
                    int targetSize = UI.scale(18);
                    g.aimage(tex, screenPos, 0.5, 0.5, UI.scale(targetSize * tex.sz().x / dsz, targetSize * tex.sz().y / dsz));

                } catch (Exception e) {
                    // Fallback: draw green circle if icon fails
                    g.chcolor(34, 139, 34, 255);
                    g.fellipse(screenPos, new Coord(UI.scale(4), UI.scale(4)));
                    g.chcolor();
                }
            }
        }
    }

    private nurgling.FishLocation fishLocationAt(Coord tc) {
        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.fishLocationService == null || dloc == null) return null;

        java.util.List<nurgling.FishLocation> locations = gui.fishLocationService.getFishLocationsForSegment(dloc.seg.id);
        int threshold = UI.scale(10); // Click radius

        for(nurgling.FishLocation loc : locations) {
            if(loc.getTileCoords().dist(tc) < threshold) {
                return loc;
            }
        }
        return null;
    }

    @Override
    public boolean filter(DisplayMarker mark) {
        // Check if we're inside an NMapWnd and if it has an active search pattern
        Widget parent = this.parent;
        while(parent != null) {
            if(parent instanceof NMapWnd) {
                NMapWnd mapWnd = (NMapWnd) parent;
                String searchPattern = mapWnd.searchPattern;

                // If search pattern is active, filter by marker name
                if(searchPattern != null && !searchPattern.trim().isEmpty()) {
                    String markerName = mark.m.nm;
                    if(markerName == null) {
                        return true; // Hide markers with no name when searching
                    }
                    // Show only markers that contain the search pattern (case-insensitive)
                    if(!markerName.toLowerCase().contains(searchPattern.toLowerCase())) {
                        return true; // Hide markers that don't match
                    }
                }
                break;
            }
            parent = parent.parent;
        }

        // Default: don't filter (show the marker)
        return false;
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        // Check for right-click on fish location
        if(ev.b == 3 && dloc != null) { // Button 3 is right-clicked
            Coord tc = ev.c.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
            nurgling.FishLocation fishLoc = fishLocationAt(tc);
            if(fishLoc != null) {
                // Handle right-click on fish - will be processed in mouseup
                return true;
            }
        }
        return super.mousedown(ev);
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        // Handle right-click release on ANY marker - draw line to it
        if(ev.b == 3 && dloc != null && sessloc != null && display != null && dgext != null) {
            Coord hsz = sz.div(2);
            int threshold = UI.scale(10); // Same threshold as fish/tree

            // Loop through all markers and check if click is near one
            for(Coord c : dgext) {
                DisplayGrid dgrid = display[dgext.ri(c)];
                if(dgrid == null)
                    continue;

                for(DisplayMarker mark : dgrid.markers(true)) {
                    if(filter(mark))
                        continue;

                    // Calculate marker's screen position (same as drawmarkers)
                    Coord screenPos = mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz);

                    // Check if click is within threshold
                    if(ev.c.dist(screenPos) < threshold) {
                        System.out.println("=== MARKER CLICKED ===");
                        System.out.println("Marker: " + mark.m.nm);
                        System.out.println("Type: " + mark.m.getClass().getSimpleName());
                        System.out.println("Screen pos: " + screenPos);

                        // Toggle selection
                        if(selectedQuestGiver == mark) {
                            selectedQuestGiver = null;
                            selectedMarkerTileCoords = null;
                            System.out.println("Deselected");
                            NGameUI gui = NUtils.getGameUI();
                            if(gui != null && gui.map instanceof NMapView) {
                                ((NMapView)gui.map).setQuestGiverTarget(null);
                            }
                        } else {
                            selectedQuestGiver = mark;
                            selectedMarkerTileCoords = mark.m.tc; // Store tile coords
                            System.out.println("Selected at tile coords: " + mark.m.tc);
                            NGameUI gui = NUtils.getGameUI();
                            if(gui != null && gui.map instanceof NMapView) {
                                // Convert tile coords to world coords (relative to session location)
                                Coord2d worldPos = mark.m.tc.sub(sessloc.tc).mul(MCache.tilesz).add(MCache.tilesz.div(2));
                                System.out.println("World pos (corrected): " + worldPos);
                                ((NMapView)gui.map).setQuestGiverTarget(worldPos);
                            }
                        }
                        System.out.println("=== END ===");
                        return true;
                    }
                }
            }
        }

        // Handle right-click release on tree location - open details window
        if(ev.b == 3 && dloc != null && sessloc != null) { // Button 3 is right-clicked
            NGameUI gui = NUtils.getGameUI();
            if(gui != null && gui.treeLocationService != null) {
                // Check for tree location at click position (in screen space)
                java.util.List<nurgling.TreeLocation> treeLocations = gui.treeLocationService.getTreeLocationsForSegment(sessloc.seg.id);
                int threshold = UI.scale(10);
                Coord hsz = sz.div(2);

                for(nurgling.TreeLocation loc : treeLocations) {
                    Coord screenPos = loc.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

                    if(ev.c.dist(screenPos) < threshold) {
                        // Check if a window is already open for this tree location
                        String locationId = loc.getLocationId();
                        TreeLocationDetailsWindow existingWnd = gui.openTreeDetailWindows.get(locationId);

                        if(existingWnd != null && existingWnd.visible()) {
                            // Window already exists and is visible, just raise it
                            existingWnd.raise();
                        } else {
                            // Create new window and track it
                            TreeLocationDetailsWindow detailsWnd = new TreeLocationDetailsWindow(loc, gui);
                            gui.add(detailsWnd, new Coord(100, 100));
                            gui.openTreeDetailWindows.put(locationId, detailsWnd);
                        }
                        return true;
                    }
                }
            }
        }

        // Handle right-click release on fish location - open details window
        if(ev.b == 3 && dloc != null && sessloc != null) { // Button 3 is right-clicked
            NGameUI gui = NUtils.getGameUI();
            if(gui != null && gui.fishLocationService != null) {
                // Check for fish location at click position (in screen space)
                java.util.List<nurgling.FishLocation> locations = gui.fishLocationService.getFishLocationsForSegment(sessloc.seg.id);
                int threshold = UI.scale(10);
                Coord hsz = sz.div(2);

                for(nurgling.FishLocation loc : locations) {
                    Coord screenPos = loc.getTileCoords().sub(dloc.tc).div(scalef()).add(hsz);

                    if(ev.c.dist(screenPos) < threshold) {
                        // Check if a window is already open for this fish location
                        String locationId = loc.getLocationId();
                        FishLocationDetailsWindow existingWnd = gui.openFishDetailWindows.get(locationId);

                        if(existingWnd != null && existingWnd.visible()) {
                            // Window already exists and is visible, just raise it
                            existingWnd.raise();
                        } else {
                            // Create new window and track it
                            FishLocationDetailsWindow detailsWnd = new FishLocationDetailsWindow(loc, gui);
                            gui.add(detailsWnd, new Coord(100, 100));
                            gui.openFishDetailWindows.put(locationId, detailsWnd);
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseup(ev);
    }

    // Accessors for MinimapClaimRenderer to access protected MiniMap fields
    public DisplayGrid[] getDisplay() {
        return display;
    }

    public Area getDgext() {
        return dgext;
    }
}
