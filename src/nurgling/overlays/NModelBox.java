package nurgling.overlays;


import haven.*;
import haven.render.*;
import nurgling.*;

import java.nio.ByteBuffer;
import java.util.*;


public class NModelBox extends Sprite implements RenderTree.Node
{

    public static class NBoundingBox
    {

        public final ArrayList<Polygon> polygons;
        public int vertices = 0;
        public boolean blocks = true;

        public NBoundingBox(
                ArrayList<Polygon> polygons,
                boolean blocks
        )
        {
            this.polygons = polygons;
            for (Polygon pol : polygons)
            {
                vertices += 4;
            }
            this.blocks = blocks;
        }

        public static class Polygon
        {
            public final Coord2d[] vertices;
            public boolean neg;

            public Polygon(Coord2d[] vertices)
            {
                this.vertices = vertices;
            }
        }

        public static NBoundingBox getBoundingBox(NHitBox hitBox)
        {
            if (hitBox != null)
            {
                ArrayList<Polygon> polygons = new ArrayList<>();
                Coord2d[] polyVertexes = new Coord2d[4];
                polyVertexes[0] = hitBox.end;
                polyVertexes[1] = new Coord2d(hitBox.begin.x, hitBox.end.y);
                polyVertexes[2] = hitBox.begin;
                polyVertexes[3] = new Coord2d(hitBox.end.x, hitBox.begin.y);
                polygons.add(new Polygon(polyVertexes));

                return new NBoundingBox(polygons, true);
            }
            else
            {
                return null;
            }
        }
    }

    public static class HidePol extends Sprite implements RenderTree.Node
    {
        public static Pipe.Op lmat = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth,new States.Facecull(),new States.LineWidth(4), new BaseColor(new java.awt.Color(227, 28, 1, 195)));
        public static Pipe.Op emat = Pipe.Op.compose(new BaseColor(new java.awt.Color(224, 193, 79, 255)));
        final Model emod;
        final Model lmod;
        private NBoundingBox.Polygon pol;

        static final VertexArray.Layout pfmt = new VertexArray.Layout(
                new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
                        12));

        public HidePol(NBoundingBox.Polygon pol)
        {
            super(null, null);
            this.pol = pol;

            VertexArray va = new VertexArray(pfmt,
                    new VertexArray.Buffer((4) * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC,
                            this::fill));
            short [] iarr = {0,1,2,3,0};
            Model.Indices indb = new Model.Indices(5, NumberFormat.UINT16, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(iarr));
            this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
            this.lmod = new Model(Model.Mode.LINE_STRIP, va, indb);
        }

        private FillBuffer fill(
                VertexArray.Buffer dst,
                Environment env
        )
        {
            FillBuffer ret = env.fillbuf(dst);
            ByteBuffer buf = ret.push();
            if (pol.neg)
            {
                for (int i = 3; i >= 0; i--)
                {
                    buf.putFloat((float) pol.vertices[i].x).putFloat((float) -pol.vertices[i].y)
                            .putFloat(1.0f);
                }
            }
            else
            {
                for (int i = 0; i < 4; i++)
                {
                    buf.putFloat((float) pol.vertices[i].x).putFloat((float) pol.vertices[i].y)
                            .putFloat(1.0f);
                }
            }
            return (ret);
        }

        public void added(RenderTree.Slot slot)
        {
            slot.add(emod,emat);
            slot.add(lmod,lmat);
        }
    }

    private final NBoundingBox bb;

    boolean isShow = false;

    boolean isVisible = false;

    Gob gob;

    public NModelBox(Gob gob)
    {
        super(null, null);
        this.gob = gob;
        this.bb = NBoundingBox.getBoundingBox(gob.ngob.hitBox);

    }

    Collection<RenderTree.Node> nodes = new ArrayList<>();
    RenderTree.Slot slot = null;

    public void added(RenderTree.Slot slot)
    {
        this.slot = slot;
        if (nodes.isEmpty())
        {
            for (NBoundingBox.Polygon pol : bb.polygons)
            {
                nodes.add(new HidePol(pol));
            }
        }

    }

    @Override
    public boolean tick(double dt)
    {
        if ((Boolean) NConfig.get(NConfig.Key.showBB) != isShow)
        {
            isShow = (Boolean) NConfig.get(NConfig.Key.showBB);
            if (isShow && slot.parent()!=null)
            {
                if(!isVisible)
                {
                isVisible = true;
                for (RenderTree.Node n : nodes)
                {
                    try {
                        slot.add(n);
                    }
                    catch (RenderTree.SlotRemoved e)
                    {
                        return true;
                    }
                }
                }
            }
            else
            {
                isVisible = false;
                slot.clear();
            }
        }
        return super.tick(dt);
    }

    @Override
    public void draw(GOut g)
    {
        super.draw(g);
    }
}
