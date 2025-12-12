package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Draws a rectangular border outline for zone measurement that follows terrain
 */
public class NZoneBorderOverlay implements RenderTree.Node, Rendered {
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(
            new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));

    private static final float Z_OFFSET = 0.5f;
    private static final Color BORDER_COLOR = new Color(255, 200, 0, 255);

    private final Pipe.Op state;
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    private Model model;

    public NZoneBorderOverlay(Coord tileUL, Coord tileBR) {
        this.state = Pipe.Op.compose(
                new BaseColor(BORDER_COLOR),
                new States.LineWidth(2.0f),
                Clickable.No,
                Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth)
        );

        createBorderModel(tileUL, tileBR);
    }

    private void createBorderModel(Coord tileUL, Coord tileBR) {
        MCache map = NUtils.getGameUI().ui.sess.glob.map;
        float tileSzX = (float) MCache.tilesz.x;
        float tileSzY = (float) MCache.tilesz.y;

        // Calculate corners in world coordinates
        float x1 = tileUL.x * tileSzX;
        float y1 = tileUL.y * tileSzY;
        float x2 = (tileBR.x + 1) * tileSzX;
        float y2 = (tileBR.y + 1) * tileSzY;

        // Get terrain height at each corner
        float z1 = getTerrainZ(map, x1, y1);
        float z2 = getTerrainZ(map, x2, y1);
        float z3 = getTerrainZ(map, x2, y2);
        float z4 = getTerrainZ(map, x1, y2);

        // Convert Y to rendering coordinates (negative)
        float ry1 = -y1;
        float ry2 = -y2;

        // Using LINES: 4 edges * 2 vertices each = 8 vertices = 24 floats
        float[] data = new float[] {
            // Top edge (corner 1 to corner 2)
            x1, ry1, z1,  x2, ry1, z2,
            // Right edge (corner 2 to corner 3)
            x2, ry1, z2,  x2, ry2, z3,
            // Bottom edge (corner 3 to corner 4)
            x2, ry2, z3,  x1, ry2, z4,
            // Left edge (corner 4 to corner 1)
            x1, ry2, z4,  x1, ry1, z1
        };

        VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4,
                DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
        VertexArray va = new VertexArray(LAYOUT, vbo);

        model = new Model(Model.Mode.LINES, va, null);
    }

    private float getTerrainZ(MCache map, float x, float y) {
        try {
            return (float) map.getcz(x, y) + Z_OFFSET;
        } catch (Loading e) {
            return Z_OFFSET;
        }
    }

    @Override
    public void added(RenderTree.Slot slot) {
        slot.ostate(state);
        synchronized (slots) {
            slots.add(slot);
        }
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        synchronized (slots) {
            slots.remove(slot);
        }
    }

    @Override
    public void draw(Pipe context, Render out) {
        if (model != null) {
            out.draw(context, model);
        }
    }
}
