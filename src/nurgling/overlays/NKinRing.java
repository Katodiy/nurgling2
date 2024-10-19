package nurgling.overlays;


import haven.*;
import haven.render.*;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.*;
import nurgling.conf.*;


public class NKinRing extends Sprite implements RenderTree.Node
{

    static final VertexArray.Layout pfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
            20),
            new VertexArray.Layout.Input( Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;

    Gob gob;

    public NKinRing(Owner owner)
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

    public static final ColorTex сt = new TexI(Resource.loadimg("marks/kintears/white")).st();
    public void added(RenderTree.Slot slot)
    {
        Buddy buddy = gob.getattr(Buddy.class);
        if((buddy!=null && buddy.b!=null && NKinProp.get(buddy.b.group).ring) || unknown)
        {
            Pipe.Op rmat = Pipe.Op.compose(сt, Rendered.postpfx);
            slot.add(emod, rmat);
            _slot = slot;
        }
    }

    RenderTree.Slot _slot;
    boolean unknown = false;
    @Override
    public boolean tick(double dt)
    {
        String posename = gob.pose();
        if((posename != null && posename.contains("knocked")) || NUtils.playerID() == gob.id)
            return true;
        Buddy buddy = gob.getattr(Buddy.class);
        if(buddy == null)
        {
            unknown = true;
            if (_slot == null)
            {
                if (NKinProp.get(0).ring)
                {
                    RUtils.multiadd(gob.slots, this);
                }
            }
        }
        else
        {
            if (buddy.b!=null && NKinProp.get(buddy.b.group).ring)
            {
                if(unknown)
                {
                    unknown = false;
                }
                if (_slot == null)
                {
                    unknown = false;
                    RUtils.multiadd(gob.slots, this);
                }
            }
            else
            {
                if (_slot != null)
                {
                    _slot.remove();
                    _slot = null;
                }
            }
        }
        return false;
    }

}
