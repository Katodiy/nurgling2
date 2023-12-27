package nurgling.overlays.map;

import haven.*;
import haven.render.*;
import nurgling.*;
import static nurgling.NMapView.MINING_OVERLAY;
import nurgling.areas.*;
import nurgling.overlays.*;
import nurgling.tools.*;

import java.awt.*;
import java.util.*;

public class NMiningOverlay extends NOverlay
{
    public Gob dummy = null;
    Coord2d oldDummy = null;
    final ArrayList<Long> curGobs = new ArrayList<>();

    public NMiningOverlay()
    {
        super(MINING_OVERLAY);
        bc = new Color(200, 200, 200, 100);
    }

    public void addDummySupp(Gob gob)
    {
        synchronized (curGobs)
        {
            curGobs.add(gob.id);
            synchronized (forAdd)
            {
                forAdd.add(gob.id);
            }
            dummy = gob;
        }
    }

    public void addMineSupp(Long gob)
    {
        synchronized (curGobs)
        {
            curGobs.add(gob);
            synchronized (forAdd)
            {
                forAdd.add(gob);
            }
        }
    }

    final ArrayList<Long> forClear = new ArrayList<>();
    final ArrayList<Long> forAdd = new ArrayList<>();

    @Override
    public void tick()
    {
        synchronized (forClear)
        {
            forClear.clear();
            for (Long id : curGobs)
            {
                if (Finder.findGob(id) == null)
                {
                    forClear.add(id);
                }
            }
            curGobs.removeAll(forClear);
        }

        requpdate2 = requpdate();

        super.tick();
    }

    boolean[][] buf2;

    public RenderTree.Node makenol(MapMesh mm, Long grid_id, Coord grid_ul)
    {
        if (mm.olvert == null)
            mm.olvert = mm.makeolvbuf();
        class Buf implements Tiler.MCons
        {
            short[] fl = new short[16];
            int fn = 0;

            public void faces(MapMesh m, Tiler.MPart d)
            {
                while (fn + d.f.length > fl.length)
                    fl = Utils.extend(fl, fl.length * 2);
                for (int fi : d.f)
                    fl[fn++] = (short) mm.olvert.vl[d.v[fi].vi];
            }
        }
        Coord t = new Coord();
        Buf buf = new Buf();
//        NArea.VArea space = NUtils.getArea(id).space.space.get(grid_id);
//        Area curArea = space.area.xl(grid_ul);
        buf2 = new boolean[mm.sz.x + 2][mm.sz.y + 2];
        for (Long id : curGobs)
        {
            Gob g = Finder.findGob(id);

            if (g != null && g.findol(NMiningSupport.class) != null)
            {
                NMiningSupport nms = (NMiningSupport) g.findol(NMiningSupport.class).spr;
                Coord beg = nms.begin.sub(mm.ul.sub(1, 1));
                Coord en = nms.end.sub(mm.ul.sub(1, 1));
                boolean[][] data = nms.getData();
                if ((beg.x >= 0 && beg.x <= mm.sz.x + 2 ||
                        en.x >= 0 && en.x <= mm.sz.x + 2) &&
                        (beg.y >= 0 && beg.y <= mm.sz.y + 2 ||
                                en.y >= 0 && en.y <= mm.sz.y + 2))
                {
                    for (t.y = Math.max(beg.y, 0); t.y < Math.min(en.y, mm.sz.y + 2); t.y++)
                    {
                        for (t.x = Math.max(beg.x, 0); t.x < Math.min(en.x, mm.sz.x + 2); t.x++)
                        {
                            if (data[t.x - beg.x][t.y - beg.y])
                            {
                                buf2[t.x][t.y] = data[t.x - beg.x][t.y - beg.y];
                            }
                        }
                    }
                }
            }
        }

        for (t.y = 0; t.y < mm.sz.y; t.y++)
        {
            for (t.x = 0; t.x < mm.sz.x; t.x++)
            {

                if (buf2[t.x + 1][t.y + 1])
                {
                    Coord gc = t.add(mm.ul);
                    mm.map.tiler(mm.map.gettile(gc)).lay(mm, t, gc, buf, false);
                }
            }
        }

        if (buf.fn == 0)
            return (null);
        haven.render.Model mod = new haven.render.Model(haven.render.Model.Mode.TRIANGLES, mm.olvert.dat,
                new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn))));
        return (new MapMesh.ShallowWrap(mod, new MapMesh.NOLOrder(id)));
    }

    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul)
    {
        if (mm.olvert == null)
            mm.olvert = mm.makeolvbuf();
        class Buf implements Tiler.MCons
        {
            int mask;
            short[] fl = new short[16];
            int fn = 0;

            public void faces(MapMesh m, Tiler.MPart d)
            {
                byte[] ef = new byte[d.v.length];
                for (int i = 0; i < d.v.length; i++)
                {
                    if (d.tcy[i] == 0.0f) ef[i] |= 1;
                    if (d.tcx[i] == 1.0f) ef[i] |= 2;
                    if (d.tcy[i] == 1.0f) ef[i] |= 4;
                    if (d.tcx[i] == 0.0f) ef[i] |= 8;
                }
                while (fn + (d.f.length * 2) > fl.length)
                    fl = Utils.extend(fl, fl.length * 2);
                for (int i = 0; i < d.f.length; i += 3)
                {
                    for (int a = 0; a < 3; a++)
                    {
                        int b = (a + 1) % 3;
                        if ((ef[d.f[i + a]] & ef[d.f[i + b]] & mask) != 0)
                        {
                            fl[fn++] = (short) mm.olvert.vl[d.v[d.f[i + a]].vi];
                            fl[fn++] = (short) mm.olvert.vl[d.v[d.f[i + b]].vi];
                        }
                    }
                }
            }
        }
        Area a = Area.sized(mm.ul, mm.sz);

        Buf buf = new Buf();
        for(Coord t : a) {
                buf.mask = 0;
            if (buf2[t.x - mm.ul.x + 1][t.y - mm.ul.y + 1])
            {
                for(int d = 0; d < 4; d++) {
                    Coord pos = t.add(Coord.uecw[d]).sub(mm.ul).add(1,1);
                    if(!buf2[pos.x][pos.y])
                        buf.mask |= 1 << d;
                }
                if(buf.mask != 0)
                    mm.map.tiler(mm.map.gettile(t)).lay(mm, t.sub(a.ul), t, buf, false);
            }
        }

        if (buf.fn == 0)
            return (null);
        haven.render.Model mod = new haven.render.Model(haven.render.Model.Mode.LINES, mm.olvert.dat,
                new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn))));
        return (new MapMesh.ShallowWrap(mod, Pipe.Op.compose(new MapMesh.NOLOrder(id), new States.LineWidth(2))));
    }

    @Override
    public boolean requpdate()
    {
        boolean res = false;
        synchronized (forAdd)
        {
            if (!forAdd.isEmpty())
            {
                res = true;
                forAdd.clear();
            }
        }
        synchronized (forClear)
        {
            if (!forClear.isEmpty())
            {
                res = true;
                forClear.clear();
            }
        }
        if(dummy== null && oldDummy!=null)
        {
            res = true;
            oldDummy = null;
        }
        if(dummy!= null && (oldDummy==null || !oldDummy.equals(dummy.rc.x,dummy.rc.y)))
        {
            res = true;
            oldDummy = new Coord2d(dummy.rc.x, dummy.rc.y);
        }
        return res;
    }
}
