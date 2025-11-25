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
 */
public class MinimapExploredAreaRenderer {
    
    /**
     * Main rendering method called from MiniMap's drawparts()
     * Renders explored area overlay on top of map tiles.
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

            int totalGrids = 0;
            int gridsWithData = 0;
            
            // At dataLevel N, each display grid represents (2^N) x (2^N) base grids
            // Instead of aggregating, render each base grid individually (like claims do)
            int gridScale = (1 << dataLevel);
            
            // Iterate through all display grids
            for (Coord gc : dgext) {
                MiniMap.DisplayGrid disp = display[dgext.ri(gc)];
                if (disp == null) {
                    continue;
                }
                
                totalGrids++;
                
                // Calculate which base grids this display grid covers
                Coord baseGridStart = disp.sc.mul(gridScale);
                Coord baseGridEnd = baseGridStart.add(gridScale, gridScale);
                
                // Render each base grid separately
                for (int bgy = baseGridStart.y; bgy < baseGridEnd.y; bgy++) {
                    for (int bgx = baseGridStart.x; bgx < baseGridEnd.x; bgx++) {
                        Coord baseGridCoord = new Coord(bgx, bgy);
                        boolean[] baseMask = exploredArea.getExploredMaskForGrid(baseGridCoord, map.sessloc.seg.id, 0);
                        
                        if (baseMask == null) {
                            continue;
                        }
                        
                        // Check if any tiles are explored
                        boolean hasExplored = false;
                        for (boolean b : baseMask) {
                            if (b) {
                                hasExplored = true;
                                break;
                            }
                        }
                        
                        if (!hasExplored) {
                            continue;
                        }
                        
                        gridsWithData++;
                        
                        try {
                            // Get overlay texture for this base grid
                            Tex overlayImg = getExploredOverlay(baseGridCoord, map.sessloc.seg.id, baseMask, dataLevel);
                            
                            if (overlayImg != null) {
                                // Calculate screen position for this base grid
                                // Base grids are ALWAYS at level 0, so gridTileSize = 100
                                int gridTileSize = MCache.cmaps.x; // Always 100 for base level
                                
                                // This base grid's tile coordinate in the segment
                                Coord baseTileUL = new Coord(bgx * gridTileSize, bgy * gridTileSize);
                                
                                // Convert to screen coordinates
                                // Use round for consistent alignment without gaps or overlaps
                                Coord baseTileBR = new Coord((bgx + 1) * gridTileSize, (bgy + 1) * gridTileSize);
                                Coord2d screenULDouble = new Coord2d(UI.scale(baseTileUL)).mul(scaleFactor).sub(new Coord2d(map.dloc.tc.div(map.scalef()))).add(new Coord2d(hsz));
                                Coord2d screenBRDouble = new Coord2d(UI.scale(baseTileBR)).mul(scaleFactor).sub(new Coord2d(map.dloc.tc.div(map.scalef()))).add(new Coord2d(hsz));
                                Coord screenUL = new Coord((int)Math.round(screenULDouble.x), (int)Math.round(screenULDouble.y));
                                Coord screenBR = new Coord((int)Math.round(screenBRDouble.x), (int)Math.round(screenBRDouble.y));
                                
                                // Image size calculated from exact boundaries
                                Coord imgsz = screenBR.sub(screenUL);
                                
                                // Draw overlay
                                g.chcolor(NMiniMap.VIEW_EXPLORED_COLOR);
                                g.image(overlayImg, screenUL, imgsz);
                                g.chcolor();
                            }
                        } catch (Exception e) {
                            // Ignore rendering errors
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle errors
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
            BufferedImage overlayBuf = renderOverlayImage(mask);
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
     * Render overlay image from boolean mask
     * Creates a semi-transparent texture with explored tiles colored
     */
    private static BufferedImage renderOverlayImage(boolean[] mask) {
        WritableRaster buf = PUtils.imgraster(MCache.cmaps);
        Color col = NMiniMap.VIEW_EXPLORED_COLOR;
        
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
