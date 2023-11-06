package nurgling.areas;

import haven.*;
import nurgling.*;
import nurgling.tools.*;

import java.util.*;

public class NArea
{
    public static class Space
    {
        private final int max = 99;
        private final int min = 0;
        HashMap<Long,Area> space = new HashMap<>();
        public Space(Coord sc, Coord ec)
        {
            Coord begin = new Coord(Math.min(sc.x, ec.x), Math.min(sc.y, ec.y));
            Coord end = new Coord(Math.max(sc.x, ec.x), Math.max(sc.y, ec.y));
            Coord bd = begin.div(MCache.cmaps);
            Coord ed = end.div(MCache.cmaps);
            Coord bm = begin.mod(MCache.cmaps);
            Coord em = end.mod(MCache.cmaps);
            if (bd.equals(ed.x,ed.y))
            {
                MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                space.put(grid.id, new Area(bm, em));
            }
            else
            {
                if (bd.x != ed.x && bd.y != ed.y)
                {
                    MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                    space.put(grid.id, new Area(bm, new Coord(max,max)));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(bd.x, ed.y));
                    space.put(grid.id, new Area(new Coord(bm.x, min), new Coord(max, em.y)));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(ed.x, bd.y));
                    space.put(grid.id, new Area(new Coord(min, bm.y), new Coord(em.x, max)));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(ed);
                    space.put(grid.id, new Area(new Coord(min, min), em));
                }
                else if (bd.x != ed.x)
                {
                    MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                    space.put(grid.id, new Area(bm, new Coord(max, em.y)));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(ed.x, bd.y));
                    space.put(grid.id, new Area(new Coord(min, bm.y), em));
                }
                else
                {
                    MCache.Grid grid = NUtils.getGameUI().map.glob.map.grids.get(bd);
                    space.put(grid.id, new Area(bm, new Coord(em.x, max)));
                    grid = NUtils.getGameUI().map.glob.map.grids.get(new Coord(bd.x, ed.y));
                    space.put(grid.id, new Area(new Coord(bm.x, min), em));
                }
            }
        }
    }
    public Space space;
    String name;
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

    public NArea(String name)
    {
        this.name = name;
    }
}
