package nurgling.overlays.map;

import haven.*;
import nurgling.NConfig;
import nurgling.tools.ExploredArea;
import nurgling.widgets.NMiniMap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Renders explored area overlay on the minimap.
 * Uses ExploredArea rectangle data to generate overlay masks for each DisplayGrid.
 * Similar to MinimapClaimRenderer but for explored (visited) areas.
 * 
 * Supports rendering both main explored area and session layer.
 */
public class MinimapExploredAreaRenderer {
    
    /**
     * Main rendering method called from MiniMap's drawparts()
     * Renders explored area overlay on top of map tiles.
     * Also renders session layer if active.
     *
     * @param map The minimap instance
     * @param g   Graphics context
     */
    public static void renderExploredArea(MiniMap map, GOut g) {
        // Check if feature is enabled
        Object val = NConfig.get(NConfig.Key.exploredAreaEnable);
        if (!(val instanceof Boolean) || !(Boolean) val) {
            return;
        }
        
        if (map.ui == null || map.ui.gui == null || map.ui.gui.map == null) {
            return;
        }

        MapView mv = map.ui.gui.map;
        if (mv == null || map.dloc == null || map.sessloc == null) {
            return;
        }

        // Must be NMiniMap to access display grids and exploredArea
        if (!(map instanceof NMiniMap)) {
            return;
        }
        NMiniMap nmap = (NMiniMap) map;

        // Get the display grid array and extent
        MiniMap.DisplayGrid[] display = nmap.getDisplay();
        Area dgext = nmap.getDgext();

        if (display == null || dgext == null) {
            return;
        }

        try {
            Coord hsz = map.sz.div(2);
            int dataLevel = nmap.getDataLevelPublic();
            float scaleFactor = nmap.getCurrentScale();

            // Get explored area data
            ExploredArea exploredArea = nmap.exploredArea;
            if (exploredArea == null) {
                return;
            }

            // Check if player is in current segment
            boolean playerSegment = (map.sessloc != null) && 
                                   ((map.curloc == null) || (map.sessloc.seg.id == map.curloc.seg.id));
            if (!playerSegment) {
                return; // Only show explored area in current segment
            }

            // At dataLevel N, each display grid represents (2^N) x (2^N) base grids
            int gridScale = (1 << dataLevel);
            
            // Iterate through all display grids
            for (Coord gc : dgext) {
                MiniMap.DisplayGrid disp = display[dgext.ri(gc)];
                if (disp == null) {
                    continue;
                }
                
                // Calculate which base grids this display grid covers
                Coord baseGridStart = disp.sc.mul(gridScale);
                Coord baseGridEnd = baseGridStart.add(gridScale, gridScale);
                
                // Render each base grid separately
                for (int bgy = baseGridStart.y; bgy < baseGridEnd.y; bgy++) {
                    for (int bgx = baseGridStart.x; bgx < baseGridEnd.x; bgx++) {
                        Coord baseGridCoord = new Coord(bgx, bgy);
                        
                        // Render main explored area
                        boolean[] baseMask = exploredArea.getExploredMaskForGrid(baseGridCoord, map.sessloc.seg.id, 0);
                        if (baseMask != null && hasAnyTrue(baseMask)) {
                            renderGridOverlay(g, map, nmap, baseGridCoord, baseMask, 
                                NMiniMap.VIEW_EXPLORED_COLOR, hsz, scaleFactor, dataLevel, false);
                        }
                        
                        // Render session layer on top if active
                        boolean[] sessionMask = exploredArea.getSessionMaskForGrid(baseGridCoord, map.sessloc.seg.id);
                        if (sessionMask != null && hasAnyTrue(sessionMask)) {
                            renderGridOverlay(g, map, nmap, baseGridCoord, sessionMask,
                                NMiniMap.VIEW_SESSION_COLOR, hsz, scaleFactor, dataLevel, true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
    }
    
    /**
     * Check if mask has any true values
     */
    private static boolean hasAnyTrue(boolean[] mask) {
        for (boolean b : mask) {
            if (b) return true;
        }
        return false;
    }
    
    /**
     * Render a single grid's overlay
     */
    private static void renderGridOverlay(GOut g, MiniMap map, NMiniMap nmap, 
            Coord baseGridCoord, boolean[] mask, Color color,
            Coord hsz, float scaleFactor, int dataLevel, boolean isSession) {
        try {
            // Get overlay texture for this base grid
            Tex overlayImg = isSession 
                ? getSessionOverlay(baseGridCoord, map.sessloc.seg.id, mask, dataLevel)
                : getExploredOverlay(baseGridCoord, map.sessloc.seg.id, mask, dataLevel);
            
            if (overlayImg != null) {
                // Calculate screen position for this base grid
                int gridTileSize = MCache.cmaps.x; // Always 100 for base level
                int bgx = baseGridCoord.x;
                int bgy = baseGridCoord.y;
                
                // This base grid's tile coordinate in the segment
                Coord baseTileUL = new Coord(bgx * gridTileSize, bgy * gridTileSize);
                
                // Convert to screen coordinates
                Coord baseTileBR = new Coord((bgx + 1) * gridTileSize, (bgy + 1) * gridTileSize);
                Coord2d screenULDouble = new Coord2d(UI.scale(baseTileUL)).mul(scaleFactor).sub(new Coord2d(map.dloc.tc.div(map.scalef()))).add(new Coord2d(hsz));
                Coord2d screenBRDouble = new Coord2d(UI.scale(baseTileBR)).mul(scaleFactor).sub(new Coord2d(map.dloc.tc.div(map.scalef()))).add(new Coord2d(hsz));
                Coord screenUL = new Coord((int)Math.round(screenULDouble.x), (int)Math.round(screenULDouble.y));
                Coord screenBR = new Coord((int)Math.round(screenBRDouble.x), (int)Math.round(screenBRDouble.y));
                
                // Image size calculated from exact boundaries
                Coord imgsz = screenBR.sub(screenUL);
                
                // Draw overlay
                g.chcolor(color);
                g.image(overlayImg, screenUL, imgsz);
                g.chcolor();
            }
        } catch (Exception e) {
            // Ignore rendering errors
        }
    }

    /**
     * Cache for explored area overlays with version tracking
     */
    private static class ExploredOverlayCache {
        Tex img;
        long seq;
        int dataLevel;
    }
    
    private static class CacheKey {
        final Coord baseGridCoord;
        final long segmentId;
        
        CacheKey(Coord baseGridCoord, long segmentId) {
            this.baseGridCoord = baseGridCoord;
            this.segmentId = segmentId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey k = (CacheKey) o;
            return segmentId == k.segmentId && baseGridCoord.equals(k.baseGridCoord);
        }
        
        @Override
        public int hashCode() {
            return baseGridCoord.hashCode() * 31 + Long.hashCode(segmentId);
        }
    }
    
    private static final java.util.Map<CacheKey, ExploredOverlayCache> overlayCache = new java.util.HashMap<>();
    private static final java.util.Map<CacheKey, ExploredOverlayCache> sessionOverlayCache = new java.util.HashMap<>();

    /**
     * Get explored area overlay for a base grid with caching
     */
    private static Tex getExploredOverlay(Coord baseGridCoord, long segmentId, boolean[] mask, int dataLevel) {
        CacheKey key = new CacheKey(baseGridCoord, segmentId);
        ExploredOverlayCache cache = overlayCache.get(key);
        
        // Check if cache is valid
        if (cache != null && cache.seq == ExploredArea.seq && cache.dataLevel == dataLevel) {
            return cache.img;
        }
        
        // Generate new overlay
        try {
            BufferedImage overlayBuf = renderOverlayImage(mask, NMiniMap.VIEW_EXPLORED_COLOR);
            Tex overlayTex = new TexI(overlayBuf);
            
            // Update cache
            cache = new ExploredOverlayCache();
            cache.img = overlayTex;
            cache.seq = ExploredArea.seq;
            cache.dataLevel = dataLevel;
            overlayCache.put(key, cache);
            
            return overlayTex;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get session overlay for a base grid with caching
     */
    private static Tex getSessionOverlay(Coord baseGridCoord, long segmentId, boolean[] mask, int dataLevel) {
        CacheKey key = new CacheKey(baseGridCoord, segmentId);
        ExploredOverlayCache cache = sessionOverlayCache.get(key);
        
        // Check if cache is valid
        if (cache != null && cache.seq == ExploredArea.sessionSeq && cache.dataLevel == dataLevel) {
            return cache.img;
        }
        
        // Generate new overlay
        try {
            BufferedImage overlayBuf = renderOverlayImage(mask, NMiniMap.VIEW_SESSION_COLOR);
            Tex overlayTex = new TexI(overlayBuf);
            
            // Update cache
            cache = new ExploredOverlayCache();
            cache.img = overlayTex;
            cache.seq = ExploredArea.sessionSeq;
            cache.dataLevel = dataLevel;
            sessionOverlayCache.put(key, cache);
            
            return overlayTex;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Render overlay image from boolean mask
     * Creates a semi-transparent texture with explored tiles colored
     * 
     * @param mask boolean array indicating explored tiles
     * @param col the color to use for the overlay
     */
    private static BufferedImage renderOverlayImage(boolean[] mask, Color col) {
        WritableRaster buf = PUtils.imgraster(MCache.cmaps);
        
        int width = MCache.cmaps.x;
        int height = MCache.cmaps.y;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                if (mask[idx]) {
                    // Set pixel with overlay color
                    buf.setSample(x, y, 0, col.getRed());
                    buf.setSample(x, y, 1, col.getGreen());
                    buf.setSample(x, y, 2, col.getBlue());
                    buf.setSample(x, y, 3, col.getAlpha());
                }
            }
        }
        
        return PUtils.rasterimg(buf);
    }
}
