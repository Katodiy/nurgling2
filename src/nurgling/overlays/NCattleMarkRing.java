package nurgling.overlays;

import haven.*;
import haven.render.*;

/**
 * Ring overlay for marked cattle roster entries
 */
public class NCattleMarkRing extends Sprite implements RenderTree.Node
{
    static final VertexArray.Layout pfmt = new VertexArray.Layout(
            new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 20),
            new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;
    Gob gob;
    ColorTex texture;

    public NCattleMarkRing(Owner owner)
    {
        super(owner, null);
        this.gob = (Gob) owner;
        
        texture = new TexI(Resource.loadimg("marks/domesticring")).st();

        double len = MCache.tilesz.x * 2;
        if (gob.ngob != null && gob.ngob.hitBox != null) {
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin), len);
        }

        float radius = (float) len;
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
        return false;
    }
}
