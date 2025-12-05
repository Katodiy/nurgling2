package haven;

import nurgling.NUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.*;

import static haven.MCache.cmaps;

/**
 * Tile highlighting system using overlay mechanism (ported from Hurricane).
 * Provides semi-transparent colored overlay on top of map tiles instead of replacing tile textures.
 */
public class TileHighlight {
    private static final Set<String> highlight = new HashSet<>();
    public static final String TAG = "tileHighlight";
    public static volatile long seq = 0;
    
    /**
     * Check if a tile resource name is currently highlighted
     */
    public static boolean isHighlighted(String name) {
        synchronized (highlight) {
            return highlight.contains(name);
        }
    }
    
    /**
     * Toggle highlighting for a tile resource name
     */
    public static void toggle(String name) {
        synchronized (highlight) {
            if(highlight.contains(name)) {
                unhighlight(name);
            } else {
                highlight(name);
            }
        }
    }
    
    /**
     * Enable highlighting for a tile resource name
     */
    public static void highlight(String name) {
        synchronized (highlight) {
            if(highlight.add(name)) {
                seq++;
            }
        }
    }
    
    /**
     * Disable highlighting for a tile resource name
     */
    public static void unhighlight(String name) {
        synchronized (highlight) {
            if(highlight.remove(name)) {
                seq++;
            }
        }
    }
    
    /**
     * Clear all highlights
     */
    public static void clear() {
        synchronized (highlight) {
            if(!highlight.isEmpty()) {
                highlight.clear();
                seq++;
            }
        }
    }
    
    /**
     * Set highlighted tiles from a collection of resource names
     */
    public static void setHighlighted(Collection<String> names) {
        synchronized (highlight) {
            highlight.clear();
            highlight.addAll(names);
            seq++;
        }
    }
    
    /**
     * Get all currently highlighted tile names
     */
    public static Set<String> getHighlighted() {
        synchronized (highlight) {
            return new HashSet<>(highlight);
        }
    }
    
    /**
     * Render overlay texture for a map grid based on current highlights
     * @param grid The map grid to render overlay for
     * @return BufferedImage with semi-transparent colored overlay
     */
    public static BufferedImage olrender(MapFile.DataGrid grid) {
        TileHighlightOverlay ol = new TileHighlightOverlay(grid);
        WritableRaster buf = PUtils.imgraster(cmaps);
        Color col = ol.color();
        if(col != null) {
            Coord c = new Coord();
            for (c.y = 0; c.y < cmaps.y; c.y++) {
                for (c.x = 0; c.x < cmaps.x; c.x++) {
                    if(ol.get(c)) {
                        // Blend overlay color with existing pixel using alpha
                        buf.setSample(c.x, c.y, 0, ((col.getRed() * col.getAlpha()) + (buf.getSample(c.x, c.y, 0) * (255 - col.getAlpha()))) / 255);
                        buf.setSample(c.x, c.y, 1, ((col.getGreen() * col.getAlpha()) + (buf.getSample(c.x, c.y, 1) * (255 - col.getAlpha()))) / 255);
                        buf.setSample(c.x, c.y, 2, ((col.getBlue() * col.getAlpha()) + (buf.getSample(c.x, c.y, 2) * (255 - col.getAlpha()))) / 255);
                        buf.setSample(c.x, c.y, 3, Math.max(buf.getSample(c.x, c.y, 3), col.getAlpha()));
                    }
                }
            }
        }
        return (PUtils.rasterimg(buf));
    }
    
    /**
     * Overlay mask that determines which tiles should be highlighted
     */
    public static class TileHighlightOverlay {
        private final boolean[] ol;
        
        public TileHighlightOverlay(MapFile.DataGrid g) {
            this.ol = new boolean[cmaps.x * cmaps.y];
            fill(g);
        }
        
        /**
         * Fill overlay mask based on current highlights
         */
        private void fill(MapFile.DataGrid grid) {
            if(grid == null) {return;}
            Coord c = new Coord(0, 0);
            for (c.x = 0; c.x < cmaps.x; c.x++) {
                for (c.y = 0; c.y < cmaps.y; c.y++) {
                    int tile = grid.gettile(c);
                    MapFile.TileInfo tileset = grid.tilesets[tile];
                    boolean v = isHighlighted(tileset.res.name);
                    set(c, v);
                    // Add 1-tile border around highlighted tiles for better visibility
                    if(v) { 
                        setn(c, true);
                    }
                }
            }
        }
        
        public boolean get(Coord c) {
            return (ol[c.x + (c.y * cmaps.x)]);
        }
        
        public void set(Coord c, boolean v) {
            ol[c.x + (c.y * cmaps.x)] = v;
        }
        
        public void set(int x, int y, boolean v) {
            if(x >= 0 && y >= 0 && x < cmaps.x && y < cmaps.y) {
                ol[x + (y * cmaps.x)] = v;
            }
        }
        
        /**
         * Set neighbors around a coordinate (8 surrounding tiles)
         */
        public void setn(Coord c, boolean v) {
            set(c.x - 1, c.y - 1, v);
            set(c.x - 1, c.y + 1, v);
            set(c.x + 1, c.y - 1, v);
            set(c.x + 1, c.y + 1, v);
            set(c.x, c.y - 1, v);
            set(c.x, c.y + 1, v);
            set(c.x - 1, c.y, v);
            set(c.x + 1, c.y, v);
        }
        
        /**
         * Overlay color (cyan/light blue)
         * Note: Alpha animation is applied during rendering in NMiniMap.drawTileHighlightOverlay()
         */
        public Color color() {
            // Cyan/light blue color with medium alpha (will be modulated during rendering)
            return new Color(0, 255, 255, 180);
        }
    }
}
