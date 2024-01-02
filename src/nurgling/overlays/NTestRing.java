package nurgling.overlays;


import haven.*;
import haven.render.*;


public class NTestRing extends Sprite implements RenderTree.Node
{

    static final VertexArray.Layout pfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
            20),
            new VertexArray.Layout.Input( Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;

    Gob gob;

    public NTestRing(Owner owner)
    {
        super(owner, null);
        gob = (Gob) owner;
        float[] data = {
                (float) (2f * MCache.tilesz.x), (float) (2f * MCache.tilesz.y), 0.5f, 1, 1,
                -(float) (2f * MCache.tilesz.x), (float) (2f * MCache.tilesz.y), 0.5f, 0, 1,
                -(float) (2f * MCache.tilesz.x), -(float) (2f * MCache.tilesz.y), 0.5f, 0, 0,
                (float) (2f * MCache.tilesz.x), -(float) (2f * MCache.tilesz.y), 0.5f, 1, 0,
        };
        VertexArray va = new VertexArray(pfmt,
                new VertexArray.Buffer((4) * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(data)));
        this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
    }

    public static final ColorTex сt = new TexI(Resource.loadimg("marks/quest")).st();
    public void added(RenderTree.Slot slot)
    {
        Pipe.Op rmat = Pipe.Op.compose(сt,Rendered.postpfx);
        slot.add(emod,rmat);
    }

    public boolean tr = false;
    @Override
    public boolean tick(double dt)
    {
        return tr;
    }

}
