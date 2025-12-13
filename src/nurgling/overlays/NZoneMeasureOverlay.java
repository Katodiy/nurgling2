package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NMapView;
import nurgling.NUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;

public class NZoneMeasureOverlay {
    // Ground highlight overlay
    private MCache.Overlay groundOverlay;

    // Border line overlay
    private NZoneBorderOverlay borderOverlay;
    private RenderTree.Slot borderSlot;

    // Zone bounds (in tile coordinates)
    private final Coord tileUL;
    private final Coord tileBR;
    private final int width;
    private final int height;

    // Colors
    private final Color fillColor;
    private final Color edgeColor;

    // Virtual gob for center label
    private OCache.Virtual labelGob;

    public NZoneMeasureOverlay(MCache map, Coord tileStart, Coord tileEnd,
                               int width, int height, Color fillColor, Color edgeColor) {
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
        this.edgeColor = edgeColor;

        // Normalize coordinates (ensure ul < br)
        this.tileUL = new Coord(
            Math.min(tileStart.x, tileEnd.x),
            Math.min(tileStart.y, tileEnd.y)
        );
        this.tileBR = new Coord(
            Math.max(tileStart.x, tileEnd.x),
            Math.max(tileStart.y, tileEnd.y)
        );

        // Create ground overlay with fill color
        Area area = new Area(tileUL, tileBR.add(1, 1));
        groundOverlay = map.new Overlay(area, createOverlayInfo(fillColor));

        // Create border overlay with edge color
        createBorderOverlay();

        // Create center label
        createCenterLabel();
    }

    private MCache.OverlayInfo createOverlayInfo(Color color) {
        return new MCache.OverlayInfo() {
            final Material mat = new Material(
                new BaseColor(color),
                States.maskdepth
            );

            public Collection<String> tags() {
                return Arrays.asList("show");
            }

            public Material mat() {
                return mat;
            }
        };
    }

    private void createBorderOverlay() {
        borderOverlay = new NZoneBorderOverlay(tileUL, tileBR, edgeColor);
        NMapView mapView = (NMapView) NUtils.getGameUI().map;
        borderSlot = mapView.basic.add(borderOverlay);
    }

    private void createCenterLabel() {
        Glob glob = NUtils.getGameUI().ui.sess.glob;
        OCache oc = glob.oc;

        double tileSzX = MCache.tilesz.x;
        double tileSzY = MCache.tilesz.y;

        // Calculate center of zone in world coordinates
        Coord2d center = new Coord2d(
            (tileUL.x + tileBR.x + 1) / 2.0 * tileSzX,
            (tileUL.y + tileBR.y + 1) / 2.0 * tileSzY
        );

        // Create virtual gob with label sprite
        labelGob = oc.new Virtual(center, 0);
        labelGob.virtual = true;
        labelGob.addcustomol(new NZoneMeasureLabelSprite(labelGob, width, height));
        oc.add(labelGob);
    }

    public boolean contains(Coord tileCoord) {
        return tileCoord.x >= tileUL.x && tileCoord.x <= tileBR.x &&
               tileCoord.y >= tileUL.y && tileCoord.y <= tileBR.y;
    }

    public void destroy() {
        if (groundOverlay != null) {
            groundOverlay.destroy();
            groundOverlay = null;
        }

        // Remove border overlay
        if (borderSlot != null) {
            borderSlot.remove();
            borderSlot = null;
            borderOverlay = null;
        }

        // Remove virtual gob
        if (labelGob != null) {
            Glob glob = NUtils.getGameUI().ui.sess.glob;
            glob.oc.remove(labelGob);
            labelGob = null;
        }
    }

    public Coord getTileUL() {
        return tileUL;
    }

    public Coord getTileBR() {
        return tileBR;
    }
}
