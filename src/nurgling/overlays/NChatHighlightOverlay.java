package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NUtils;

/**
 * Highlight overlay for objects referenced in chat using @{id} notation.
 * Similar to NLPassistant but with a timer and bright highlight color.
 */
public class NChatHighlightOverlay extends Sprite implements RenderTree.Node {
    
    static final VertexArray.Layout pfmt = new VertexArray.Layout(
        new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 20),
        new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20)
    );
    
    final Model emod;
    final Gob gob;
    final ColorTex ct;
    final long startTime;
    
    // Duration in milliseconds (12 seconds)
    private static final long DURATION_MS = 12000;
    
    public NChatHighlightOverlay(Owner owner) {
        super(owner, null);
        
        // Create bright green highlight texture
        ct = new TexI(Resource.loadimg("marks/newlpassistant")).st();
        
        gob = (Gob) owner;
        startTime = System.currentTimeMillis();
        
        // Calculate size based on hitbox or default tile size
        double len = MCache.tilesz.x * 2;
        if (gob.ngob.hitBox != null) {
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin), len);
        }
        
        // Create vertex data for quad
        float[] data = new float[]{
                (float) len, (float) len, 1f, 1, 1,
                -(float) len, (float) len, 1f, 1, 0,
                -(float) len, -(float) len, 1f, 0, 0,
                (float) len, -(float) len, 1f, 0, 1,
        };
        
        VertexArray va = new VertexArray(
            pfmt,
            new VertexArray.Buffer(
                (4) * pfmt.inputs[0].stride,
                DataBuffer.Usage.STATIC,
                DataBuffer.Filler.of(data)
            )
        );
        
        this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
    }
    
    public void added(RenderTree.Slot slot) {
        // Setup rendering with transparency and blend mode
        Pipe.Op rmat = Pipe.Op.compose(
            new Rendered.Order.Default(-100),
            new States.Depthtest(States.Depthtest.Test.LE),
            States.maskdepth,
            FragColor.blend(new BlendMode(
                BlendMode.Function.ADD,
                BlendMode.Factor.SRC_ALPHA,
                BlendMode.Factor.INV_SRC_ALPHA,
                BlendMode.Function.ADD,
                BlendMode.Factor.ONE,
                BlendMode.Factor.INV_SRC_ALPHA
            )),
            ct,
            Rendered.postpfx
        );
        slot.add(emod, rmat);
    }
    
    @Override
    public boolean tick(double dt) {
        // Remove overlay after duration expires
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= DURATION_MS;
    }
}
