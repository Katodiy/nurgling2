package nurgling.overlays;


import haven.*;
import haven.render.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.tools.VSpec;
import nurgling.widgets.NQuestInfo;

import java.util.ArrayList;


public class NLPassistant extends Sprite implements RenderTree.Node
{

    static final VertexArray.Layout pfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
            20),
            new VertexArray.Layout.Input( Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;

    Gob gob;

    String name;

    ColorTex сt;

    int targetSize = 0;


    public NLPassistant(Owner owner)
    {
        super(owner, null);
        сt = new TexI(Resource.loadimg("marks/lpassistant")).st();
        gob = (Gob) owner;
        name = gob.ngob.name;
        targetSize = VSpec.object.get(name).size();
        double len = MCache.tilesz.x*2;
        if(gob.ngob.hitBox!=null)
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin),len);
        float[] data = {
                (float) len, (float) len, 10f, 1, 1,
                -(float) len, (float) len, 10f, 1, 0,
                -(float) len, -(float) len, 10f, 0, 0,
                (float) len, -(float) len, 10f, 0, 1,
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
        return !(Boolean) NConfig.get(NConfig.Key.lpassistent) || NUtils.getGameUI() == null || NUtils.getGameUI().getCharInfo().LpExplorerGetSize(name) == targetSize;
    }

}
