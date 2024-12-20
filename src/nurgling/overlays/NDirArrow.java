package nurgling.overlays;

import haven.*;
import haven.render.*;
import haven.render.Model.Indices;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.widgets.NAlarmWdg;


import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class NDirArrow extends Sprite implements RenderTree.Node, PView.Render2D  {

    Pipe.Op smat;
    final VertexBuf.VertexData posa;
    final VertexBuf vbuf;
    final Model smod;
    private Coord2d lc;
    float[] barda;
    Coord2d dir;

    float r;

    public Gob target;
    Long id;

    Tex tex;
    final  float height = 15.f;
    Coord3f markerPos;
    Tex img;

    boolean forDelete = false;

    public NDirArrow(Sprite.Owner owner, Color color, float r, Gob target, Tex marker) {
        super(owner, null);
        img = marker;
        id = target.id;
        smat = new BaseColor(color);
        Coord2d cc = target.rc;
        dir = cc.sub(((Gob)owner).rc);
        dir.norm();
        this.target = target;
        this.r = r;
        int n = Math.max(24, (int)(2 * Math.PI * r / 11.0));
        FloatBuffer posb = Utils.wfbuf(n * 3 * 2);
        FloatBuffer nrmb = Utils.wfbuf(n * 3 * 2);

        VertexBuf.VertexData posa = new VertexBuf.VertexData(posb);
        VertexBuf.NormalData nrma = new VertexBuf.NormalData(nrmb);
        VertexBuf vbuf = new VertexBuf(posa, nrma);
        this.smod = new Model(Model.Mode.TRIANGLES, vbuf.data(), new Indices(21 , NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::sidx));
        this.posa = posa;
        this.vbuf = vbuf;
    }

    private FillBuffer sidx(Indices dst, Environment env) {
        FillBuffer ret = env.fillbuf(dst);
        ShortBuffer buf = ret.push().asShortBuffer();
        int b= 0;
        for(int i = 0; i <=6;i++) {
            buf.put(b++, (short) i);
            buf.put(b++, (short) 0);
            buf.put(b++, (short) (i+1));
        }
        return(ret);
    }


    private void setDir(Render g, Coord2d dir, double dist) {
        FloatBuffer posb = posa.data;
        dir.y = -dir.y;
        double cur_r = Math.min(r,0.2*dist);
        posb.put(0, (float)(1.2*cur_r*dir.x)).put(      1, (float)(1.2*cur_r*dir.y)).put(     2,  height);
        markerPos = new Coord3f(0.6f*posb.get(0),0.6f*posb.get(1), posb.get(2));
        Coord2d newDir = new Coord2d(dir.x*cur_r, dir.y*cur_r).rotate(-Math.PI/6);

        for(int i = 1; i <= 7; i++) {
            posb.put(i * 3, (float)(newDir.x)).put(     i  * 3 + 1, (float)(newDir.y)).put(     i  * 3 + 2,  height);
            newDir = newDir.rotate(Math.PI/18);
        }
        vbuf.update(g);
    }

    @Override
    public boolean tick(double dt) {
        return !NAlarmWdg.isInAlarm(target.id);
    }

    public void gtick(Render g) {
        if(target!=null) {
            Coord2d cc = target.rc;

            Coord2d new_dir = cc.sub(((Gob) owner).rc);
            double dist = new_dir.len();
            new_dir = new_dir.norm();
            setDir(g, new_dir, dist);
        }
    }

    public void added(RenderTree.Slot slot) {
        slot.ostate(Pipe.Op.compose(Rendered.postpfx,
                new States.Facecull(States.Facecull.Mode.NONE),
                Location.goback("gobx")));
        slot.add(smod, smat);
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        super.removed(slot);
    }

    @Override
    public void draw(GOut g, Pipe state) {
        if(img!=null) {
            Coord sc = Homo3D.obj2view(markerPos, state, Area.sized(g.sz())).round2();
            g.aimage(img, sc, 0.5, 0.5);
        }
    }
}