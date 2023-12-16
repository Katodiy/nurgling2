package nurgling.areas;

import haven.*;
import static haven.MCache.cmaps;
import nurgling.*;
import nurgling.tools.*;
import org.json.*;

import java.awt.*;
import java.util.*;

public class NArea
{



    public void update()
    {
    }

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
        private final int max = 100;
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
            Coord em = end.mod(cmaps).add(1,1);
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
    }

    public NArea(JSONObject obj)
    {
        this.name = (String) obj.get("name");
        this.id = (Integer) obj.get("id");
        if(obj.has("color"))
        {
            JSONObject color = (JSONObject) obj.get("color");
            if (color != null)
            {
                this.color = new Color((Integer) color.get("r"), (Integer) color.get("g"), (Integer) color.get("b"), (Integer) color.get("a"));
            }
        }
        space = new Space();
        JSONArray jareas = (JSONArray) obj.get("space");
        for (int i = 0; i < jareas.length(); i++)
        {
            JSONObject jarea = (JSONObject) jareas.get(i);
            space.space.put((Long) jarea.get("id"), new VArea(new Area(new Coord((Integer) jarea.get("begin_x"), (Integer) jarea.get("begin_y")), new Coord((Integer) jarea.get("end_x"), (Integer) jarea.get("end_y")))));
            grids_id.add((Long)jarea.get("id"));
        }
        if(obj.has("in"))
        {
            jin = (JSONArray) obj.get("in");
        }
        if(obj.has("out"))
        {
            jout = (JSONArray) obj.get("out");
        }
        if(obj.has("spec"))
        {
            jspec = (JSONArray) obj.get("spec");
            for(int i = 0 ; i < jspec.length(); i++)
                spec.add((String)jspec.get(i));
        }
    }
    public Space space;
    public String name;
    public int id;
    public Color color = new Color(194,194,65,56);
    public final ArrayList<Long> grids_id = new ArrayList<>();

    public ArrayList<String> spec = new ArrayList<>();
    public boolean inWork = false;

    public Area getArea()
    {
        Coord begin = null;
        Coord end = null;
        for (Long id : space.space.keySet())
        {
            MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(id);
            if(grid!=null)
            {
                Area area = space.space.get(id).area;
                Coord b = area.ul.add(grid.ul);
                Coord e = area.br.add(grid.ul);
                begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
                end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
            }
        }
        return new Area(begin,end);
    }

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
                return new Pair<Coord2d, Coord2d>(begin.mul(MCache.tilesz), end.mul(MCache.tilesz).add(MCache.tilesz));
        }
        return null;
    }

    public void tick(double dt)
    {
        if(NUtils.getGameUI().map.nols.get(id)==null && !inWork)
        {
            NUtils.getGameUI().map.addCustomOverlay(id);
        }
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
        res.put("id", id);
        JSONObject jcolor = new JSONObject();
        jcolor.put("r", color.getRed());
        jcolor.put("g", color.getGreen());
        jcolor.put("b", color.getBlue());
        jcolor.put("a", color.getAlpha());
        res.put("color", jcolor);
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
        res.put("in",jin);
        res.put("out",jout);
        JSONArray jspec = new JSONArray();
        for(String s: spec)
        {
            jspec.put(s);
        }
        res.put("spec",jspec);
        this.jspec = jspec;
        return res;
    }
    
    public JSONArray jin = new JSONArray();
    public JSONArray jspec = new JSONArray();
    public JSONArray jout = new JSONArray();
}
