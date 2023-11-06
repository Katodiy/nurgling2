package nurgling.areas;

import haven.*;
import static haven.MCache.cmaps;
import nurgling.*;
import nurgling.tools.*;
import org.json.*;

import java.util.*;

public class NArea
{
    private static int nextolid = 0;
    public static class VArea
    {
        public Area area;
        public boolean isVis = false;

        public VArea(Area area)
        {
            this.area = area;
        }
    }

    public static class Space
    {
        private final int max = 99;
        private final int min = 0;

        public HashMap<Long,VArea> space = new HashMap<>();
        public Space()
        {}

        public Space(Coord sc, Coord ec)
        {
            Coord begin = new Coord(Math.min(sc.x, ec.x), Math.min(sc.y, ec.y));
            Coord end = new Coord(Math.max(sc.x, ec.x), Math.max(sc.y, ec.y));
            Coord bd = begin.div(cmaps);
            Coord ed = end.div(cmaps);
            Coord bm = begin.mod(cmaps);
            Coord em = end.mod(cmaps);
            if (bd.equals(ed.x,ed.y))
            {
                MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                space.put(grid.id, new VArea(new Area(bm, em)));
            }
            else
            {
                if (bd.x != ed.x && bd.y != ed.y)
                {
                    MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                    space.put(grid.id, new VArea(new Area(bm, new Coord(max,max))));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(bd.x, ed.y));
                    space.put(grid.id, new VArea(new Area(new Coord(bm.x, min), new Coord(max, em.y))));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(ed.x, bd.y));
                    space.put(grid.id, new VArea(new Area(new Coord(min, bm.y), new Coord(em.x, max))));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(ed);
                    space.put(grid.id, new VArea(new Area(new Coord(min, min), em)));
                }
                else if (bd.x != ed.x)
                {
                    MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                    space.put(grid.id, new VArea(new Area(bm, new Coord(max, em.y))));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(ed.x, bd.y));
                    space.put(grid.id, new VArea(new Area(new Coord(min, bm.y), em)));
                }
                else
                {
                    MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                    space.put(grid.id, new VArea(new Area(bm, new Coord(em.x, max))));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(bd.x, ed.y));
                    space.put(grid.id, new VArea(new Area(new Coord(bm.x, min), em)));
                }
            }
        }
    }

    public NArea(String name)
    {
        this.name = name;
        olid = nextolid++ % MCache.customolssize;
    }

    public NArea(JSONObject obj)
    {
        this.name = (String) obj.get("name");
        space = new Space();
        JSONArray jareas = (JSONArray) obj.get("space");
        for (int i = 0; i < jareas.length(); i++)
        {
            JSONObject jarea = (JSONObject) jareas.get(i);
            space.space.put((Long) jarea.get("id"), new VArea(new Area(new Coord((Integer) jarea.get("begin_x"), (Integer) jarea.get("begin_y")), new Coord((Integer) jarea.get("end_x"), (Integer) jarea.get("end_y")))));
        }
        olid = (nextolid++) % MCache.customolssize;
    }
    public Space space;
    public String name;
    final int olid;
    NAlias items = new NAlias();
    NAlias ws = new NAlias();
    NAlias ic = new NAlias();
    NAlias containers = new NAlias();

    public NAlias getItems()
    {
        return items;
    }

    public NAlias getWorkstations()
    {
        return ws;
    }

    public NAlias getICategories()
    {
        return ic;
    }

    public NAlias getContainers()
    {
        return containers;
    }


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

    ArrayList<History> current = new ArrayList<>();
    public boolean isHighlighted = false;
    public boolean wasHighlighted = false;
    public boolean inWork = false;

    public Pair<Coord2d,Coord2d> getRCArea()
    {
        if(isVisible())
        {
            Coord begin = null;
            Coord end = null;
            for (Long id : space.space.keySet())
            {
                MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(id);
                Area area = space.space.get(id).area;
                Coord b = area.ul.add(grid.ul);
                Coord e = area.br.add(grid.ul);
                begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
                end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
            }
            if (begin != null)
                return new Pair<Coord2d, Coord2d>(begin.mul(MCache.tilesz).sub(MCache.tilehsz), end.mul(MCache.tilesz).add(MCache.tilehsz));
        }
        return null;
    }

    public void tick(double dt)
    {
        if(!inWork)
        {
            NMapView.NOverlayInfo id = ((NMapView) NUtils.getGameUI().map).olsinf.get(isHighlighted ? "hareas" : ("areas" + String.valueOf(olid)));

            boolean needUpdate = false;
            boolean needReset = false;

            if (isVisible())
                for (History h : current)
                {
                    for (int i = 0; i < h.g.ols.length; i++)
                    {
                        if (h.g.ols[i] != null && h.g.ol[i] != null && h.g.ols[i].get().layer(MCache.ResOverlay.class) == id.id)
                        {
                            if (h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] != h.val)
                            {
                                needReset = true;
                                h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] = h.val;
                            }
                        }
                    }
                }

            if (wasHighlighted)
            {
                needReset = true;
                wasHighlighted = false;
            }

            if (needReset)
            {
                NMapView.NOverlayInfo rid = ((NMapView) NUtils.getGameUI().map).olsinf.get((isHighlighted) ? ("areas" + String.valueOf(olid)) : "hareas");
                clearOverlayHistory(rid);

                current.clear();
            }
            for (long a : space.space.keySet())
            {
                MCache.Grid g = NUtils.getGameUI().map.glob.map.findGrid(a);
                if ((!space.space.get(a).isVis || needReset) && g != null)
                {
                    for (int i = 0; i < g.ols.length; i++)
                    {
                        try
                        {
                            if (g.ols[i].get().layer(MCache.ResOverlay.class) == id.id)
                            {
                                Area area = space.space.get(a).area;
                                for (int x = area.ul.x; x <= area.br.x; x++)
                                {
                                    for (int y = area.ul.y; y <= area.br.y; y++)
                                    {
                                        g.ol[i][x + (y * cmaps.x)] = true;
                                        current.add(new History(g, new Coord(x, y), true));
                                    }
                                }
                                space.space.get(a).isVis = true;
                                needUpdate = true;
                            }
                        }
                        catch (Loading e)
                        {
                            //TODO: fcn loading
                        }
                    }
                }
                else
                {
                    if (g == null && space.space.get(a).isVis)
                    {
                        space.space.get(a).isVis = false;
                        needUpdate = true;
                    }

                }
            }
            if (needUpdate)
                ((NMapView) NUtils.getGameUI().map).setStatus(id.id, true);
        }
    }

    private void clearOverlayHistory(NMapView.NOverlayInfo rid)
    {
        for (History h : current)
        {
            for (int i = 0; i < h.g.ols.length; i++)
            {
                if (h.g.ols[i] != null && h.g.ol[i] != null && h.g.ols[i].get().layer(MCache.ResOverlay.class) == rid.id)
                {
                    h.g.ol[i][h.t.x + (h.t.y * MCache.cmaps.x)] = false;
                }
            }
        }
        ((NMapView) NUtils.getGameUI().map).setStatus(rid.id, true);
    }

    public void clearOverlayArea()
    {
        NMapView.NOverlayInfo rid = ((NMapView) NUtils.getGameUI().map).olsinf.get((!isHighlighted) ? ("areas" + String.valueOf(olid)) : "hareas");
        clearOverlayHistory(rid);
        current.clear();
    }

    private boolean isVisible()
    {
        for(VArea s: space.space.values())
        {
            if(!s.isVis)
                return false;
        }
        return true;
    }

    public JSONObject toJson()
    {
        JSONObject res = new JSONObject();
        res.put("name", name);
        JSONArray jspaces = new JSONArray();
        for(long id : space.space.keySet())
        {
            JSONObject jspace = new JSONObject();
            jspace.put("id", id);
            jspace.put("begin_x", space.space.get(id).area.ul.x);
            jspace.put("begin_y", space.space.get(id).area.ul.y);
            jspace.put("end_x", space.space.get(id).area.br.x);
            jspace.put("end_y", space.space.get(id).area.br.y);
            jspaces.put(jspace);
        }
        res.put("space",jspaces);
        return res;
    }
}
