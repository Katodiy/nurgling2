package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.pf.*;
import nurgling.pf.Utils;

import java.util.*;

public class PathFinder implements Action
{
    public static double pfmdelta = 0.1;
    NPFMap pfmap;
    Coord start_pos;
    Coord end_pos;
    ArrayList<Coord> end_poses;
    boolean isHardMode = false;

    Coord2d begin;
    Coord2d end;

    public PathFinder(Coord2d begin, Coord2d end)
    {
        this.begin = begin;
        this.end = end;
    }

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
        while (coords.isEmpty())
        {
            if (posl.x >= 0)
            {
                checkAndAdd(posl, coords);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posl.y > 1)
                            checkAndAdd(new Coord(posl.x, posl.y - i), coords);
                        if (posl.y < pfmap.getSize())
                            checkAndAdd(new Coord(posl.x, posl.y + i), coords);
                    }
                }
            }
            posl = new Coord(posl.x - 1, posl.y);
            if (posr.x < pfmap.getSize())
            {
                checkAndAdd(posr, coords);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posr.y > i)
                            checkAndAdd(new Coord(posr.x, posr.y - i), coords);
                        if (posr.y < pfmap.getSize())
                            checkAndAdd(new Coord(posr.x, posr.y + i), coords);
                    }
                }
            }
            posr = new Coord(posr.x + 1, posr.y);
            if (posb.y >= 0)
            {
                checkAndAdd(posb, coords);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posb.x > i)
                            checkAndAdd(new Coord(posb.x - i, posb.y), coords);
                        if (posb.x < pfmap.getSize())
                            checkAndAdd(new Coord(posb.x + i, posb.y), coords);
                    }
                }
            }
            posb = new Coord(posb.x, posb.y - 1);
            if (posu.y < pfmap.getSize())
            {
                checkAndAdd(posu, coords);
                if (!isHardMode)
                {
                    for (int i = 1; i <= delta; i++)
                    {
                        if (posu.x > i)
                            checkAndAdd(new Coord(posu.x - i, posu.y), coords);
                        if (posu.x < pfmap.getSize())
                            checkAndAdd(new Coord(posu.x + i, posu.y), coords);
                    }
                }
            }
            posu = new Coord(posu.x, posu.y + 1);
            delta++;
        }
        return coords;
    }

    private void checkAndAdd(Coord pos, ArrayList<Coord> coords)
    {
        if (pfmap.getCells()[pos.x][pos.y].val == 0)
        {
            pfmap.getCells()[pos.x][pos.y].val = 7;
            coords.add(pos);
        }
    }


    PathFinder(Coord2d end)
    {
        this(NUtils.getGameUI().map.player().rc, end);
    }

    PathFinder(Gob target)
    {
        this(target.rc);
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

    LinkedList<Graph.Vertex> construct() throws InterruptedException
    {
        LinkedList<Graph.Vertex> path = new LinkedList<>();
        int mul = 1;
        while (path.size() == 0 && mul < 10)
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
                else
                    mul++;
            }
        }
        return null;
    }
}
