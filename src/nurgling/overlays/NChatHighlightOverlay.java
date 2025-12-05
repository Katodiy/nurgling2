package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NUtils;

/**
 * Highlight overlay for objects referenced in chat using @{id} notation.
 * Similar to NLPassistant but with a timer and bright pulsating highlight.
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
    private RenderTree.Slot slot;
    
    // Duration in milliseconds (12 seconds)
    private static final long DURATION_MS = 12000;
    // Pulsation frequency (Hz)
    private static final double PULSE_FREQ = 2.0;
    // Pulsation amplitude (0.0 - 1.0)
    private static final double PULSE_AMP = 0.3;
    
    public NChatHighlightOverlay(Owner owner) {
        super(owner, null);
        
        // Create bright green highlight texture
        ct = new TexI(Resource.loadimg("marks/altselect")).st();
        
        gob = (Gob) owner;
        startTime = System.currentTimeMillis();
        
        // Calculate size based on hitbox or default tile size
        double len = MCache.tilesz.x * 2;
        if (gob.ngob.hitBox != null) {
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin), len);
        }
        
        // Create vertex data for quad
        float[] data = {
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
        this.slot = slot;
        updateSlot();
    }
    
    private void updateSlot() {
        if (slot == null)
            return;
            
        // Calculate pulsation scale
        long elapsed = System.currentTimeMillis() - startTime;
        double t = elapsed / 1000.0; // time in seconds
        double pulse = 1.0 + PULSE_AMP * Math.sin(2 * Math.PI * PULSE_FREQ * t);
        
        // Create scale transform
        Matrix4f scaleMatrix = Transform.makescale(new Matrix4f(), (float) pulse, (float) pulse, 1.0f);
        
        Location scaleLoc = new Location(scaleMatrix, "scale");
        
        // Setup rendering with transparency, blend mode and scale
        Pipe.Op rmat = Pipe.Op.compose(
            scaleLoc,
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
            Clickable.No,
            Rendered.postpfx
        );
        
        slot.ostate(rmat);
        slot.add(emod);
    }
    
    @Override
    public boolean tick(double dt) {
        // Update pulsation
        updateSlot();
        
        // Remove overlay after duration expires
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= DURATION_MS;
    }
}
