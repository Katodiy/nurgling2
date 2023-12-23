package nurgling.overlays;


import haven.*;
import haven.render.*;
import nurgling.*;


public class NTargetFight extends Sprite implements RenderTree.Node
{

    static final VertexArray.Layout pfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
            20),
            new VertexArray.Layout.Input( Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;

    Gob gob;

    ColorTex сt;

    public NTargetFight(Owner owner)
    {
        super(owner, null);
        сt = new TexI(Resource.loadimg("marks/targetfight")).st();
        gob = (Gob) owner;
        double len = MCache.tilesz.x;
        if(gob.ngob.hitBox!=null)
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin),len);
        float[] data = {
                (float) len, (float) len, 1f, 1, 1,
                -(float) len, (float) len, 1f, 1, 0,
                -(float) len, -(float) len, 1f, 0, 0,
                (float) len, -(float) len, 1f, 0, 1,
        };
        VertexArray va = new VertexArray(pfmt,
                new VertexArray.Buffer((4) * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(data)));
        this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
    }


    public void added(RenderTree.Slot slot)
    {
        Pipe.Op rmat = Pipe.Op.compose(сt,Rendered.postpfx);
        slot.add(emod,rmat);
    }



    @Override
    public boolean tick(double dt)
    {
        return !(NUtils.getGameUI().fv.current!= null && NUtils.getGameUI().fv.current.gobid == gob.id);
    }

}
