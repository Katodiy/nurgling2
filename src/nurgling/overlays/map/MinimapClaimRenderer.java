package nurgling.overlays.map;

import haven.*;
import nurgling.NMapView;

import java.awt.Color;
import java.util.Collection;

/**
 * Renders personal claims, village claims, and realms on the minimap.
 * Synchronizes with the 3D map overlay visibility toggles.
 */
public class MinimapClaimRenderer {
    // Claim colors (semi-transparent to avoid obscuring terrain)
    private static final Color CPLOT_COLOR = new Color(0, 255, 0, 60);    // Personal - Green
    private static final Color VLG_COLOR = new Color(255, 128, 255, 60);   // Village - Pink
    private static final Color PROV_COLOR = new Color(255, 0, 255, 60);    // Realm - Magenta

    // Outline colors for better visibility
    private static final Color CPLOT_BORDER = new Color(0, 255, 0, 120);
    private static final Color VLG_BORDER = new Color(255, 128, 255, 120);
    private static final Color PROV_BORDER = new Color(255, 0, 255, 120);

    /**
     * Main rendering method called from MiniMap's drawparts()
     *
     * @param map The minimap instance
     * @param g   Graphics context
     */
    public static void renderClaims(MiniMap map, GOut g) {
        if (map.ui == null || map.ui.gui == null || map.ui.gui.map == null) {
            return;
        }

        MapView mv = map.ui.gui.map;
        if (mv == null || map.dloc == null || map.sessloc == null || map.curloc == null) {
            return;
        }

        // Must be NMapView to access loaded overlays
        if (!(mv instanceof NMapView)) {
            return;
        }

        try {
            // Get player's position in world-relative tile coordinates (NOT segment-absolute)
            Gob player = mv.player();
            if (player == null) {
                return;
            }

            Coord playerTileWorld = player.rc.div(MCache.tilesz).floor();

            // Query area around player in world-relative tile coordinates
            // Smaller radius for better performance - claims are large, and we don't need to render far away
            final int QUERY_RADIUS = 100; // tiles in each direction
            Area queryArea = Area.sized(
                playerTileWorld.sub(QUERY_RADIUS, QUERY_RADIUS),
                new Coord(QUERY_RADIUS * 2, QUERY_RADIUS * 2)
            );

            // Try to get overlays for this area
            Collection<MCache.OverlayInfo> overlays;
            try {
                overlays = map.ui.sess.glob.map.getols(queryArea);
            } catch (Loading e) {
                // Data not loaded yet, skip this frame
                return;
            }

            if (overlays.isEmpty()) {
                return;
            }

            // Render each overlay
            for (MCache.OverlayInfo info : overlays) {
                // Check each tag in this overlay
                for (String tag : info.tags()) {
                    // Check if this overlay type is enabled in the 3D map
                    if (!mv.visol(tag)) {
                        continue;
                    }

                    // Get color for this claim type
                    Color fillColor = getColorForTag(tag);
                    Color borderColor = getBorderColorForTag(tag);

                    if (fillColor != null) {
                        renderOverlay(map, g, info, queryArea, fillColor, borderColor);
                    }
                }
            }

        } catch (Exception e) {
            // Silently handle errors
        }
    }

    /**
     * Renders a single overlay on the minimap
     */
    private static void renderOverlay(MiniMap map, GOut g,
                                      MCache.OverlayInfo info,
                                      Area area,
                                      Color fillColor,
                                      Color borderColor) {
        try {
            // Get the boolean mask for this overlay
            boolean[] mask = new boolean[area.area()];
            map.ui.sess.glob.map.getol(info, area, mask);

            // Check if we're viewing the current segment
            if (map.sessloc == null || map.curloc == null) {
                return;
            }

            // Only render if in the same segment
            if (map.curloc.seg.id != map.sessloc.seg.id) {
                return;
            }

            // Merge adjacent tiles into large rectangles for better performance
            int width = area.br.x - area.ul.x;
            int height = area.br.y - area.ul.y;

            // Track which tiles we've already rendered
            boolean[] rendered = new boolean[mask.length];

            // Scan for rectangles
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = y * width + x;

                    // Skip if not claimed or already rendered
                    if (!mask[idx] || rendered[idx]) {
                        continue;
                    }

                    // Find the width of the rectangle (horizontal extent)
                    int rectWidth = 0;
                    while (x + rectWidth < width &&
                           mask[y * width + (x + rectWidth)] &&
                           !rendered[y * width + (x + rectWidth)]) {
                        rectWidth++;
                    }

                    // Find the height of the rectangle (vertical extent)
                    // Check how many rows below have the same horizontal span
                    int rectHeight = 1;
                    boolean canExtend = true;
                    while (canExtend && y + rectHeight < height) {
                        // Check if this entire row matches the width
                        for (int dx = 0; dx < rectWidth; dx++) {
                            int checkIdx = (y + rectHeight) * width + (x + dx);
                            if (!mask[checkIdx] || rendered[checkIdx]) {
                                canExtend = false;
                                break;
                            }
                        }
                        if (canExtend) {
                            rectHeight++;
                        }
                    }

                    // Mark all tiles in this rectangle as rendered
                    for (int dy = 0; dy < rectHeight; dy++) {
                        for (int dx = 0; dx < rectWidth; dx++) {
                            rendered[(y + dy) * width + (x + dx)] = true;
                        }
                    }

                    // Render this rectangle
                    Coord tileUL = new Coord(area.ul.x + x, area.ul.y + y);
                    Coord tileBR = new Coord(area.ul.x + x + rectWidth, area.ul.y + y + rectHeight);

                    // Convert to world coordinates
                    Coord2d worldUL = new Coord2d(tileUL).mul(MCache.tilesz);
                    Coord2d worldBR = new Coord2d(tileBR).mul(MCache.tilesz);

                    // Convert to screen coordinates
                    Coord screenUL = map.p2c(worldUL);
                    Coord screenBR = map.p2c(worldBR);

                    // Draw filled rectangle
                    g.chcolor(fillColor);
                    g.frect2(screenUL, screenBR);

                    // Draw border
                    if (borderColor != null) {
                        g.chcolor(borderColor);
                        g.rect2(screenUL, screenBR);
                    }
                }
            }

            g.chcolor(); // Reset color
        } catch (Exception e) {
            // Silently handle errors (likely Loading exceptions for unloaded areas)
        }
    }

    /**
     * Get fill color for a claim tag
     */
    private static Color getColorForTag(String tag) {
        switch (tag) {
            case "cplot":
                return CPLOT_COLOR;
            case "vlg":
                return VLG_COLOR;
            case "prov":
            case "realm":  // Support both "prov" and "realm" tags
                return PROV_COLOR;
            default:
                return null;
        }
    }

    /**
     * Get border color for a claim tag
     */
    private static Color getBorderColorForTag(String tag) {
        switch (tag) {
            case "cplot":
                return CPLOT_BORDER;
            case "vlg":
                return VLG_BORDER;
            case "prov":
            case "realm":
                return PROV_BORDER;
            default:
                return null;
        }
    }
}
