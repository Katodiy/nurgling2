package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.pf.*;
import nurgling.pf.Utils;

import java.util.*;
import java.util.concurrent.atomic.*;

public class PathFinder implements Action
{
    public static double pfmdelta = 0.1;
    NPFMap pfmap;
    Coord start_pos;
    Coord end_pos;
    ArrayList<Coord> end_poses;
    public boolean isHardMode = false;

    Coord2d begin;
    Coord2d end;

    public PathFinder(Coord2d begin, Coord2d end)
    {
        this.begin = begin;
        this.end = end;
    }
    public PathFinder(Coord2d end)
    {
        this(NUtils.getGameUI().map.player().rc, end);
    }

    public PathFinder(Gob target)
    {
        this(target.rc);
        target_id = target.id;
    }

    long target_id = -1;
    private void fixStartEnd()
    {
        NPFMap.Cell[][] cells = pfmap.getCells();
        if (cells[start_pos.x][start_pos.y].val != 0)
        {
            start_pos = findFreeNear(start_pos).get(0);
        }
        cells[start_pos.x][start_pos.y].val = 7;
        if (cells[end_pos.x][end_pos.y].val != 0)
        {
            end_poses = findFreeNear(end_pos);
            for (Coord coord : end_poses)
            {
                cells[coord.x][coord.y].val = 7;
            }
        }
        else
        {
            cells[end_pos.x][end_pos.y].val = 7;
        }
    }

    private ArrayList<Coord> findFreeNear(Coord pos)
    {
        ArrayList<Coord> coords = new ArrayList<>();
        Coord posl = new Coord(pos.x - 1, pos.y);
        Coord posb = new Coord(pos.x, pos.y - 1);
        Coord posu = new Coord(pos.x, pos.y + 1);
        Coord posr = new Coord(pos.x + 1, pos.y);

        int delta = 1;
        AtomicBoolean xp = new AtomicBoolean(true);
        AtomicBoolean xm = new AtomicBoolean(true);
        AtomicBoolean yp = new AtomicBoolean(true);
        AtomicBoolean ym = new AtomicBoolean(true);

        boolean andLastCircle = true;
        while ((coords.isEmpty() || andLastCircle) && (xp.get() || xm.get() || yp.get() || ym.get()) && delta < 100)
        {
            if (!coords.isEmpty())
                andLastCircle = false;
            if (posl.x >= 0 && xm.get())
            {
                checkAndAdd(posl, coords, xm);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posl.y > 1)
                            checkAndAdd(new Coord(posl.x, posl.y - i), coords, null);
                        if (posl.y < pfmap.getSize() - 1)
                            checkAndAdd(new Coord(posl.x, posl.y + i), coords, null);
                    }
                }
            }
            posl = new Coord(posl.x - 1, posl.y);
            if (posr.x < pfmap.getSize() - 1 && xp.get())
            {
                checkAndAdd(posr, coords, xp);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posr.y > 1)
                            checkAndAdd(new Coord(posr.x, posr.y - i), coords, null);
                        if (posr.y < pfmap.getSize() - 1)
                            checkAndAdd(new Coord(posr.x, posr.y + i), coords, null);
                    }
                }
            }
            posr = new Coord(posr.x + 1, posr.y);
            if (posb.y >= 0 && ym.get())
            {
                checkAndAdd(posb, coords, ym);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posb.x > i)
                            checkAndAdd(new Coord(posb.x - i, posb.y), coords, null);
                        if (posb.x < pfmap.getSize() - 1)
                            checkAndAdd(new Coord(posb.x + i, posb.y), coords, null);
                    }
                }
            }
            posb = new Coord(posb.x, posb.y - 1);
            if (posu.y < pfmap.getSize() - 1 && yp.get())
            {
                checkAndAdd(posu, coords, yp);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posu.x > i)
                            checkAndAdd(new Coord(posu.x - i, posu.y), coords, null);
                        if (posu.x < pfmap.getSize() - 1)
                            checkAndAdd(new Coord(posu.x + i, posu.y), coords, null);
                    }
                }
            }
            posu = new Coord(posu.x, posu.y + 1);
            delta++;
        }
        return coords;
    }

    private void checkAndAdd(Coord pos, ArrayList<Coord> coords, AtomicBoolean check)
    {
        if (pfmap.getCells()[pos.x][pos.y].val == 0)
        {
            pfmap.getCells()[pos.x][pos.y].val = 7;
            coords.add(pos);
        }
        else if (target_id!=-1 && check!=null)
        {
            if(!pfmap.getCells()[pos.x][pos.y].content.contains(target_id))
                check.set(false);
        }
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        while(true)
        {
            LinkedList<Graph.Vertex> path = construct();
            if (path != null)
            {
                boolean needRestart = false;
                for (Graph.Vertex vert : path)
                {
                    Coord2d targetCoord = Utils.pfGridToWorld(vert.pos);
                    if(!(new GoTo(targetCoord).run(gui)).IsSuccess())
                    {
                        this.begin = gui.map.player().rc;
                        needRestart = true;
                        break;
                    }
                }
                if(!needRestart)
                    return Results.SUCCESS();
            }
            else
            {
                return Results.ERROR("Can't find path");
            }
        }
    }

    public LinkedList<Graph.Vertex> construct() throws InterruptedException
    {
        LinkedList<Graph.Vertex> path = new LinkedList<>();
        int mul = 1;
        while (path.size() == 0 && mul < 5)
        {
            pfmap = new NPFMap(begin, end, mul);
            pfmap.build();
            start_pos = Utils.toPfGrid(begin).sub(pfmap.getBegin());
            end_pos = Utils.toPfGrid(end).sub(pfmap.getBegin());
            // Находим свободные начальные и конечные точки
            fixStartEnd();
            NPFMap.print(pfmap.getSize(), pfmap.getCells());
            Graph res = null;
            if (pfmap.getCells()[end_pos.x][end_pos.y].val == 7)
            {
                Thread th = new Thread(res = new Graph(pfmap, start_pos, end_pos));
                th.start();
                th.join();
            }
            else
            {
                LinkedList<Graph> graphs = new LinkedList<>();
                for (Coord ep : end_poses)
                {
                    graphs.add(new Graph(pfmap, start_pos, ep));
                }
                LinkedList<Thread> threads = new LinkedList<>();
                for (Graph graph : graphs)
                {
                    Thread th;
                    threads.add(th = new Thread(graph));
                    th.start();
                }
                for (Thread t : threads)
                {
                    t.join();
                }

                graphs.sort(new Comparator<Graph>()
                {
                    @Override
                    public int compare(Graph o1, Graph o2)
                    {
                        return (Integer.compare(o1.getPathLen(), o2.getPathLen()));
                    }
                });
                if (graphs.size() > 0)
                    res = graphs.get(0);
            }
            if (res != null)
            {
                NPFMap.print(pfmap.getSize(), res.getVert());
                path = res.getPath();
                if (path.size() > 0)
                    return path;
            }
            mul++;
        }
        return null;
    }


    public static boolean isAvailable(Gob target) throws InterruptedException
    {
        return new PathFinder(target).construct()!=null;
    }

    public static boolean isAvailable(Gob target, boolean hardMode) throws InterruptedException
    {
        PathFinder pf = new PathFinder(target);
        pf.isHardMode = true;
        return pf.construct()!=null;
    }
}
