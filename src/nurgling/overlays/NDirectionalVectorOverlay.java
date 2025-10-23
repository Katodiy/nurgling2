package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.tools.DirectionalVector;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders fixed directional vectors in the 3D world for triangulation
 * Vectors are drawn from fixed origin points in specific directions
 */
public class NDirectionalVectorOverlay implements RenderTree.Node {
    private static final float RAY_LENGTH = 100000f; // Very long rays (effectively infinite)
    private static final float RAY_HEIGHT = 1.0f; // Height above ground

    /**
     * Renders all directional vectors in the world
     */
    @Override
    public void added(RenderTree.Slot slot) {
        // Get map view to access vectors
        NGameUI gui = NUtils.getGameUI();
        if(gui == null || !(gui.map instanceof NMapView)) return;
        NMapView mapView = (NMapView) gui.map;

        // Create rendering for each vector
        for(DirectionalVector vector : mapView.directionalVectors) {
            try {
                // Calculate end point of ray
                Coord2d farPoint = vector.getPointAt(RAY_LENGTH);

                // Create vertex data for the ray line
                float[] vertices = new float[] {
                    (float)vector.origin.x, -(float)vector.origin.y, RAY_HEIGHT,
                    (float)farPoint.x, -(float)farPoint.y, RAY_HEIGHT
                };

                // Create vertex array
                VertexArray.Layout layout = new VertexArray.Layout(
                    new VertexArray.Layout.Input(
                        Homo3D.vertex,
                        new VectorFormat(3, NumberFormat.FLOAT32),
                        0, 0, 12
                    )
                );

                VertexArray va = new VertexArray(layout,
                    new VertexArray.Buffer(vertices.length * 4,
                        DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(vertices)));

                // Create model for the line
                Model model = new Model(Model.Mode.LINES, va, null);

                // Create material state - red semi-transparent line
                Pipe.Op material = Pipe.Op.compose(
                    new BaseColor(new Color(255, 80, 80, 200)),
                    new States.LineWidth(3.0f),
                    States.Depthtest.none,
                    FragColor.blend(new BlendMode())
                );

                slot.add(model, material);

                // Also draw a small marker at the origin point
                addOriginMarker(slot, vector.origin);

            } catch(Exception e) {
                System.err.println("Error rendering vector: " + e);
            }
        }
    }

    /**
     * Adds a small marker sphere at the vector origin point
     */
    private void addOriginMarker(RenderTree.Slot slot, Coord2d origin) {
        try {
            // Create a small circle/marker at the origin
            int segments = 16;
            List<Float> vertices = new ArrayList<>();

            float markerRadius = 5.0f;
            for(int i = 0; i <= segments; i++) {
                double angle = (i * 2.0 * Math.PI) / segments;
                float x = (float)(origin.x + Math.cos(angle) * markerRadius);
                float y = (float)(origin.y + Math.sin(angle) * markerRadius);
                vertices.add(x);
                vertices.add(-y);
                vertices.add(RAY_HEIGHT + 0.5f); // Slightly above ray
            }

            // Convert to float array
            float[] vertArray = new float[vertices.size()];
            for(int i = 0; i < vertices.size(); i++) {
                vertArray[i] = vertices.get(i);
            }

            VertexArray.Layout layout = new VertexArray.Layout(
                new VertexArray.Layout.Input(
                    Homo3D.vertex,
                    new VectorFormat(3, NumberFormat.FLOAT32),
                    0, 0, 12
                )
            );

            VertexArray va = new VertexArray(layout,
                new VertexArray.Buffer(vertArray.length * 4,
                    DataBuffer.Usage.STATIC,
                    DataBuffer.Filler.of(vertArray)));

            Model model = new Model(Model.Mode.LINE_STRIP, va, null);

            Pipe.Op material = Pipe.Op.compose(
                new BaseColor(new Color(255, 50, 50, 255)),
                new States.LineWidth(2.0f),
                States.Depthtest.none
            );

            slot.add(model, material);

        } catch(Exception e) {
            // Ignore marker rendering errors
        }
    }
}
