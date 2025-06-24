package nurgling.areas;

import haven.*;
import static haven.MCache.cmaps;
import nurgling.*;
import nurgling.actions.PathFinder;
import nurgling.routes.RoutePoint;
import nurgling.tools.*;
import org.json.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NArea
{
    public long gid = Long.MIN_VALUE;
    public String path = "";
    public boolean hide = false;

    public static class Specialisation
    {
        public String name;
        public String subtype = null;

        public Specialisation(String name, String subtype) {
            this.name = name;
            this.subtype = subtype;
        }

        public Specialisation(String name) {
            this.name = name;
        }
    }


    private static class TestedArea
    {
        NArea area;
        double th;

        public TestedArea(NArea area, double th) {
            this.area = area;
            this.th = th;
        }
    }

    static Comparator<TestedArea> ta_comp = new Comparator<TestedArea>(){
        @Override
        public int compare(TestedArea o1, TestedArea o2)
        {
            return Double.compare(o1.th, o2.th);
        }
    };

    boolean containIn(String name)
    {
        for (int i = 0; i < jin.length(); i++)
        {
            if(((String) ((JSONObject)jin.get(i)).get("name")).equals(name))
                return true;
        }
        return false;
    }

    boolean containIn(NAlias name)
    {
        for (int i = 0; i < jin.length(); i++)
        {
            if(NParser.checkName((String) ((JSONObject)jin.get(i)).get("name"),name))
                return true;
        }
        return false;
    }


    boolean containOut(String name, double th)
    {
        for (int i = 0; i < jout.length(); i++) {
            if (((String) ((JSONObject) jout.get(i)).get("name")).equals(name))
                if (((JSONObject) jout.get(i)).has("th") && ((Integer) ((JSONObject) jout.get(i)).get("th")) > th)
                    return true;
                else
                    return true;
        }
        return false;
    }

    private boolean containOut(NAlias name, double th)
    {
        for (int i = 0; i < jout.length(); i++) {
            if (NParser.checkName((String) ((JSONObject) jout.get(i)).get("name"),name))
                if (((JSONObject) jout.get(i)).has("th") && ((Integer) ((JSONObject) jout.get(i)).get("th")) > th)
                    return true;
                else
                    return true;
        }
        return false;
    }

    boolean containOut(String name)
    {
        for (int i = 0; i < jout.length(); i++) {
            if (((String) ((JSONObject) jout.get(i)).get("name")).equals(name))
                    return true;
        }
        return false;
    }

    private boolean containOut(NAlias name)
    {
        for (int i = 0; i < jout.length(); i++) {
            if (NParser.checkName((String) ((JSONObject) jout.get(i)).get("name"),name))
                return true;
        }
        return false;
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
        if(obj.has("path"))
        {
            this.path = obj.getString("path");
        }
        else if(obj.has("dir"))
        {
            this.path = "/" + obj.getString("path");
        }
        if(obj.has("hide")) {
            this.hide = obj.getBoolean("hide");
        }
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
            for(int i = 0 ; i < jspec.length(); i++) {

                String name = (String) ((JSONObject) jspec.get(i)).get("name");
                if (((JSONObject) jspec.get(i)).has("subtype")) {
                    spec.add(new Specialisation(name, (String) ((JSONObject) jspec.get(i)).get("subtype")));
                }
                else
                {
                    spec.add(new Specialisation(name));
                }
            }
        }
    }
    public Space space;
    public String name;
    public int id;
    public Color color = new Color(194,194,65,56);
    public final ArrayList<Long> grids_id = new ArrayList<>();

    public ArrayList<Specialisation> spec = new ArrayList<>();
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
                if(grid==null || hide)
                    return null;
                Area area = space.space.get(id).area;
                Coord b = area.ul.add(grid.ul);
                Coord e = area.br.add(grid.ul);
                begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
                end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
            }
            if (begin != null) {
                if (NUtils.player()!=null && begin.mul(MCache.tilesz).dist(NUtils.player().rc) > 1000 && end.mul(MCache.tilesz).dist(NUtils.player().rc) > 1000) {
                    return null;
                }
                return new Pair<Coord2d, Coord2d>(begin.mul(MCache.tilesz), end.sub(1, 1).mul(MCache.tilesz).add(MCache.tilesz));
            }
        }
        return null;
    }

    public void tick(double dt)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null && NUtils.getGameUI().map.nols.get(id)==null && !inWork)
        {
            NUtils.getGameUI().map.addCustomOverlay(id);
        }
    }

    boolean isVisible() {
        for (Long id : space.space.keySet()) {
            synchronized (NUtils.getGameUI().map.glob.map.grids) {
                for (MCache.Grid g : NUtils.getGameUI().map.glob.map.grids.values()) {
                    if (g.id == id)
                        return true;
                }
            }
        }
        return false;
    }

    public JSONObject toJson()
    {
        JSONObject res = new JSONObject();
        res.put("name", name);
        res.put("id", id);
        res.put("path", path);
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
        res.put("hide",hide);
        JSONArray jspec = new JSONArray();
        for(Specialisation s: spec)
        {
            JSONObject obj = new JSONObject();
            obj.put("name", s.name);
            if(s.subtype!=null)
                obj.put("subtype", s.subtype);
            jspec.put(obj);
        }
        res.put("spec",jspec);
        this.jspec = jspec;
        return res;
    }
    
    public JSONArray jin = new JSONArray();
    public JSONArray jspec = new JSONArray();
    public JSONArray jout = new JSONArray();

    public static class Ingredient
    {
        public static enum Type
        {
            BARTER,
            CONTAINER,
            BARREL, PILE
        }

        public Type type;

        String name;


        public int th = -1;
        public Ingredient(Type type, String name)
        {
            this.type = type;
            this.name = name;
        }

        public Ingredient(Type type, String name, int th)
        {
            this(type,name);
            this.th = th;
        }
    }

    public Ingredient getInput(String name)
    {
        for (int i = 0; i < jin.length(); i++)
        {
            JSONObject obj = (JSONObject)jin.get(i);
            if(((String)((JSONObject)jin.get(i)).get("name")).equals(name))
            {
                NArea.Ingredient.Type type = (obj.has("type")) ?
                        type = NArea.Ingredient.Type.valueOf((String) obj.get("type")) :
                        Ingredient.Type.CONTAINER;
                return new Ingredient(type,name);
            }
        }
        return null;
    }

    public Ingredient getOutput(String name) {
        for (int i = 0; i < jout.length(); i++)
        {
            JSONObject obj = (JSONObject)jout.get(i);
            if(((String)((JSONObject)jout.get(i)).get("name")).equals(name))
            {
                NArea.Ingredient.Type type = (obj.has("type")) ?
                        type = NArea.Ingredient.Type.valueOf((String) obj.get("type")) :
                        Ingredient.Type.CONTAINER;
                if(((JSONObject)jout.get(i)).has("th"))
                {
                    return new Ingredient(type,name, (Integer)((JSONObject)jout.get(i)).get("th"));
                }
                return new Ingredient(type,name);
            }
        }
        return null;
    }



    public ArrayList<Coord2d> getTiles(NAlias name){
        ArrayList<Coord2d> tiles = new ArrayList<>();
        Pair<Coord2d,Coord2d> range = getRCArea();
        Coord2d pos = new Coord2d(range.a.x,range.a.y);
        while ( pos.x < range.b.x ) {
            while ( pos.y < range.b.y ) {
                Coord pltc = ( new Coord2d ( pos.x / MCache.tilesz.x, pos.y / MCache.tilesz.y ) ).floor ();
                Resource res_beg = NUtils.getGameUI().ui.sess.glob.map.tilesetr ( NUtils.getGameUI().ui.sess.glob.map.gettile ( pltc ) );
                if ( NParser.checkName ( res_beg.name, name ) ) {
                    tiles.add(new Coord2d(pos.x, pos.y));
                }
                pos.y += MCache.tilesz.y;
            }
            pos.y = range.a.y;
            pos.x += MCache.tilesz.x;
        }
        return tiles;
    }

}
