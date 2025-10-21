package nurgling.overlays.map;

import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.widgets.NMiniMap;

import java.awt.Color;
import java.util.Collection;

/**
 * Renders personal claims, village claims, and realms on the minimap.
 * Synchronizes with the 3D map overlay visibility toggles.
 */
public class MinimapClaimRenderer {
    // Claim colors (semi-transparent to avoid obscuring terrain)
    private static final Color CPLOT_COLOR = new Color(0, 255, 0, 60);    // Personal - Green
    private static final Color VLG_COLOR = new Color(255, 255, 0, 60);     // Village - Yellow
    private static final Color PROV_COLOR = new Color(255, 0, 255, 60);    // Realm - Magenta

    // Outline colors for better visibility
    private static final Color CPLOT_BORDER = new Color(0, 255, 0, 120);
    private static final Color VLG_BORDER = new Color(255, 255, 0, 120);
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
        NMapView nmv = (NMapView) mv;

        try {
            // Get player's position in world-relative tile coordinates (NOT segment-absolute)
            Gob player = mv.player();
            if (player == null) {
                return;
            }

            Coord playerTileWorld = player.rc.div(MCache.tilesz).floor();

            // Query area around player in world-relative tile coordinates
            final int QUERY_RADIUS = 100; // tiles in each direction
            Area queryArea = Area.sized(
                playerTileWorld.sub(QUERY_RADIUS, QUERY_RADIUS),
                new Coord(QUERY_RADIUS * 2, QUERY_RADIUS * 2)
            );

            System.out.println("=== CLAIM OVERLAY RENDERING ===");
            System.out.println("Player tc (sessloc.tc): " + map.sessloc.tc);
            System.out.println("Player tile (rc/tilesz): " + playerTileWorld);
            System.out.println("Query area around player: " + queryArea);

            // Try to get overlays for this area
            Collection<MCache.OverlayInfo> overlays;
            try {
                overlays = map.ui.sess.glob.map.getols(queryArea);
            } catch (Loading e) {
                // Data not loaded yet, skip this frame
                System.out.println("Loading exception - data not loaded for query area");
                return;
            }

            System.out.println("Found " + overlays.size() + " overlays in query area");

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
                        System.out.println("Rendering overlay with tag: " + tag);
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

            // Render each claimed tile
            int idx = 0;
            int tilesRendered = 0;
            int debugCount = 0;
            for (Coord tc : area) {
                if (mask[idx++]) {
                    tilesRendered++;

                    // Debug first tile to understand coordinate system
                    if (debugCount == 0) {
                        System.out.println("  First tile tc from iteration: " + tc);
                        System.out.println("  sessloc.tc: " + map.sessloc.tc);
                        debugCount++;
                    }

                    // tc is in world-relative tile coordinates
                    // Convert to world coordinates, then to screen coordinates
                    Coord2d tc2d = new Coord2d(tc);
                    Coord2d worldUL = tc2d.mul(MCache.tilesz);
                    Coord2d worldBR = worldUL.add(MCache.tilesz);

                    Coord screenUL = map.p2c(worldUL);
                    Coord screenBR = map.p2c(worldBR);

                    // Draw filled rectangle for the claim using frect2 like fog
                    g.chcolor(fillColor);
                    g.frect2(screenUL, screenBR);

                    // Draw border for better visibility
                    if (borderColor != null) {
                        g.chcolor(borderColor);
                        g.rect2(screenUL, screenBR);
                    }
                }
            }
            if (tilesRendered > 0) {
                System.out.println("  Rendered " + tilesRendered + " tiles");
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
