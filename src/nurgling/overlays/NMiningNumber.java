package nurgling.overlays;


import haven.*;
import haven.render.*;
import nurgling.*;


public class NMiningNumber extends Sprite implements RenderTree.Node
{

    static final VertexArray.Layout pfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
            20),
            new VertexArray.Layout.Input( Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;

    Gob gob;

    ColorTex сt;

    public NMiningNumber(Owner owner, int val)
    {
        super(owner, null);
        сt = new TexI(Resource.loadimg("marks/mining/" + String.valueOf(val))).st();
        gob = (Gob) owner;
        float[] data = {
                (float) (0.5f * MCache.tilesz.x), (float) (0.5f * MCache.tilesz.y), 1f, 1, 1,
                -(float) (0.5f * MCache.tilesz.x), (float) (0.5f * MCache.tilesz.y), 1f, 1, 0,
                -(float) (0.5f * MCache.tilesz.x), -(float) (0.5f * MCache.tilesz.y), 1f, 0, 0,
                (float) (0.5f * MCache.tilesz.x), -(float) (0.5f * MCache.tilesz.y), 1f, 0, 1,
        };
        VertexArray va = new VertexArray(pfmt,
                new VertexArray.Buffer((4) * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(data)));
        this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
    }


    public void added(RenderTree.Slot slot)
    {
        Pipe.Op rmat = Pipe.Op.compose(сt,Rendered.postpfx, States.Depthtest.none);
        slot.add(emod,rmat);
    }

    @Override
    public boolean tick(double dt)
    {
        return gob!=null && NUtils.getGameUI().map.player()!=null && gob.rc.dist(NUtils.getGameUI().map.player().rc)>500;
    }

}
