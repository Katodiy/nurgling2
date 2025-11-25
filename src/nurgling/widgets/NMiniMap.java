package nurgling.widgets;

import haven.*;
import nurgling.NConfig;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.LocalizedResourceTimer;
import nurgling.NGameUI;
import nurgling.overlays.map.MinimapClaimRenderer;
import nurgling.overlays.map.MinimapExploredAreaRenderer;
import nurgling.tools.ExploredArea;

import java.awt.*;
import java.awt.image.BufferedImage;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class NMiniMap extends MiniMap {
    public static final Coord _sgridsz = new Coord(100, 100);
    public static final Coord VIEW_SZ = UI.scale(_sgridsz.mul(9).div(tilesz.floor()));
    public static final Color VIEW_EXPLORED_COLOR = new Color(255, 255, 0, 144); // Yellow semi-transparent for explored area (120 + 20% of 120 = 144)
    public static final Color VIEW_BG_COLOR = new Color(255, 255, 255, 60);
    public static final Color VIEW_BORDER_COLOR = new Color(0, 0, 0, 128);
    public final ExploredArea exploredArea = new ExploredArea(this);

    private String currentTerrainName = null;

    // Cache for fish icon textures to avoid reloading every frame
    private final java.util.HashMap<String, TexI> fishIconCache = new java.util.HashMap<>();

    // Cache for tree icon textures to avoid reloading every frame
    private final java.util.HashMap<String, TexI> treeIconCache = new java.util.HashMap<>();

    // Visibility flags for tree and fish icons
    public boolean showTreeIcons = true;
    public boolean showFishIcons = true;

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
            int dataLevel = getDataLevel();
            float scaleFactor = getScaleFactor();
            float zmult = (float)(1 << dataLevel) / scaleFactor;
            Coord viewsz = VIEW_SZ.div(zmult).sub(22, 22);
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
        
        // Draw tile highlight overlay
        drawTileHighlightOverlay(g);

        // Render explored area overlay (yellow semi-transparent)
        MinimapExploredAreaRenderer.renderExploredArea(this, g);
        
        // Render claim overlays (personal, village, realm)
        MinimapClaimRenderer.renderClaims(this, g);

        boolean playerSegment = (sessloc != null) && ((curloc == null) || (sessloc.seg.id == curloc.seg.id));
        // Show grid when zoomed in enough (scale >= 0.25, i.e. not too far out)
        if(currentScale >= 0.25f && (Boolean) NConfig.get(NConfig.Key.showGrid)) {drawgrid(g);}
        // Show view box when zoomed in (scale >= 0.5)
        if(playerSegment && currentScale >= 0.5f && (Boolean)NConfig.get(NConfig.Key.showView)) {drawview(g);}
        drawmarkers(g);
        // Show icons on all zoom levels with high detail (data level 0 or 1)
        int dataLevel = getDataLevel();
        if(dataLevel <= 1)
            drawicons(g);
        drawparty(g);


        drawtempmarks(g);
        drawterrainname(g);
        drawResourceTimers(g);
        drawFishLocations(g);
        drawTreeLocations(g);
        drawQueuedWaypoints(g);  // Draw waypoint visualization
        drawMarkerLine(g);       // Draw line to selected marker
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

    // Clip a line to a rectangle boundary using Liang-Barsky algorithm
    private Coord2d[] clipLineToRect(Coord2d p1, Coord2d p2, Coord2d rectSize) {
        double x1 = p1.x, y1 = p1.y;
        double x2 = p2.x, y2 = p2.y;
        double dx = x2 - x1;
        double dy = y2 - y1;

        double t0 = 0.0, t1 = 1.0;

        // Check all four edges
        double[] pArr = {-dx, dx, -dy, dy};
        double[] qArr = {x1, rectSize.x - x1, y1, rectSize.y - y1};

        for(int i = 0; i < 4; i++) {
            if(pArr[i] == 0) {
                // Line is parallel to this edge
                if(qArr[i] < 0) {
                    return null; // Line is outside
                }
            } else {
                double r = qArr[i] / pArr[i];
                if(pArr[i] < 0) {
                    // Entering edge
                    if(r > t1) return null; // Line is outside
                    if(r > t0) t0 = r;
                } else {
                    // Exiting edge
                    if(r < t0) return null; // Line is outside
                    if(r < t1) t1 = r;
                }
            }
        }

        // Line is at least partially inside
        Coord2d newP1 = new Coord2d(x1 + t0 * dx, y1 + t0 * dy);
        Coord2d newP2 = new Coord2d(x1 + t1 * dx, y1 + t1 * dy);
        return new Coord2d[] {newP1, newP2};
    }

    // Draw line from player to selected marker
    protected void drawMarkerLine(GOut g) {
        NGameUI gui = NUtils.getGameUI();
        if(gui == null || !(gui.map instanceof NMapView)) return;
        NMapView mapView = (NMapView) gui.map;

        // Draw gold line to selected marker icon (follows player)
        if(mapView.selectedMarkerTileCoords != null && sessloc != null && dloc != null) {
            try {
                // Get player's current position on the minimap
                Coord playerScreenPos = null;
                if(ui != null && ui.gui != null && ui.gui.map != null) {
                    Coord2d playerWorld = new Coord2d(ui.gui.map.getcc());
                    playerScreenPos = p2c(playerWorld);
                } else {
                    playerScreenPos = xlate(sessloc);
                }

                if(playerScreenPos != null) {
                    // Get marker position on minimap
                    Coord hsz = sz.div(2);
                    Coord markerScreenPos = mapView.selectedMarkerTileCoords.sub(dloc.tc).div(scalef()).add(hsz);

                    // Clip line to map bounds
                    Coord2d[] clipped = clipLineToRect(new Coord2d(playerScreenPos), new Coord2d(markerScreenPos), new Coord2d(sz));
                    if(clipped != null) {
                        // Draw gold line from player to marker
                        g.chcolor(255, 215, 0, 220); // Gold color for marker path
                        g.line(clipped[0].floor(), clipped[1].floor(), 3); // Thicker line for visibility
                        g.chcolor();
                    }
                }
            } catch(Exception e) {
                // Ignore errors
            }
        }

        // Draw directional vectors (fixed rays, don't follow player)
        if(!mapView.directionalVectors.isEmpty() && dloc != null) {
            Coord hsz = sz.div(2);

            for(nurgling.tools.DirectionalVector vector : mapView.directionalVectors) {
                try {
                    // Convert tile coordinates to minimap screen coordinates
                    Coord originScreenPos = vector.originTileCoords.sub(dloc.tc).div(scalef()).add(hsz);

                    // Calculate a far point along the vector direction
                    double rayLength = 10000; // Tiles (effectively infinite on map scale)
                    Coord2d farPointTiles = vector.getTilePointAt(rayLength);
                    Coord farScreenPos = new Coord((int)farPointTiles.x, (int)farPointTiles.y).sub(dloc.tc).div(scalef()).add(hsz);

                    // Clip the vector line to map bounds
                    Coord2d[] clipped = clipLineToRect(new Coord2d(originScreenPos), new Coord2d(farScreenPos), new Coord2d(sz));
                    if(clipped != null) {
                        // Draw the ray from origin toward far point
                        g.chcolor(100, 150, 255, 200); // Blue color for directional vectors
                        g.line(clipped[0].floor(), clipped[1].floor(), 2);
                        g.chcolor();
                    }
                } catch(Exception e) {
                    // Skip this vector if there's an error
                    continue;
                }
            }
        }
    }

    void drawview(GOut g) {
        if(ui.gui.map==null || sessloc == null || dloc == null)
            return;
        Gob player = ui.gui.map.player();
        if(player != null) {
            // Use same calculation as explored area
            Coord ul = player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz).floor(tilesz).add(sessloc.tc);
            Coord unscaledViewSize = _sgridsz.mul(9).div(tilesz.floor());
            Coord br = ul.add(unscaledViewSize);
            
            // Expand BR by 1,1 to match explored area
            Coord expandedBR = br.add(1, 1);
            
            // Convert to screen coordinates
            Coord hsz = sz.div(2);
            Coord screenUL = ul.sub(dloc.tc).div(scalef()).add(hsz);
            Coord screenBR = expandedBR.sub(dloc.tc).div(scalef()).add(hsz);
            Coord screenSize = screenBR.sub(screenUL);
            
            g.chcolor(VIEW_BG_COLOR);
            g.frect(screenUL, screenSize);
            g.chcolor(VIEW_BORDER_COLOR);
            g.rect(screenUL, screenSize);
            g.chcolor();
        }
    }

    void drawgrid(GOut g) {
        if(dgext == null || dloc == null) return;
        
        int dataLevel = getDataLevel();
        float scaleFactor = getScaleFactor();
        Coord hsz = sz.div(2);
        
        double width = UI.scale(1f);
        Color col = g.getcolor();
        g.chcolor(Color.RED);
        
        // Draw grid lines at grid boundaries
        // Each grid is cmaps tiles at its data level
        int gridSizeInTiles = cmaps.x * (1 << dataLevel);
        
        for (int x = dgext.ul.x; x <= dgext.br.x; x++) {
            // Grid coordinate to tile coordinate
            Coord tilePosX = new Coord(x * gridSizeInTiles, 0);
            // Tile coordinate to screen coordinate
            Coord screenPos = UI.scale(tilePosX).mul(currentScale).sub(dloc.tc.div(scalef())).add(hsz);
            
            if(screenPos.x >= 0 && screenPos.x <= sz.x) {
                g.line(new Coord(screenPos.x, 0), new Coord(screenPos.x, sz.y), width);
            }
        }
        
        for (int y = dgext.ul.y; y <= dgext.br.y; y++) {
            // Grid coordinate to tile coordinate
            Coord tilePosY = new Coord(0, y * gridSizeInTiles);
            // Tile coordinate to screen coordinate
            Coord screenPos = UI.scale(tilePosY).mul(currentScale).sub(dloc.tc.div(scalef())).add(hsz);
            
            if(screenPos.y >= 0 && screenPos.y <= sz.y) {
                g.line(new Coord(0, screenPos.y), new Coord(sz.x, screenPos.y), width);
            }
        }
        
        g.chcolor(col);
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(ui.gui.map==null)
            return;

        NGameUI gui = NUtils.getGameUI();
        
        // Smooth zoom interpolation
        if(Math.abs(currentScale - targetScale) > 0.001f) {
            // Interpolate towards target scale
            currentScale += (targetScale - currentScale) * ZOOM_SPEED;
            
            // Snap to target if very close
            if(Math.abs(currentScale - targetScale) < 0.001f) {
                currentScale = targetScale;
            }
        }

        if((Boolean) NConfig.get(NConfig.Key.exploredAreaEnable)) {
            if ((sessloc != null) && ((curloc == null) || (sessloc.seg.id == curloc.seg.id))) {
                exploredArea.tick(dt);
                Gob player = ui.gui.map.player();
                if (player != null && dloc != null) {
                    Coord ul = player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz).floor(tilesz).add(sessloc.tc);
                    Coord unscaledViewSize = _sgridsz.mul(9).div(tilesz.floor());
                    Coord br = ul.add(unscaledViewSize);
                    
                    // Expand BR by 1,1 to cover rounding gaps
                    Coord expandedBR = br.add(1, 1);
                    
                    exploredArea.updateExploredTiles(ul, expandedBR, curloc.seg.id);
                }
            }
        }

        // Process waypoint movement queue through the centralized service
        if(gui != null && gui.waypointMovementService != null) {
            gui.waypointMovementService.processMovementQueue(file, sessloc);
        }
    }

    // Linear scale factor - this is the actual zoom level
    // scale = 4.0 means 4x zoom in, scale = 0.25 means 4x zoom out
    private float currentScale = 1.0f;
    private float targetScale = 1.0f;
    
    // Smooth zoom speed (how fast to interpolate to target)
    private static final float ZOOM_SPEED = 0.15f; // 15% per frame at 60fps = very smooth
    
    // Public accessor for currentScale (needed by MinimapClaimRenderer)
    public float getCurrentScale() {
        return currentScale;
    }
    
    // Invalidates all display caches to force complete map regeneration
    // Call this when settings change that affect map rendering (search, uniform colors, etc.)
    public void invalidateDisplayCache() {
        currentLevelCache = null;
        previousLevelCache = null;
        nextLevelCache = null;
        display = null;
        dgext = null;
    }
    
    // Returns true if terrain search is active in the main map window
    private boolean isTerrainSearchActive() {
        try {
            nurgling.NGameUI gui = nurgling.NUtils.getGameUI();
            if (gui != null && gui.mapfile != null) {
                String pattern = gui.mapfile.searchPattern;
                return pattern != null && !pattern.trim().isEmpty();
            }
        } catch (Exception ignored) { }
        return false;
    }

    // Helper method to calculate which data level to use based on current scale
    private int getDataLevel() {
        // When terrain search is active, force finest detail to ensure MapSource.drawmap()
        // is used so tile highlighting (selectedtex) is applied.
        if (cachedSearchActive) {
            return 0;
        }
        // Choose data level based on scale:
        // scale >= 1.0: use level 0 (finest detail)
        // scale >= 0.5: use level 0 (still detailed enough)
        // scale >= 0.25: use level 1 (2x coarser)
        // scale >= 0.125: use level 2 (4x coarser)
        // scale >= 0.0625: use level 3 (8x coarser)
        // etc.
        if(currentScale >= 0.5f) {
            return 0; // Use finest detail
        } else if(currentScale >= 0.25f) {
            return 1;
        } else if(currentScale >= 0.125f) {
            return 2;
        } else if(currentScale >= 0.0625f) {
            return 3;
        } else if(currentScale >= 0.03125f) {
            return 4;
        } else {
            return 5;
        }
    }
    
    // Public accessor for getDataLevel (needed by MinimapClaimRenderer)
    public int getDataLevelPublic() {
        return getDataLevel();
    }
    
    private float getScaleFactor() {
        // Calculate how much to scale the current data level
        int dataLevel = getDataLevel();
        
        // The scale factor is how much to scale the tiles at this data level
        // to achieve the desired currentScale
        // Each data level represents 2^level zoom out from level 0
        // So to get currentScale, we need: scaleFactor * (1 / 2^level) = currentScale
        // Therefore: scaleFactor = currentScale * 2^level
        
        return currentScale * (1 << dataLevel);
    }

    // Track current data level to detect when it changes
    private int currentDataLevel = 0;
    
    // Track last known search pattern to detect changes
    private String lastSearchPattern = "";
    
    // Cached state of whether terrain search is currently active
    private boolean cachedSearchActive = false;
    
    // Multi-level cache: keep current, previous, and next levels loaded
    // This eliminates black screens and loading freezes
    private class LevelCache {
        DisplayGrid[] display;
        Area dgext;
        int dataLevel;
        
        LevelCache(DisplayGrid[] display, Area dgext, int dataLevel) {
            this.display = display;
            this.dgext = dgext;
            this.dataLevel = dataLevel;
        }
    }
    
    private LevelCache currentLevelCache = null;
    private LevelCache previousLevelCache = null;
    private LevelCache nextLevelCache = null;

    // Override redisplay to support smooth zoom with fractional scaling
    protected void redisplay(Location loc) {
        Coord hsz = sz.div(2);
        
        int dataLevel = getDataLevel();
        float scaleFactor = getScaleFactor();
        
        // Check if search pattern changed and force rebuild if needed
        String currentSearchPattern = "";
        try {
            nurgling.NGameUI gui = nurgling.NUtils.getGameUI();
            if (gui != null && gui.mapfile != null && gui.mapfile.searchPattern != null) {
                currentSearchPattern = gui.mapfile.searchPattern;
            }
        } catch (Exception ignored) { }
        
        boolean searchPatternChanged = !currentSearchPattern.equals(lastSearchPattern);
        if (searchPatternChanged) {
            lastSearchPattern = currentSearchPattern;
            cachedSearchActive = !currentSearchPattern.trim().isEmpty();
            invalidateDisplayCache();
        }
        
        // Calculate grid size for this data level (in tiles)
        int gridTileSize = cmaps.x * (1 << dataLevel);
        
        // Calculate effective screen size of one tile
        float tileScreenSize = UI.scale(1) * scaleFactor / (1 << dataLevel);
        
        // Calculate how many tiles fit on screen
        int tilesOnScreenX = (int)Math.ceil(UI.unscale(sz.x) / (scaleFactor / (1 << dataLevel))) + gridTileSize * 2;
        int tilesOnScreenY = (int)Math.ceil(UI.unscale(sz.y) / (scaleFactor / (1 << dataLevel))) + gridTileSize * 2;
        
        // Calculate grid coordinates
        Coord centerGrid = loc.tc.div(gridTileSize);
        Coord gridsNeeded = new Coord(
            (int)Math.ceil((float)tilesOnScreenX / gridTileSize) + 2,
            (int)Math.ceil((float)tilesOnScreenY / gridTileSize) + 2
        );
        
        Area next = Area.sized(centerGrid.sub(gridsNeeded.div(2)), gridsNeeded);
        
        // Detect data level changes
        boolean dataLevelChanged = (dataLevel != currentDataLevel);
        
        if(dataLevelChanged) {
            // Shift cache: current becomes previous, next becomes current (if available)
            if(dataLevel > currentDataLevel) {
                // Zooming out: use preloaded next level if available
                previousLevelCache = currentLevelCache;
                if(nextLevelCache != null && nextLevelCache.dataLevel == dataLevel) {
                    currentLevelCache = nextLevelCache;
                    nextLevelCache = null;
                } else {
                    currentLevelCache = null;
                }
            } else {
                // Zooming in: use preloaded previous level if available
                nextLevelCache = currentLevelCache;
                if(previousLevelCache != null && previousLevelCache.dataLevel == dataLevel) {
                    currentLevelCache = previousLevelCache;
                    previousLevelCache = null;
                } else {
                    currentLevelCache = null;
                }
            }
            currentDataLevel = dataLevel;
        }
        
        // Update current level display
        boolean needsUpdate = (currentLevelCache == null) || 
                             (loc.seg != dseg) || 
                             (zoomlevel != dlvl) || 
                             !next.equals(dgext) || 
                             super.needUpdate || 
                             dataLevelChanged || 
                             searchPatternChanged;
                             
        if(needsUpdate) {
            DisplayGrid[] nd = new DisplayGrid[next.rsz()];
            
            // Try to reuse grids from cache
            if(currentLevelCache != null && !dataLevelChanged && currentLevelCache.dgext != null) {
                for(Coord c : currentLevelCache.dgext) {
                    if(next.contains(c))
                        nd[next.ri(c)] = currentLevelCache.display[currentLevelCache.dgext.ri(c)];
                }
            }
            
            super.needUpdate = false;
            currentLevelCache = new LevelCache(nd, next, dataLevel);
            
            // Update base class members
            display = nd;
            dseg = loc.seg;
            dlvl = zoomlevel;
            dgext = next;
            dtext = Area.sized(next.ul.mul(gridTileSize), next.sz().mul(gridTileSize));
        }
        dloc = loc;
        
        // Load grids for current level
        if(file.lock.readLock().tryLock()) {
            try {
                // Load current level grids
                if(currentLevelCache != null && currentLevelCache.display != null) {
                    for(Coord c : dgext) {
                        if(currentLevelCache.display[dgext.ri(c)] == null) {
                            currentLevelCache.display[dgext.ri(c)] = new DisplayGrid(dloc.seg, c, dataLevel, dloc.seg.grid(dataLevel, c.mul(1 << dataLevel)));
                        }
                    }
                    display = currentLevelCache.display;
                }
                
                // Preload next level (more zoomed out) in background
                int nextLevel = dataLevel + 1;
                if(nextLevel <= 5 && (nextLevelCache == null || nextLevelCache.dataLevel != nextLevel)) {
                    int nextGridTileSize = cmaps.x * (1 << nextLevel);
                    Coord nextCenterGrid = loc.tc.div(nextGridTileSize);
                    int nextTilesOnScreenX = (int)Math.ceil(UI.unscale(sz.x) / (currentScale / (1 << nextLevel))) + nextGridTileSize * 2;
                    int nextTilesOnScreenY = (int)Math.ceil(UI.unscale(sz.y) / (currentScale / (1 << nextLevel))) + nextGridTileSize * 2;
                    Coord nextGridsNeeded = new Coord(
                        (int)Math.ceil((float)nextTilesOnScreenX / nextGridTileSize) + 2,
                        (int)Math.ceil((float)nextTilesOnScreenY / nextGridTileSize) + 2
                    );
                    Area nextArea = Area.sized(nextCenterGrid.sub(nextGridsNeeded.div(2)), nextGridsNeeded);
                    
                    DisplayGrid[] nextDisplay = new DisplayGrid[nextArea.rsz()];
                    // Load a few grids to start preloading
                    int loaded = 0;
                    for(Coord c : nextArea) {
                        if(loaded++ > 4) break; // Don't load too many at once to avoid lag
                        nextDisplay[nextArea.ri(c)] = new DisplayGrid(dloc.seg, c, nextLevel, dloc.seg.grid(nextLevel, c.mul(1 << nextLevel)));
                    }
                    nextLevelCache = new LevelCache(nextDisplay, nextArea, nextLevel);
                }
                
                // Preload previous level (more zoomed in) in background
                int prevLevel = dataLevel - 1;
                if(prevLevel >= 0 && (previousLevelCache == null || previousLevelCache.dataLevel != prevLevel)) {
                    int prevGridTileSize = cmaps.x * (1 << prevLevel);
                    Coord prevCenterGrid = loc.tc.div(prevGridTileSize);
                    int prevTilesOnScreenX = (int)Math.ceil(UI.unscale(sz.x) / (currentScale / (1 << prevLevel))) + prevGridTileSize * 2;
                    int prevTilesOnScreenY = (int)Math.ceil(UI.unscale(sz.y) / (currentScale / (1 << prevLevel))) + prevGridTileSize * 2;
                    Coord prevGridsNeeded = new Coord(
                        (int)Math.ceil((float)prevTilesOnScreenX / prevGridTileSize) + 2,
                        (int)Math.ceil((float)prevTilesOnScreenY / prevGridTileSize) + 2
                    );
                    Area prevArea = Area.sized(prevCenterGrid.sub(prevGridsNeeded.div(2)), prevGridsNeeded);
                    
                    DisplayGrid[] prevDisplay = new DisplayGrid[prevArea.rsz()];
                    // Load a few grids to start preloading
                    int loaded = 0;
                    for(Coord c : prevArea) {
                        if(loaded++ > 4) break; // Don't load too many at once
                        prevDisplay[prevArea.ri(c)] = new DisplayGrid(dloc.seg, c, prevLevel, dloc.seg.grid(prevLevel, c.mul(1 << prevLevel)));
                    }
                    previousLevelCache = new LevelCache(prevDisplay, prevArea, prevLevel);
                }
            } finally {
                file.lock.readLock().unlock();
            }
        }
        for(DisplayIcon icon : icons)
            icon.dispupdate();
    }

    private void drawtempmarks(GOut g) {
        if((Boolean)NConfig.get(NConfig.Key.tempmark)) {
            Gob player = NUtils.player();
            if (player != null) {
                int dataLevel = getDataLevel();
                float scaleFactor = getScaleFactor();
                double zmult = (double)((1 << dataLevel) / scaleFactor);
                Coord rc = p2c(player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz));
                Coord viewsz = VIEW_SZ.div(zmult);

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
            // Zoom out - multiply by 0.95 (5% decrease per step)
            targetScale *= 0.95f;
            // Limit minimum scale
            if(targetScale < 0.03125f) // 1/32 zoom out
                targetScale = 0.03125f;
        } else {
            // Zoom in - multiply by 1.0526 (inverse of 0.95, ~5.3% increase)
            targetScale *= 1.0526f;
            // Limit maximum scale to 4x
            if(targetScale > 4.0f)
                targetScale = 4.0f;
        }
        
        // Update zoomlevel for compatibility with base class
        zoomlevel = (int)(Math.log(1.0f / targetScale) / Math.log(2) * 10);
        if(targetScale > 1.0f) zoomlevel = 0;
        
        return(true);
    }

    protected boolean allowzoomout() {
        // Allow zoom out as long as scale is above minimum
        return currentScale > 0.03125f;
    }

    @Override
    public float scalef() {
        int dataLevel = getDataLevel();
        float scaleFactor = getScaleFactor();
        return(UI.unscale((float)(1 << dataLevel) / scaleFactor));
    }

    @Override
    public Coord st2c(Coord tc) {
        int dataLevel = getDataLevel();
        float scaleFactor = getScaleFactor();
        
        Coord base = tc.add(sessloc.tc).sub(dloc.tc).div(1 << dataLevel);
        return(UI.scale(base).mul(scaleFactor).add(sz.div(2)));
    }

    @Override
    public Coord c2st(Coord c) {
        int dataLevel = getDataLevel();
        float scaleFactor = getScaleFactor();
        
        Coord unscaled = UI.unscale(c.sub(sz.div(2)).div(scaleFactor));
        return unscaled.mul(1 << dataLevel).add(dloc.tc).sub(sessloc.tc);
    }

    @Override
    public void drawmap(GOut g) {
        Coord hsz = sz.div(2);
        int dataLevel = getDataLevel();
        float scaleFactor = getScaleFactor();
        
        // Draw cached previous level if transitioning (to avoid black screen)
        boolean shouldDrawPrevious = false;
        if(previousLevelCache != null && previousLevelCache.display != null && previousLevelCache.dgext != null) {
            // Check if current level is fully loaded with textures
            int loadedGrids = 0;
            int totalGrids = 0;
            
            if(currentLevelCache != null && currentLevelCache.display != null && currentLevelCache.dgext != null) {
                for(Coord c : currentLevelCache.dgext) {
                    totalGrids++;
                    DisplayGrid disp = currentLevelCache.display[currentLevelCache.dgext.ri(c)];
                    if(disp != null) {
                        // Just check if grid object exists, don't force texture load
                        loadedGrids++;
                    }
                }
            }
            
            // Draw previous level if current level is less than 80% loaded
            // More conservative threshold to keep old level visible longer
            shouldDrawPrevious = (totalGrids == 0 || loadedGrids < totalGrids * 0.8f);
            
            if(shouldDrawPrevious) {
                // Draw previous level with adjusted scale
                float prevScaleFactor = currentScale * (1 << previousLevelCache.dataLevel);
                
                for(Coord c : previousLevelCache.dgext) {
                    DisplayGrid disp = previousLevelCache.display[previousLevelCache.dgext.ri(c)];
                    if(disp == null) continue;
                    
                    // Calculate position with exact tile boundaries to avoid gaps
                    Coord2d ulDouble = new Coord2d(UI.scale(c.mul(cmaps))).mul(prevScaleFactor).sub(new Coord2d(dloc.tc.div(scalef()))).add(new Coord2d(hsz));
                    Coord2d brDouble = new Coord2d(UI.scale(c.add(1, 1).mul(cmaps))).mul(prevScaleFactor).sub(new Coord2d(dloc.tc.div(scalef()))).add(new Coord2d(hsz));
                    
                    // Floor upper-left, ceil bottom-right to ensure tiles overlap slightly rather than gap
                    Coord ul = new Coord((int)Math.floor(ulDouble.x), (int)Math.floor(ulDouble.y));
                    Coord br = new Coord((int)Math.ceil(brDouble.x), (int)Math.ceil(brDouble.y));
                    Coord size = br.sub(ul);
                    
                    drawgrid(g, ul, disp, size);
                }
            }
            // Note: We keep previousLevelCache around for quick access when zooming back
        }
        
        // Draw current level
        if(display != null && dgext != null) {
            for(Coord c : dgext) {
                DisplayGrid disp = display[dgext.ri(c)];
                if(disp == null)
                    continue;
                    
                // Calculate position with exact tile boundaries to avoid gaps
                // Calculate the exact position of this grid corner and the next grid corner
                Coord2d ulDouble = new Coord2d(UI.scale(c.mul(cmaps))).mul(scaleFactor).sub(new Coord2d(dloc.tc.div(scalef()))).add(new Coord2d(hsz));
                Coord2d brDouble = new Coord2d(UI.scale(c.add(1, 1).mul(cmaps))).mul(scaleFactor).sub(new Coord2d(dloc.tc.div(scalef()))).add(new Coord2d(hsz));
                
                // Floor upper-left, ceil bottom-right to ensure tiles overlap slightly rather than gap
                Coord ul = new Coord((int)Math.floor(ulDouble.x), (int)Math.floor(ulDouble.y));
                Coord br = new Coord((int)Math.ceil(brDouble.x), (int)Math.ceil(brDouble.y));
                Coord size = br.sub(ul);
                
                drawgrid(g, ul, disp, size);
            }
        }
    }

    public void drawgrid(GOut g, Coord ul, DisplayGrid disp, Coord size) {
        try {
            Tex img = disp.img();
            if(img != null) {
                // Use the explicitly calculated size to avoid gaps
                g.image(img, ul, size);
            }
        } catch(Loading l) {
        }
    }
    
    // Compatibility method for old code paths
    public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
        try {
            Tex img = disp.img();
            if(img != null) {
                float scaleFactor = getScaleFactor();
                // Use double precision and round to avoid gaps between tiles
                Coord2d imgsizDouble = new Coord2d(UI.scale(img.sz())).mul(scaleFactor);
                Coord imgsz = new Coord((int)Math.round(imgsizDouble.x), (int)Math.round(imgsizDouble.y));
                g.image(img, ul, imgsz);
            }
        } catch(Loading l) {
        }
    }

    @Override
    public void drawmarkers(GOut g) {
        Coord hsz = sz.div(2);

        // Get marker search pattern from NMapWnd if we're inside one
        String markerSearchPattern = null;
        Widget parentWidget = this.parent;
        while(parentWidget != null) {
            if(parentWidget instanceof NMapWnd) {
                markerSearchPattern = ((NMapWnd) parentWidget).markerSearchPattern;
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

                // Then check marker search pattern filter
                if(markerSearchPattern != null && !markerSearchPattern.trim().isEmpty()) {
                    String markerName = mark.m.nm;
                    if(markerName == null) {
                        continue; // Hide markers with no name when searching
                    }
                    // Show only markers that contain the search pattern (case-insensitive)
                    if(!markerName.toLowerCase().contains(markerSearchPattern.toLowerCase())) {
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
            if(gui != null && gui.treeLocationService != null && showTreeIcons) {
                // Check if markers are hidden (respect "Hide Markers" button)
                MapWnd mapwnd = gui.mapfile;
                boolean markersHidden = (mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall));

                if(!markersHidden) {
                    // Get marker search pattern (if any) for filtering
                    String markerSearchPattern = null;
                    Widget parentWidget = this.parent;
                    while(parentWidget != null) {
                        if(parentWidget instanceof NMapWnd) {
                            markerSearchPattern = ((NMapWnd) parentWidget).markerSearchPattern;
                            break;
                        }
                        parentWidget = parentWidget.parent;
                    }

                    java.util.List<nurgling.TreeLocation> treeLocations = gui.treeLocationService.getTreeLocationsForSegment(sessloc.seg.id);
                    int threshold = UI.scale(10); // Screen pixels

                    for(nurgling.TreeLocation loc : treeLocations) {
                        // Apply marker search pattern filter
                        if(markerSearchPattern != null && !markerSearchPattern.trim().isEmpty()) {
                            String treeName = loc.getTreeName();
                            if(treeName == null || !treeName.toLowerCase().contains(markerSearchPattern.toLowerCase())) {
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
            if(gui != null && gui.fishLocationService != null && showFishIcons) {
                // Check if markers are hidden (respect "Hide Markers" button)
                MapWnd mapwnd = gui.mapfile;
                boolean markersHidden = (mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall));

                if(!markersHidden) {
                    // Get marker search pattern from NMapWnd if we're inside one
                    String markerSearchPattern = null;
                    Widget parentWidget = this.parent;
                    while(parentWidget != null) {
                        if(parentWidget instanceof NMapWnd) {
                            markerSearchPattern = ((NMapWnd) parentWidget).markerSearchPattern;
                            break;
                        }
                        parentWidget = parentWidget.parent;
                    }

                    java.util.List<nurgling.FishLocation> locations = gui.fishLocationService.getFishLocationsForSegment(sessloc.seg.id);
                    int threshold = UI.scale(10); // Screen pixels

                    for(nurgling.FishLocation loc : locations) {
                        // Apply marker search pattern filter
                        if(markerSearchPattern != null && !markerSearchPattern.trim().isEmpty()) {
                            String fishName = loc.getFishName();
                            if(fishName == null) {
                                continue; // Skip fish with no name when searching
                            }
                            // Show only fish that contain the marker search pattern (case-insensitive)
                            if(!fishName.toLowerCase().contains(markerSearchPattern.toLowerCase())) {
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

        // Check if fish icons are hidden by checkbox
        if(!showFishIcons) return;

        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.fishLocationService == null) return;

        // Check if markers are hidden (respect "Hide Markers" button)
        MapWnd mapwnd = gui.mapfile;
        if(mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall)) {
            return; // Don't draw fish locations when markers are hidden
        }

        // Get marker search pattern from NMapWnd if we're inside one
        String markerSearchPattern = null;
        Widget parentWidget = this.parent;
        while(parentWidget != null) {
            if(parentWidget instanceof NMapWnd) {
                markerSearchPattern = ((NMapWnd) parentWidget).markerSearchPattern;
                break;
            }
            parentWidget = parentWidget.parent;
        }

        // Use sessloc.seg.id like waypoints and markers do
        java.util.List<nurgling.FishLocation> fishLocations = gui.fishLocationService.getFishLocationsForSegment(sessloc.seg.id);

        Coord hsz = sz.div(2);

        for(nurgling.FishLocation fishLoc : fishLocations) {
            // Apply marker search pattern filter to fish names
            if(markerSearchPattern != null && !markerSearchPattern.trim().isEmpty()) {
                String fishName = fishLoc.getFishName();
                if(fishName == null) {
                    continue; // Hide fish with no name when searching
                }
                // Show only fish that contain the marker search pattern (case-insensitive)
                if(!fishName.toLowerCase().contains(markerSearchPattern.toLowerCase())) {
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

        // Check if tree icons are hidden by checkbox
        if(!showTreeIcons) return;

        NGameUI gui = NUtils.getGameUI();
        if(gui == null || gui.treeLocationService == null) return;

        // Check if markers are hidden (respect "Hide Markers" button)
        MapWnd mapwnd = gui.mapfile;
        if(mapwnd != null && Utils.eq(mapwnd.markcfg, MapWnd.MarkerConfig.hideall)) {
            return; // Don't draw tree locations when markers are hidden
        }

        // Get marker search pattern from NMapWnd if we're inside one
        String markerSearchPattern = null;
        Widget parentWidget = this.parent;
        while(parentWidget != null) {
            if(parentWidget instanceof NMapWnd) {
                markerSearchPattern = ((NMapWnd) parentWidget).markerSearchPattern;
                break;
            }
            parentWidget = parentWidget.parent;
        }

        // Use sessloc.seg.id like waypoints and markers do
        java.util.List<nurgling.TreeLocation> treeLocations = gui.treeLocationService.getTreeLocationsForSegment(sessloc.seg.id);

        Coord hsz = sz.div(2);

        for(nurgling.TreeLocation treeLoc : treeLocations) {
            // Apply marker search pattern filter to tree names
            if(markerSearchPattern != null && !markerSearchPattern.trim().isEmpty()) {
                String treeName = treeLoc.getTreeName();
                if(treeName == null) {
                    continue; // Hide trees with no name when searching
                }
                // Show only trees that contain the marker search pattern (case-insensitive)
                if(!treeName.toLowerCase().contains(markerSearchPattern.toLowerCase())) {
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
        // Check if we're inside an NMapWnd and if it has an active marker search pattern
        Widget parent = this.parent;
        while(parent != null) {
            if(parent instanceof NMapWnd) {
                NMapWnd mapWnd = (NMapWnd) parent;
                String markerSearchPattern = mapWnd.markerSearchPattern;

                // If marker search pattern is active, filter by marker name
                if(markerSearchPattern != null && !markerSearchPattern.trim().isEmpty()) {
                    String markerName = mark.m.nm;
                    if(markerName == null) {
                        return true; // Hide markers with no name when searching
                    }
                    // Show only markers that contain the search pattern (case-insensitive)
                    if(!markerName.toLowerCase().contains(markerSearchPattern.toLowerCase())) {
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
                        NGameUI gui = NUtils.getGameUI();
                        if(gui != null && gui.map instanceof NMapView) {
                            NMapView mapView = (NMapView) gui.map;

                            // Toggle selection based on coordinates (works for markers and pointers)
                            if(mapView.selectedMarkerTileCoords != null &&
                               mapView.selectedMarkerTileCoords.equals(mark.m.tc)) {
                                // Deselect - same location clicked again
                                mapView.setSelectedMarker(null, null);
                            } else {
                                // Select this marker
                                mapView.setSelectedMarker(mark, mark.m.tc);
                            }
                        }
                        return true;
                    }
                }
            }
        }

        // Handle right-click release on tree location - open details window
        if(ev.b == 3 && dloc != null && sessloc != null && showTreeIcons) { // Button 3 is right-clicked
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
        if(ev.b == 3 && dloc != null && sessloc != null && showFishIcons) { // Button 3 is right-clicked
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

    /**
     * Draw tile highlight overlay on top of map tiles
     */
    private void drawTileHighlightOverlay(GOut g) {
        // Draw tile highlight overlay if any tiles are highlighted
        if(!TileHighlight.getHighlighted().isEmpty() && display != null && dgext != null && dloc != null) {
            Coord hsz = sz.div(2);
            int dataLevel = getDataLevel();
            float scaleFactor = getScaleFactor();
            
            // Calculate dynamic alpha for pulsating effect
            int alpha = (int)(100 + 155 * Math.sin(Math.PI * ((System.currentTimeMillis() % 1000) / 1000.0)));
            
            for(Coord c : dgext) {
                DisplayGrid disp = display[dgext.ri(c)];
                if(disp == null)
                    continue;
                
                try {
                    Tex overlayImg = getTileHighlightOverlay(disp);
                    if(overlayImg != null) {
                        // Use round for consistent alignment without gaps or overlaps
                        Coord2d ulDouble = new Coord2d(UI.scale(c.mul(cmaps))).mul(scaleFactor).sub(new Coord2d(dloc.tc.div(scalef()))).add(new Coord2d(hsz));
                        Coord2d brDouble = new Coord2d(UI.scale(c.add(1, 1).mul(cmaps))).mul(scaleFactor).sub(new Coord2d(dloc.tc.div(scalef()))).add(new Coord2d(hsz));
                        Coord ul = new Coord((int)Math.round(ulDouble.x), (int)Math.round(ulDouble.y));
                        Coord br = new Coord((int)Math.round(brDouble.x), (int)Math.round(brDouble.y));
                        Coord imgsz = br.sub(ul);
                        
                        g.chcolor(255, 255, 255, alpha);
                        g.image(overlayImg, ul, imgsz);
                        g.chcolor();
                    }
                } catch(Exception e) {
                    // Ignore overlay rendering errors
                }
            }
        }
    }

    /**
     * Cache for tile highlight overlays with version tracking
     */
    private static class TileHighlightCache {
        Tex img;
        long seq;
        MapFile.DataGrid grid;
    }
    
    private final java.util.Map<DisplayGrid, TileHighlightCache> tileHighlightCache = new java.util.HashMap<>();

    /**
     * Get tile highlight overlay for a display grid with caching
     */
    private Tex getTileHighlightOverlay(DisplayGrid disp) {
        TileHighlightCache cache = tileHighlightCache.get(disp);
        MapFile.DataGrid grid = (MapFile.DataGrid) disp.gref.get();
        
        // Check if cache is valid
        if(cache != null && cache.grid == grid && cache.seq == TileHighlight.seq) {
            return cache.img;
        }
        
        // Generate new overlay
        try {
            java.awt.image.BufferedImage overlayBuf = TileHighlight.olrender(grid);
            Tex overlayTex = new TexI(overlayBuf);
            
            // Update cache
            cache = new TileHighlightCache();
            cache.img = overlayTex;
            cache.seq = TileHighlight.seq;
            cache.grid = grid;
            tileHighlightCache.put(disp, cache);
            
            return overlayTex;
        } catch(Exception e) {
            return null;
        }
    }
}
