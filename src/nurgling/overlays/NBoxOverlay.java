package nurgling.overlays;

import haven.*;
import haven.render.*;
import haven.render.Model.Indices;
import haven.render.gl.GLState;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NBoxOverlay extends Sprite {
    static final Pipe.Op blacc = Pipe.Op.compose(new BaseColor(new Color(0, 0, 0, 140)), new States.LineWidth(6));
    static final Pipe.Op col =  Pipe.Op.compose(new BaseColor(new Color(255,0,0,140)));
    VertexBuf.VertexData posa;
    VertexBuf vbuf;
    Model smod, emod;
    private Coord2d lc;
    private Coord2d size = new Coord2d(100,100);
    private Coord2d fixator;
    float height = 6f;
    public List<RenderTree.Slot> slots = new ArrayList<>();

    public NBoxOverlay(final Owner owner, Coord2d size,  Coord2d fixator) {
        super(owner, null);
        this.size = size;
        this.fixator = fixator;
        init();
    }

    private void init() {
        float l = (float) (-this.size.x / 2);
        float u = (float) (-this.size.y / 2);
        float r = -l;
        float b = -u;
        int xn = Math.max(2, 1 + (int) ((r - l) / 11.0));
        int yn = Math.max(2, 1 + (int) ((b - u) / 11.0));
        int hn = xn + yn, n = hn * 2;
        FloatBuffer posb = Utils.wfbuf(n * 3 * 2);
        FloatBuffer nrmb = Utils.wfbuf(n * 3 * 2);
        ShortBuffer sidx = Utils.wsbuf(n * 6);
        ShortBuffer eidx = Utils.wsbuf(n);
        int I, ii, v, N = n * 3;
        for (int i = 0; i < xn; i++) {
            float x = l + ((r - l) * i) / (xn - 1);
            I = i;
            v = I * 3;
            posb.put(v + 0, x).put(v + 1, -u).put(v + 2, 10);
            posb.put(v + N + 0, x).put(v + N + 1, -u).put(v + N + 2, -10);
            nrmb.put(v + 0, 0).put(v + 1, 1).put(v + 2, 0);
            nrmb.put(v + N + 0, 0).put(v + N + 1, 1).put(v + N + 2, 0);
            if (i < xn - 1) {
                ii = i * 6;
                sidx.put(ii + 0, (short) (I + 1)).put(ii + 1, (short) (I + 1 + n)).put(ii + 2, (short) I);
                sidx.put(ii + 3, (short) (I + 1 + n)).put(ii + 4, (short) (I + n)).put(ii + 5, (short) I);
            }
            I = i + hn;
            v = I * 3;
            posb.put(v + 0, x).put(v + 1, -b).put(v + 2, 10);
            posb.put(v + N + 0, x).put(v + N + 1, -b).put(v + N + 2, -10);
            nrmb.put(v + 0, 0).put(v + 1, -1).put(v + 2, 0);
            nrmb.put(v + N + 0, 0).put(v + N + 1, -1).put(v + N + 2, 0);
            if (i < xn - 1) {
                ii = (i + hn) * 6;
                sidx.put(ii + 0, (short) I).put(ii + 1, (short) (I + n)).put(ii + 2, (short) (I + 1));
                sidx.put(ii + 3, (short) (I + n)).put(ii + 4, (short) (I + 1 + n)).put(ii + 5, (short) (I + 1));
            }
        }
        for (int i = 0; i < yn; i++) {
            float y = u + ((b - u) * i) / (yn - 1);
            I = i + xn;
            v = I * 3;
            posb.put(v + 0, r).put(v + 1, -y).put(v + 2, 10);
            posb.put(v + N + 0, r).put(v + N + 1, -y).put(v + N + 2, -10);
            nrmb.put(v + 0, 1).put(v + 1, 0).put(v + 2, 0);
            nrmb.put(v + N + 0, 1).put(v + N + 1, 0).put(v + N + 2, 0);
            if (i < yn - 1) {
                ii = (i + xn) * 6;
                sidx.put(ii + 0, (short) I).put(ii + 1, (short) (I + n)).put(ii + 2, (short) (I + 1));
                sidx.put(ii + 3, (short) (I + n)).put(ii + 4, (short) (I + 1 + n)).put(ii + 5, (short) (I + 1));
            }
            I = i + xn + hn;
            v = I * 3;
            posb.put(v + 0, l).put(v + 1, -y).put(v + 2, 10);
            posb.put(v + N + 0, l).put(v + N + 1, -y).put(v + N + 2, -10);
            nrmb.put(v + 0, -1).put(v + 1, 0).put(v + 2, 0);
            nrmb.put(v + N + 0, -1).put(v + N + 1, 0).put(v + N + 2, 0);
            if (i < yn - 1) {
                ii = (i + xn + hn) * 6;
                sidx.put(ii + 0, (short) (I + 1)).put(ii + 1, (short) (I + 1 + n)).put(ii + 2, (short) I);
                sidx.put(ii + 3, (short) (I + 1 + n)).put(ii + 4, (short) (I + n)).put(ii + 5, (short) I);
            }
        }
        for (int i = 0; i < xn; i++) {
            eidx.put(i, (short) i);
            eidx.put(i + hn, (short) ((xn - i - 1) + hn));
        }
        for (int i = 0; i < yn; i++) {
            eidx.put(i + xn, (short) (i + xn));
            eidx.put(i + hn + xn, (short) ((yn - i - 1) + xn + hn));
        }
        VertexBuf.VertexData posa = new VertexBuf.VertexData(Utils.bufcp(posb));
        VertexBuf.NormalData nrma = new VertexBuf.NormalData(Utils.bufcp(nrmb));
        VertexBuf vbuf = new VertexBuf(posa, nrma);
        this.smod = new Model(Model.Mode.TRIANGLES, vbuf.data(), new Indices(n * 6, NumberFormat.UINT16, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(sidx.array())));
        this.emod = new Model(Model.Mode.LINE_STRIP, vbuf.data(), new Indices(n + 1, NumberFormat.UINT16, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(eidx.array())));
        this.posa = posa;
        this.vbuf = vbuf;
    }

    Location pos = null;

    private void setz(Render g,Glob glob, Coord2d rc) {
        FloatBuffer pa = posa.data;
        int p = posa.size() / 2;
        try {
            float rz = (float) glob.map.getcz(rc);
            pos = Location.xlate(new Coord3f((float) rc.x, -(float) rc.y, rz));
            for (int i = 0; i < p; i++) {
                float z = (float) glob.map.getcz(rc.x + pa.get(i * 3), rc.y - pa.get(i * 3 + 1)) - rz;
                pa.put(i * 3 + 2, z + height);
                pa.put((p + i) * 3 + 2, z - height);
            }
        } catch (Loading e) {
        }
        vbuf.update(g);
    }

    public void gtick(Render g) {
        Coord2d cc = ((Gob) owner).rc;
        if (fixator != null)
            cc = cc.floor(fixator).mul(fixator).add(fixator.div(2));
        if ((lc == null) || !lc.equals(cc)) {
            setz(g, owner.context(Glob.class), cc);
            if (!Objects.equals(lc, cc)){
                lc = cc;
                for (RenderTree.Slot sl:slots){
                    sl.ostate(Pipe.Op.compose(Rendered.postpfx,
                                    new States.Facecull(States.Facecull.Mode.NONE),
                                    //Location.goback("gobx")
                                    p -> p.put(Homo3D.loc, null), pos
                            )
                    );
                }
            }
        }
    }

    public void added(RenderTree.Slot slot) {
        slot.ostate(Pipe.Op.compose(Rendered.postpfx,
                        new States.Facecull(States.Facecull.Mode.NONE),
                        //Location.goback("gobx")
                        p -> p.put(Homo3D.loc, null), pos
                )
        );

        if (col != null) {
            slot.add(this.smod, this.col);
            slot.add(this.emod, blacc);
        }
        slots.add(slot);
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        slots.remove(slot);
    }

    @Override
    public boolean tick(double dt) {
        return super.tick(dt);
    }
}

