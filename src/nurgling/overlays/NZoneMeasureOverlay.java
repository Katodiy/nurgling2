package nurgling.overlays;

import haven.*;
import haven.render.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;

public class NZoneMeasureOverlay implements PView.Render2D, RenderTree.Node {
    // Ground highlight overlay
    private MCache.Overlay groundOverlay;

    // Zone bounds (in tile coordinates)
    private final Coord tileUL;
    private final Coord tileBR;
    private final int width;
    private final int height;

    // Pre-rendered text textures
    private TexI widthLabel;
    private TexI heightLabel;

    // Text style
    private static final Text.Foundry labelFnd = new Text.Foundry(Text.sans, 14).aa(true);

    // Render slot for the label sprite
    private RenderTree.Slot labelSlot;

    // Material for ground overlay (semi-transparent yellow)
    private static final MCache.OverlayInfo zoneol = new MCache.OverlayInfo() {
        final Material mat = new Material(
            new BaseColor(255, 200, 0, 64),
            States.maskdepth
        );

        public Collection<String> tags() {
            return Arrays.asList("show");
        }

        public Material mat() {
            return mat;
        }
    };

    public NZoneMeasureOverlay(MCache map, Coord tileStart, Coord tileEnd,
                               int width, int height) {
        this.width = width;
        this.height = height;

        // Normalize coordinates (ensure ul < br)
        this.tileUL = new Coord(
            Math.min(tileStart.x, tileEnd.x),
            Math.min(tileStart.y, tileEnd.y)
        );
        this.tileBR = new Coord(
            Math.max(tileStart.x, tileEnd.x),
            Math.max(tileStart.y, tileEnd.y)
        );

        // Create ground overlay
        Area area = new Area(tileUL, tileBR.add(1, 1));
        groundOverlay = map.new Overlay(area, zoneol);

        // Pre-render dimension labels
        widthLabel = new TexI(labelFnd.render(String.valueOf(width), Color.WHITE).img);
        heightLabel = new TexI(labelFnd.render(String.valueOf(height), Color.WHITE).img);
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
        if (labelSlot != null) {
            labelSlot.remove();
            labelSlot = null;
        }
    }

    @Override
    public void draw(GOut g, Pipe state) {
        // Calculate edge midpoints in world coordinates
        float tileSzX = (float) MCache.tilesz.x;
        float tileSzY = (float) MCache.tilesz.y;

        // Top edge center (width label)
        Coord3f topCenter = new Coord3f(
            (tileUL.x + tileBR.x + 1) / 2f * tileSzX,
            -tileUL.y * tileSzY,
            5f
        );

        // Bottom edge center (width label)
        Coord3f bottomCenter = new Coord3f(
            (tileUL.x + tileBR.x + 1) / 2f * tileSzX,
            -(tileBR.y + 1) * tileSzY,
            5f
        );

        // Left edge center (height label)
        Coord3f leftCenter = new Coord3f(
            tileUL.x * tileSzX,
            -(tileUL.y + tileBR.y + 1) / 2f * tileSzY,
            5f
        );

        // Right edge center (height label)
        Coord3f rightCenter = new Coord3f(
            (tileBR.x + 1) * tileSzX,
            -(tileUL.y + tileBR.y + 1) / 2f * tileSzY,
            5f
        );

        Area viewport = Area.sized(g.sz());

        // Draw width labels (top and bottom edges)
        Coord scTop = Homo3D.obj2view(topCenter, state, viewport).round2();
        Coord scBottom = Homo3D.obj2view(bottomCenter, state, viewport).round2();
        if (scTop != null) {
            drawLabelWithBackground(g, widthLabel, scTop);
        }
        if (scBottom != null) {
            drawLabelWithBackground(g, widthLabel, scBottom);
        }

        // Draw height labels (left and right edges)
        Coord scLeft = Homo3D.obj2view(leftCenter, state, viewport).round2();
        Coord scRight = Homo3D.obj2view(rightCenter, state, viewport).round2();
        if (scLeft != null) {
            drawLabelWithBackground(g, heightLabel, scLeft);
        }
        if (scRight != null) {
            drawLabelWithBackground(g, heightLabel, scRight);
        }
    }

    private void drawLabelWithBackground(GOut g, TexI label, Coord pos) {
        // Draw dark background for readability
        Coord sz = label.sz();
        Coord bgUL = pos.sub(sz.div(2)).sub(3, 2);
        Coord bgBR = pos.add(sz.div(2)).add(3, 2);

        g.chcolor(new Color(0, 0, 0, 200));
        g.frect2(bgUL, bgBR);
        g.chcolor();

        // Draw text centered
        g.aimage(label, pos, 0.5, 0.5);
    }

    @Override
    public void added(RenderTree.Slot slot) {
        this.labelSlot = slot;
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        this.labelSlot = null;
    }

    public Coord getTileUL() {
        return tileUL;
    }

    public Coord getTileBR() {
        return tileBR;
    }
}
