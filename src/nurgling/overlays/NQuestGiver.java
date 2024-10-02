package nurgling.overlays;


import haven.*;
import haven.render.*;
import nurgling.NGob;
import nurgling.NUtils;
import nurgling.widgets.NQuestInfo;

import java.awt.*;
import java.util.HashSet;


public class NQuestGiver extends Sprite implements RenderTree.Node, PView.Render2D
{
    public static final Font bsans  = new Font("Sans", Font.BOLD, 10);
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(bsans, 20, Color.WHITE).aa(true), 2, 1, new Color(36, 25, 25));

    static Tex qrage = Resource.loadtex("nurgling/hud/quest/qrage");
    static Tex qlol = Resource.loadtex("nurgling/hud/quest/qlol");
    static Tex qbring = Resource.loadtex("nurgling/hud/quest/qbring");
    static Tex qwave = Resource.loadtex("nurgling/hud/quest/qwave");
    static Tex qgreet = Resource.loadtex("nurgling/hud/quest/qgreet");
    static Tex qcompleted = Resource.loadtex("nurgling/hud/quest/qcompleted");

    static final VertexArray.Layout pfmt = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0,
            20),
            new VertexArray.Layout.Input( Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;

    Gob gob;
    String name;
    ColorTex сt;
    Tex label;
    HashSet<String> tag;
    NQuestInfo.MarkerInfo mi;
    public NQuestGiver(Owner owner, NQuestInfo.MarkerInfo mi)
    {
        super(owner, null);
        сt = new TexI(Resource.loadimg("marks/questgiver")).st();
        gob = (Gob) owner;
        name = gob.ngob.name;
        label = active_title.render(mi.name).tex();
        tag = mi.prop;
        this.mi = mi;
        double len = MCache.tilesz.x*2;
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
    public void draw(GOut g) {

        super.draw(g);
    }

    void initMarks()
    {
        tag = mi.prop;

    }


    @Override
    public boolean tick(double dt)
    {
        if(mi.prop!=tag)
        {
            initMarks();
        }
        return false;
    }

    @Override
    public void draw(GOut g, Pipe state) {
        Coord sc = Homo3D.obj2view(new Coord3f(0, 0, 15), state, Area.sized(g.sz())).round2();
        g.aimage(label, sc, 0.5, 0.5);
        if (tag!=null && !tag.isEmpty()) {
            double x = -(tag.size()) / 2. + 1;
            double dy = 1;
            if (tag.contains("rage")) {

                Coord coord = Homo3D.obj2view(new Coord3f(0, 0, UI.scale(35) + NUtils.getDeltaZ()), state, Area.sized(g.sz())).round2();
                g.aimage(qrage, coord, x, 0.5);
                x += dy;
            }
            if (tag.contains("laugh")) {
                Coord coord = Homo3D.obj2view(new Coord3f(0, 0, UI.scale(35) + NUtils.getDeltaZ()), state, Area.sized(g.sz())).round2();
                g.aimage(qlol, coord, x, 0.5);
                x += dy;
            }
            if (tag.contains("wave")) {
                Coord coord = Homo3D.obj2view(new Coord3f(0, 0, UI.scale(35) + NUtils.getDeltaZ()), state, Area.sized(g.sz())).round2();
                g.aimage(qwave, coord, x, 0.5);
                x += dy;
            }
            if (tag.contains("bring")) {
                Coord coord = Homo3D.obj2view(new Coord3f(0, 0, UI.scale(35) + NUtils.getDeltaZ()), state, Area.sized(g.sz())).round2();
                g.aimage(qbring, coord, x, 0.5);
                x += dy;
            }
            if (tag.contains("greet")) {
                Coord coord = Homo3D.obj2view(new Coord3f(0, 0, UI.scale(35) + NUtils.getDeltaZ()), state, Area.sized(g.sz())).round2();
                g.aimage(qgreet, coord, x, 0.5);
                x += dy;
            }
            if (tag.contains("tell")) {
                Coord coord = Homo3D.obj2view(new Coord3f(0, 0, UI.scale(35) + NUtils.getDeltaZ()), state, Area.sized(g.sz())).round2();
                g.aimage(qcompleted, coord, x, 0.5);
            }
        }
    }
}
