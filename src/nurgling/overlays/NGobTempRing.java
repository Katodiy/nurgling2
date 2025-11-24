package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NUtils;

/**
 * Temporary ring for objects without GobIcon (session-only, not saved)
 */
public class NGobTempRing extends Sprite implements RenderTree.Node
{
    static final VertexArray.Layout pfmt = new VertexArray.Layout(
            new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 20),
            new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;
    Gob gob;
    ColorTex texture;
    float radius;

    public NGobTempRing(Owner owner, float radius)
    {
        super(owner, null);
        this.gob = (Gob) owner;
        this.radius = radius;
        
        // Use same texture as NGobIconRing
        texture = new TexI(Resource.loadimg("marks/notifyrings")).st();

        float[] data = {
                radius, radius, 5f, 1, 1,
                -radius, radius, 5f, 0, 1,
                -radius, -radius, 5f, 0, 0,
                radius, -radius, 5f, 1, 0,
        };
        
        VertexArray va = new VertexArray(pfmt,
                new VertexArray.Buffer((4) * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(data)));
        this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
    }

    public void added(RenderTree.Slot slot)
    {
        Pipe.Op rmat = Pipe.Op.compose(
                new Rendered.Order.Default(-100), 
                new States.Depthtest(States.Depthtest.Test.LE), 
                States.maskdepth,
                FragColor.blend(new BlendMode(
                        BlendMode.Function.ADD, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.INV_SRC_ALPHA,
                        BlendMode.Function.ADD, BlendMode.Factor.ONE, BlendMode.Factor.INV_SRC_ALPHA)), 
                texture, 
                Clickable.No,
                Rendered.postpfx);
        slot.add(emod, rmat);
    }

    @Override
    public boolean tick(double dt)
    {
        // Keep the ring (only removed manually)
        return false;
    }

    /**
     * Creates a temp ring with auto size based on gob's hitbox
     */
    public static NGobTempRing createAutoSize(Gob gob) {
        double len = MCache.tilesz.x * 2;
        if (gob.ngob != null && gob.ngob.hitBox != null) {
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin), len);
        }
        return new NGobTempRing(gob, (float) len);
    }
}
