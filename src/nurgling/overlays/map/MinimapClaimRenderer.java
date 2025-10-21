package nurgling.overlays.map;

import haven.*;
import nurgling.widgets.NMiniMap;

import java.awt.Color;

/**
 * Renders personal claims, village claims, and realms on the minimap.
 * Uses persistent MapFile data instead of runtime MCache, showing all discovered claims.
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
     * Queries persistent MapFile data from DisplayGrids to show all discovered claims.
     *
     * @param map The minimap instance
     * @param g   Graphics context
     */
    public static void renderClaims(MiniMap map, GOut g) {
        if (map.ui == null || map.ui.gui == null || map.ui.gui.map == null) {
            return;
        }

        MapView mv = map.ui.gui.map;
        if (mv == null || map.dloc == null) {
            return;
        }

        // Must be NMiniMap to access display grids
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

            // Iterate through all display grids
            for (Coord gc : dgext) {
                MiniMap.DisplayGrid disp = display[dgext.ri(gc)];
                if (disp == null) {
                    continue;
                }

                // Get the underlying DataGrid from MapFile
                MapFile.DataGrid grid;
                try {
                    grid = disp.gref.get();
                } catch (Loading e) {
                    // Grid data not loaded yet
                    continue;
                }

                if (grid == null || grid.ols.isEmpty()) {
                    continue;
                }

                // Process each overlay in this grid
                for (MapFile.Overlay overlay : grid.ols) {
                    try {
                        // Get the overlay resource and its tags
                        Resource res = overlay.olid.get();
                        MCache.ResOverlay olinfo = res.flayer(MCache.ResOverlay.class);

                        if (olinfo == null) {
                            continue;
                        }

                        // Check each tag to see if it matches a claim type we want to render
                        for (String tag : olinfo.tags()) {
                            // Check if this overlay type is enabled in the 3D map
                            if (!mv.visol(tag)) {
                                continue;
                            }

                            // Get color for this claim type
                            Color fillColor = getColorForTag(tag);
                            Color borderColor = getBorderColorForTag(tag);

                            if (fillColor != null) {
                                renderOverlay(map, g, overlay, disp, hsz, fillColor, borderColor);
                                break; // Only render once per overlay, even if it has multiple tags
                            }
                        }
                    } catch (Loading e) {
                        // Resource not loaded yet, skip this overlay
                    } catch (Exception e) {
                        // Silently handle other errors
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle errors
        }
    }

    /**
     * Renders a single overlay on the minimap using rectangle merging for performance.
     * The overlay data comes from the persistent MapFile storage.
     */
    private static void renderOverlay(MiniMap map, GOut g,
                                      MapFile.Overlay overlay,
                                      MiniMap.DisplayGrid disp,
                                      Coord hsz,
                                      Color fillColor,
                                      Color borderColor) {
        try {
            // The overlay boolean array is indexed as: x + (y * cmaps.x)
            // where cmaps is the grid size (100x100 tiles typically)
            int width = MCache.cmaps.x;
            int height = MCache.cmaps.y;
            boolean[] mask = overlay.ol;

            if (mask.length != width * height) {
                return; // Invalid mask size
            }

            // Track which tiles we've already rendered
            boolean[] rendered = new boolean[mask.length];

            // Scan for rectangles to merge
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

                    // Convert grid-local coordinates to segment tile coordinates
                    // disp.sc is the grid coordinate (e.g., grid 5,3)
                    // Each grid is cmaps tiles (e.g., 100x100)
                    Coord tileUL = disp.sc.mul(MCache.cmaps).add(x, y);
                    Coord tileBR = disp.sc.mul(MCache.cmaps).add(x + rectWidth, y + rectHeight);

                    // Convert to screen coordinates using the same formula as MiniMap.drawmap()
                    // Formula: UI.scale(tile).sub(dloc.tc.div(scalef())).add(hsz)
                    Coord screenUL = UI.scale(tileUL).sub(map.dloc.tc.div(map.scalef())).add(hsz);
                    Coord screenBR = UI.scale(tileBR).sub(map.dloc.tc.div(map.scalef())).add(hsz);

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
            // Silently handle errors
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
