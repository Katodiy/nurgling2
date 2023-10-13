package nurgling;

import haven.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static haven.MCache.cmaps;

public class NMiningSafeMap extends Sprite
{
    Gob owner;
    String name;
    boolean isPlob;
    MapView.Plob plob;

    Coord2d plobrc = Coord2d.z;
    double r;
    boolean isInstalled = false;


    public static class History
    {
        MCache.Grid g;
        Coord t;
        boolean val;

        public History(MCache.Grid g, Coord t, boolean b)
        {
            this.g = g;
            this.t = t;
            this.val = b;
        }
    }

    ArrayList<History> hist = new ArrayList<>();
    ArrayList<History> current = new ArrayList<>();


    public NMiningSafeMap(Owner owner, String key, double r)
    {
        super(owner, null);
        this.name = key;
        this.owner = (Gob) owner;
        if (owner != null)
        {
            if (owner instanceof MapView.Plob)
            {
                this.plob = (MapView.Plob) owner;
                isPlob = true;
            }
        }
        this.r = r;
    }

    @Override
    public boolean tick(double dt)
    {
        NMapView.NOverlayInfo id = ((NMapView) NUtils.getGameUI().map).olsinf.get(name);
        if (!isPlob)
        {
            boolean needUpdate = false;
            for (History h : current)
            {
                if (isInstalled)
                    for (int i = 0; i < h.g.ols.length; i++)
                    {
                        if (h.g.ols[i].get().layer(MCache.ResOverlay.class) == id.id)
                        {
                            if (h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] != h.val)
                            {
                                needUpdate = true;
                                h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] = h.val;
                            }
                        }
                    }
            }
            if (needUpdate)
                ((NMapView) NUtils.getGameUI().map).setStatus(id.id, true);
        }
        if ((!isInstalled && !isPlob) || (isPlob && (plobrc.x != plob.rc.x || plobrc.y != plob.rc.y)))
        {
            Coord2d tar = new Coord2d(owner.rc.x, owner.rc.y);
            Coord beg = (tar.sub(new Coord2d(r - 1, r - 1))).div(MCache.tilesz).floor();
            Coord end = (tar.add(new Coord2d(r + 1, r + 1))).div(MCache.tilesz).floor();
            Area a = Area.sized(beg, new Coord2d(2 * r + MCache.tilesz.x, 2 * r + MCache.tilesz.x).div(MCache.tilesz).floor());
            MCache map = NUtils.getUI().sess.glob.map;
            boolean needUpdate = false;
            Set<MCache.Grid> grids = new HashSet<>();
            grids.add(map.getgrid(beg.div(cmaps)));
            grids.add(map.getgrid(end.div(cmaps)));
            grids.add(map.getgrid(new Coord(beg.x, end.y).div(cmaps)));
            grids.add(map.getgrid(new Coord(end.x, beg.y).div(cmaps)));
            if (isPlob)
            {
                plobrc = new Coord2d(tar.x, tar.y);
                for (History h : hist)
                {
                    for (int i = 0; i < h.g.ols.length; i++)
                    {
                        if (h.g.ols[i].get().layer(MCache.ResOverlay.class) == id.id)
                        {
                            h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] = h.val;
                        }
                    }
                }
                if (!hist.isEmpty())
                    ((NMapView) NUtils.getGameUI().map).setStatus(id.id, true);
                hist.clear();
            }
            current.clear();
            for (MCache.Grid g : grids)
            {
                for (int i = 0; i < g.ols.length; i++)
                {
                    if (g.ols[i].get().layer(MCache.ResOverlay.class) == id.id)
                    {
                        for (Coord c : a)
                        {
                            Coord t = c.sub(g.ul);
                            if ((t.x + (t.y * MCache.cmaps.x)) < 10000 && t.x < 100 && t.y < 100 && t.x >= 0 && t.y >= 0)
                            {
                                boolean res = (c.mul(MCache.tilesz).add(new Coord2d(MCache.tilesz.x / 2., MCache.tilesz.y / 2.)).dist(tar) <= r);
                                if (res)
                                {
                                    current.add(new History(g, t, res));
                                    if (!g.ol[i][t.x + (t.y * MCache.cmaps.x)])
                                    {
                                        hist.add(new History(g, t, g.ol[i][t.x + (t.y * MCache.cmaps.x)]));
                                        g.ol[i][t.x + (t.y * MCache.cmaps.x)] = res;
                                        needUpdate = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (needUpdate || (isPlob && (plobrc.x != plob.rc.x || plobrc.y != plob.rc.y)))
                ((NMapView) NUtils.getGameUI().map).setStatus(id.id, true);
            if (!isPlob)
            {
                isInstalled = true;
            }
            if (owner != null)
                ((NMapView) NUtils.getGameUI().map).olsinf.get(name).gobs.put(owner.id, current);
        }
        return super.tick(dt);
    }
}
