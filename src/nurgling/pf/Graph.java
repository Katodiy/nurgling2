package nurgling.pf;

import haven.*;
import nurgling.NHitBox;
import nurgling.NUtils;

import java.util.*;

public class Graph implements Runnable
{

    @Override
    public void run()
    {
        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                vert[i][j] = new Vertex(map.getCells()[i][j].pos, map.getCells()[i][j].val);
                Coord c = new Coord(i, j);
                vert[i][j].dist = c.dist(end);
                vert[i][j].i = i;
                vert[i][j].j = j;
            }
        }
        Vertex next = vert[begin.x][begin.y];
        next.len = 0;
        candidates.add(next);
        do
        {
            analise(next);
            candidates.remove(next);

            candidates.sort(new Comparator<Vertex>()
            {
                @Override
                public int compare(Vertex o1, Vertex o2)
                {
                    int res;
                    return (Double.compare(o1.dist * 100 + o1.len, o2.dist * 100 + o2.len));
                    //return ((res = Double.compare(o1.dist, o2.dist)) == 0 ? Integer.compare(o1.len, o2.len) : res);
                }
            });
            if (candidates.size() > 0)
                next = candidates.get(0);
            else
            {
                return;
            }
        }
        while (vert[end.x][end.y].val != 3);
        addToPath(vert[end.x][end.y]);
    }

    private void analise(Vertex v)
    {
        v.val = 3;
        if (v.i > 0)
        {
            if (v.j > 0)
            {
                if ((vert[v.i][v.j - 1].val & 3) == 0 && (vert[v.i - 1][v.j].val & 3) == 0)
                    check(vert[v.i - 1][v.j - 1], v.len + 141);
            }
            check(vert[v.i - 1][v.j], v.len + 100);
            if (v.j < size - 1)
            {
                if ((vert[v.i][v.j + 1].val & 3) == 0 && (vert[v.i - 1][v.j].val & 3) == 0)
                    check(vert[v.i - 1][v.j + 1], v.len + 141);
            }
        }
        if (v.j > 0)
        {
            check(vert[v.i][v.j - 1], v.len + 100);
            if (v.i < size - 1)
            {
                if ((vert[v.i][v.j - 1].val & 3) == 0 && (vert[v.i + 1][v.j].val & 3) == 0)
                    check(vert[v.i + 1][v.j - 1], v.len + 141);
            }
        }
        if (v.i < size - 1)
        {
            check(vert[v.i + 1][v.j], v.len + 100);
            if (v.j < size - 1)
            {
                if ((vert[v.i][v.j + 1].val & 3) == 0 && (vert[v.i + 1][v.j].val & 3) == 0)
                    check(vert[v.i + 1][v.j + 1], v.len + 141);
            }
        }
        if (v.j < size - 1)
        {
            check(vert[v.i][v.j + 1], v.len + 100);
        }
    }

    private void check(Vertex c, int len)
    {
        if (c.val == 0 && c.len == -1)
        {
            c.len = len;
            candidates.add(c);
        }
    }

    public int getPathLen()
    {
        return vert[end.x][end.y].len != -1 ? vert[end.x][end.y].len : Integer.MAX_VALUE;
    }

    public LinkedList<Vertex> path = new LinkedList<>();

    boolean addToPath(Vertex v)
    {
        path.add(0, v);
        v.val = 8;
        if (v.len == 0)
            return true;
        if (v.i > 0)
        {
            if (v.j > 0)
            {
                if (checkPath(vert[v.i - 1][v.j - 1], v.len - 141))
                    return addToPath(vert[v.i - 1][v.j - 1]);
            }
            if (checkPath(vert[v.i - 1][v.j], v.len - 100))
                return addToPath(vert[v.i - 1][v.j]);
            if (v.j < size - 1)
            {
                if (checkPath(vert[v.i - 1][v.j + 1], v.len - 141))
                    return addToPath(vert[v.i - 1][v.j + 1]);
            }
        }
        if (v.j > 0)
        {
            if (checkPath(vert[v.i][v.j - 1], v.len - 100))
                return addToPath(vert[v.i][v.j - 1]);
            if (v.i < size - 1)
            {
                if (checkPath(vert[v.i + 1][v.j - 1], v.len - 141))
                    return addToPath(vert[v.i + 1][v.j - 1]);
            }
        }
        if (v.i < size - 1)
        {
            if (checkPath(vert[v.i + 1][v.j], v.len - 100))
                return addToPath(vert[v.i + 1][v.j]);
            if (v.j < size - 1)
            {
                if (checkPath(vert[v.i + 1][v.j + 1], v.len - 141))
                    return addToPath(vert[v.i + 1][v.j + 1]);
            }
        }
        if (v.j < size - 1)
        {
            if (checkPath(vert[v.i][v.j + 1], v.len - 100))
                return addToPath(vert[v.i][v.j + 1]);
        }
        return false;
    }

    private boolean checkPath(Vertex c, int len)
    {
        return c.val == 3 && c.len == len;
    }

    public static LinkedList<Vertex> getPath(NPFMap map, LinkedList<Vertex> path)
    {
            Coord dir = new Coord();
            Gob player = NUtils.player();

            if (!path.isEmpty()) {
                Iterator<Vertex> it;
                it = path.iterator();
                Vertex prev = it.next();
                prev.val = 9;
                LinkedList<Vertex> for_remove = new LinkedList<>();
                while (it.hasNext()) {
                    Vertex cur = it.next();
                    Coord cur_dir = new Coord(cur.i, cur.j).sub(new Coord(prev.i, prev.j));
                    if (cur_dir.equals(dir.x, dir.y))
                        for_remove.add(prev);
                    else {
                        prev.val = 9;
                        dir = cur_dir;
                    }
                    prev = cur;
                }
                path.removeAll(for_remove);
            }
            if (!path.isEmpty()) {
                LinkedList<Vertex> for_remove = new LinkedList<>();
                int shift = 2;

                for (int i = -1; i < path.size(); i++) {
                    int di = 0;
                    while (i + shift < path.size()) {
                        Coord2d first = (i != -1) ? Utils.pfGridToWorld(path.get(i).pos, (byte) 4) : NUtils.player().rc;
                        Coord2d second = Utils.pfGridToWorld(path.get(i + shift).pos, (byte) 4);
                        Coord2d fsdir = second.sub(first);
                        Coord2d center = fsdir.div(2).add(first);
                        int hlen = (int) Math.ceil(fsdir.len() / 2);
                        NHitBox hb = new NHitBox(new Coord(-2, -hlen), new Coord(2, hlen));

                        if (map.checkCA(new CellsArray(hb, fsdir.curAngle(), center, (byte) 4))) {
                            for_remove.add(path.get(i + shift - 1));
                            shift++;
                            di++;
                        } else {
                            shift = 2;
                            i += di;
                            break;
                        }
                    }
                }
                path.removeAll(for_remove);
                if (player != null && Utils.pfGridToWorld(path.get(0).pos, (byte) 4).dist(player.rc) <= 1)
                    path.remove(0);
            }
        return path;
    }

    public class Vertex extends NPFMap.Cell
    {
        double dist;
        int len = -1;

        public int i;
        public int j;

        public Vertex(Coord pos, short val)
        {
            super(pos);
            this.val = (val == 7) ? 0 : val;
        }
    }

    public Vertex[][] getVert()
    {
        return vert;
    }

    LinkedList<Vertex> candidates = new LinkedList<>();

    Vertex[][] vert;

    final NPFMap map;

    final Coord begin, end;
    int size;

    public Graph(NPFMap map, Coord begin, Coord end)
    {
        size = map.getSize();
        vert = new Vertex[map.getSize()][map.getSize()];
        this.map = map;
        this.begin = begin;
        this.end = end;
    }
}
