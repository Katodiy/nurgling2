package nurgling.overlays.map;

import haven.*;
import haven.render.*;
import nurgling.*;
import nurgling.areas.*;

import java.util.*;

public class NOverlay extends MapView.MapRaster
{
    final Integer id;

    final Grid base = new Grid<RenderTree.Node>() {
        public RenderTree.Node getcut(Coord cc) {
            return(map.getnolcut(id, cc));
        }
    };
    final Grid outl = new Grid<RenderTree.Node>() {
        public RenderTree.Node getcut(Coord cc) {
            return(map.getnedgecut(id, cc));
        }
    };

    public NOverlay(Integer id) {
        super(NUtils.getGameUI().map.glob.map, NUtils.getGameUI().map.view);
        this.id = id;
    }

    public void tick() {
        super.tick();
        if(area != null) {
            base.tick();
            outl.tick();
        }
    }

    public void added(RenderTree.Slot slot) {
//			Material overlay_mat = new Material(new BaseColor(194,194,65,56));
        slot.add(base,new BaseColor(NUtils.getArea(id).color));
        slot.add(outl, new BaseColor(200,200,200,200));
        super.added(slot);
    }

    public Loading loading() {
        Loading ret = super.loading();
        if(ret != null)
            return(ret);
        if((ret = base.lastload) != null)
            return(ret);
        return(null);
    }

    public void remove() {

        slot.remove();
        for(MCache.Grid.Cut cut : cuts)
        {
            cut.nols.remove(id);
            cut.nedgs.remove(id);
        }
    }

    public RenderTree.Node makenol(MapMesh mm, Long grid_id, Coord grid_ul) {
        if(mm.olvert == null)
            mm.olvert = mm.makeolvbuf();
        class Buf implements Tiler.MCons {
            short[] fl = new short[16];
            int fn = 0;

            public void faces(MapMesh m, Tiler.MPart d) {
                while(fn + d.f.length > fl.length)
                    fl = Utils.extend(fl, fl.length * 2);
                for(int fi : d.f)
                    fl[fn++] = (short)mm.olvert.vl[d.v[fi].vi];
            }
        }
        Coord t = new Coord();
        Buf buf = new Buf();
        NArea.VArea space = NUtils.getArea(id).space.space.get(grid_id);
        Area curArea = space.area.xl(grid_ul);
        for(t.y = 0; t.y < mm.sz.y; t.y++) {
            for(t.x = 0; t.x < mm.sz.x; t.x++) {
                Coord gc = t.add(mm.ul);
                if(curArea.contains(gc))
                {
                    mm.map.tiler(mm.map.gettile(gc)).lay(mm, t, gc, buf, false);
                }
            }
        }

        if(buf.fn == 0)
            return(null);
        haven.render.Model mod = new haven.render.Model(haven.render.Model.Mode.TRIANGLES, mm.olvert.dat,
                new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn))));
        return(new MapMesh.ShallowWrap(mod, new MapMesh.NOLOrder(id)));
    }

    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul) {
        if(mm.olvert == null)
            mm.olvert = mm.makeolvbuf();
        class Buf implements Tiler.MCons {
            int mask;
            short[] fl = new short[16];
            int fn = 0;

            public void faces(MapMesh m, Tiler.MPart d) {
                byte[] ef = new byte[d.v.length];
                for(int i = 0; i < d.v.length; i++) {
                    if(d.tcy[i] == 0.0f) ef[i] |= 1;
                    if(d.tcx[i] == 1.0f) ef[i] |= 2;
                    if(d.tcy[i] == 1.0f) ef[i] |= 4;
                    if(d.tcx[i] == 0.0f) ef[i] |= 8;
                }
                while(fn + (d.f.length * 2) > fl.length)
                    fl = Utils.extend(fl, fl.length * 2);
                for(int i = 0; i < d.f.length; i += 3) {
                    for(int a = 0; a < 3; a++) {
                        int b = (a + 1) % 3;
                        if((ef[d.f[i + a]] & ef[d.f[i + b]] & mask) != 0) {
                            fl[fn++] = (short)mm.olvert.vl[d.v[d.f[i + a]].vi];
                            fl[fn++] = (short)mm.olvert.vl[d.v[d.f[i + b]].vi];
                        }
                    }
                }
            }
        }
        Area a = Area.sized(mm.ul, mm.sz);

        Buf buf = new Buf();
        NArea.VArea space = NUtils.getArea(id).space.space.get(grid_id);
        Area curArea = space.area.xl(grid_ul);
        Area fullarea = NUtils.getArea(id).getArea();
        for(Coord t : a) {
            if(curArea.contains(t))
            {
                buf.mask = 0;
                for(int d = 0; d < 4; d++) {
                    if(!fullarea.contains(t.add(Coord.uecw[d])))
                        buf.mask |= 1 << d;
                }
                if(buf.mask != 0)
                    mm.map.tiler(mm.map.gettile(t)).lay(mm, t.sub(a.ul), t, buf, false);
            }
        }
        if(buf.fn == 0)
            return(null);
        haven.render.Model mod = new haven.render.Model(haven.render.Model.Mode.LINES, mm.olvert.dat,
                new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn))));
        return(new MapMesh.ShallowWrap(mod, Pipe.Op.compose(new MapMesh.NOLOrder(id), new States.LineWidth(2))));
    }

    public ArrayList<MCache.Grid.Cut> cuts = new ArrayList<>();
}