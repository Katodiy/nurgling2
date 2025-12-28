package nurgling.overlays.map;

import haven.*;
import nurgling.NMapView;
import nurgling.navigation.ChunkNavData;
import nurgling.navigation.ChunkNavGraph;
import nurgling.navigation.ChunkNavManager;
import nurgling.widgets.NMiniMap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders ChunkNav exploration overlay on the minimap.
 * Shows 5x5 = 25 sections per chunk: green if fully observed, red if any tile unobserved.
 *
 * Simple approach: iterate MCache grids directly, use p2c() for screen positions.
 */
public class MinimapChunkNavRenderer {

    // Section configuration - must match ChunkNavData
    private static final int TOTAL_SECTIONS = ChunkNavData.TOTAL_SECTIONS; // 25
    private static final int SECTIONS_PER_SIDE = ChunkNavData.SECTIONS_PER_SIDE; // 5
    private static final int TILES_PER_SECTION_SIDE = ChunkNavData.TILES_PER_SECTION_SIDE; // 20

    // Colors for section states
    private static final Color OBSERVED_COLOR = new Color(0, 200, 0, 100);    // Green, semi-transparent
    private static final Color UNOBSERVED_COLOR = new Color(200, 0, 0, 100);  // Red, semi-transparent

    // Cache for overlay textures (keyed by grid ID)
    private static final Map<Long, OverlayCache> textureCache = new HashMap<>();

    private static class OverlayCache {
        Tex texture;
        boolean[] sectionStates;
    }

    /**
     * Main rendering method called from NMiniMap.drawparts()
     */
    public static void renderChunkNav(NMiniMap map, GOut g) {
        if (map.ui == null || map.ui.gui == null || map.ui.gui.map == null) {
            return;
        }

        if (!(map.ui.gui.map instanceof NMapView)) {
            return;
        }

        NMapView mapView = (NMapView) map.ui.gui.map;
        ChunkNavManager chunkNav = mapView.getChunkNavManager();

        if (chunkNav == null || !chunkNav.isInitialized()) {
            return;
        }

        ChunkNavGraph graph = chunkNav.getGraph();
        if (graph == null) {
            return;
        }

        if (map.dloc == null || map.sessloc == null) {
            return;
        }

        // Only render in player's segment
        if (map.curloc != null && map.sessloc.seg.id != map.curloc.seg.id) {
            return;
        }

        MCache mcache = null;
        try {
            mcache = map.ui.sess.glob.map;
        } catch (Exception e) {
            return;
        }
        if (mcache == null) return;

        try {
            // Iterate MCache grids directly
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid == null || grid.ul == null) {
                        continue;
                    }

                    // Look up ChunkNav data by grid ID
                    ChunkNavData chunk = graph.getChunk(grid.id);

                    // Get section states
                    boolean[] sectionStates = new boolean[TOTAL_SECTIONS];
                    boolean hasData = (chunk != null);
                    if (hasData) {
                        for (int i = 0; i < TOTAL_SECTIONS; i++) {
                            sectionStates[i] = chunk.isSectionFullyObserved(i);
                        }
                    }

                    // Skip grids with no ChunkNav data
                    if (!hasData) {
                        continue;
                    }

                    // Get overlay texture
                    Tex overlayTex = getOverlayTexture(grid.id, sectionStates);
                    if (overlayTex == null) {
                        continue;
                    }

                    // Convert grid corners to screen positions using p2c()
                    // grid.ul is upper-left tile, grid covers cmaps tiles
                    Coord2d worldUL = new Coord2d(grid.ul).mul(MCache.tilesz);
                    Coord2d worldBR = new Coord2d(grid.ul.add(MCache.cmaps)).mul(MCache.tilesz);

                    Coord screenUL = map.p2c(worldUL);
                    Coord screenBR = map.p2c(worldBR);

                    if (screenUL == null || screenBR == null) {
                        continue;
                    }

                    Coord imgsz = screenBR.sub(screenUL);

                    // Draw the overlay
                    g.image(overlayTex, screenUL, imgsz);
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
    }

    /**
     * Get or create overlay texture for a grid.
     */
    private static Tex getOverlayTexture(long gridId, boolean[] sectionStates) {
        OverlayCache cache = textureCache.get(gridId);

        // Check if cache is valid
        if (cache != null && statesMatch(cache.sectionStates, sectionStates)) {
            return cache.texture;
        }

        // Generate new overlay
        BufferedImage overlayBuf = renderSectionOverlay(sectionStates);
        Tex overlayTex = new TexI(overlayBuf);

        // Update cache
        cache = new OverlayCache();
        cache.texture = overlayTex;
        cache.sectionStates = sectionStates.clone();
        textureCache.put(gridId, cache);

        return overlayTex;
    }

    private static boolean statesMatch(boolean[] a, boolean[] b) {
        if (a == null || b == null) return a == b;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    /**
     * Render the section overlay image (5x5 grid = 25 sections).
     */
    private static BufferedImage renderSectionOverlay(boolean[] sectionStates) {
        WritableRaster buf = PUtils.imgraster(MCache.cmaps);

        int width = MCache.cmaps.x;  // 100
        int height = MCache.cmaps.y; // 100

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Determine which section this pixel is in (5x5 grid)
                int sx = x / TILES_PER_SECTION_SIDE;  // 0-4
                int sy = y / TILES_PER_SECTION_SIDE;  // 0-4
                int section = sy * SECTIONS_PER_SIDE + sx;  // 0-24

                Color col = sectionStates[section] ? OBSERVED_COLOR : UNOBSERVED_COLOR;

                buf.setSample(x, y, 0, col.getRed());
                buf.setSample(x, y, 1, col.getGreen());
                buf.setSample(x, y, 2, col.getBlue());
                buf.setSample(x, y, 3, col.getAlpha());
            }
        }

        return PUtils.rasterimg(buf);
    }

    /**
     * Clear texture cache. Call when switching genus.
     */
    public static void clearCache() {
        textureCache.clear();
    }
}
