package nurgling.pf;

import haven.*;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.tools.Finder;

import java.util.*;

public class Graph implements Runnable
{

    @Override
    public void run() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Vertex v = new Vertex(map.getCells()[i][j].pos, map.getCells()[i][j].val);
                v.dist = Coord.of(i, j).dist(end);
                v.i = i; v.j = j;
                vert[i][j] = v;
            }
        }

        Vertex start = vert[begin.x][begin.y];
        start.len = 0;

        PriorityQueue<Vertex> pq = new PriorityQueue<>(size * size, (a, b) -> Double.compare(a.dist * 100 + a.len, b.dist * 100 + b.len));
        pq.add(start);

        while (!pq.isEmpty()) {
            Vertex cur = pq.poll();
            if (cur.val == 4) continue;
            cur.val = 4;

            if (cur.i == end.x && cur.j == end.y) {
                addToPath(cur);
                return;
            }

            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;

                    int ni = cur.i + di, nj = cur.j + dj;
                    if (ni < 0 || nj < 0 || ni >= size || nj >= size) continue;

                    Vertex n = vert[ni][nj];
                    if ((n.val & 3) != 0) continue;

                    int cost = (di == 0 || dj == 0) ? 100 : 141;
                    int newLen = cur.len + cost;

                    if (newLen < n.len || n.len == -1) {
                        n.len  = newLen;
                        n.prev = cur;
                        if (!pq.contains(n)) pq.add(n);
                    }
                }
            }
        }

        if (vert[end.x][end.y].val != 4) return;
    }

    private boolean addToPath(Vertex v) {
        path.clear();
        for (Vertex u = v; u != null; u = u.prev) {
            path.addFirst(u);
            u.val = 8;
        }
        return true;
    }

    public int getPathLen()
    {
        return vert[end.x][end.y].len != -1 ? vert[end.x][end.y].len : Integer.MAX_VALUE;
    }

    public LinkedList<Vertex> path = new LinkedList<>();

    public static LinkedList<Vertex> getPath(NPFMap map, LinkedList<Vertex> path)
    {
            Coord dir = new Coord();

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
                        Coord2d first = (i != -1) ? Utils.pfGridToWorld(path.get(i).pos) : NUtils.player().rc;
                        Coord2d second = Utils.pfGridToWorld(path.get(i + shift).pos);
                        Coord2d fsdir = second.sub(first);
                        Coord2d center = fsdir.div(2).add(first);
                        int hlen = (int) Math.ceil(fsdir.len() / 2);
                        //TODO remake with beam box
                        NHitBox hb = new NHitBox(new Coord(-hlen,-2 ), new Coord(hlen, 2));
                        ArrayList<Coord> data;
                        if ((data = map.checkCA(new CellsArray(hb, fsdir.curAngle(), center))).isEmpty()) {
                            for_remove.add(path.get(i + shift - 1));
                            shift++;
                            di++;
                        } else {
                            NHitBoxD hbd = new NHitBoxD(hb.begin, hb.end, center, fsdir.curAngle());
                            boolean isFree = true;
                            ArrayList<Coord> corners = new ArrayList<>();
                            for(Coord2d c2d : hbd.c)
                            {
                                corners.add(Utils.toPfGrid(c2d).sub(map.begin));
                            }
                            for(Coord datac : data)
                            {
                                if(corners.contains(datac))
                                {
                                    for (long id : map.cells[datac.x][datac.y].content)
                                    {
                                        Gob g = Finder.findGob(id);
                                        if(g!=null) {
                                            if (hbd.intersects(new NHitBoxD(g.ngob.hitBox.begin, g.ngob.hitBox.end, g.rc, g.a),true)) {
                                                isFree = false;
                                                break;
                                            }
                                        }
                                    }
                                    if(!isFree)
                                        break;
                                }
                                else
                                {
                                    isFree = false;
                                    break;
                                }
                            }

                            if(isFree)
                            {
                                for_remove.add(path.get(i + shift - 1));
                                shift++;
                                di++;
                            }
                            else {
                                shift = 2;
                                i += di;
                                break;
                            }
                        }
                    }
                }
                path.removeAll(for_remove);
//                if (player != null && Utils.pfGridToWorld(path.get(0).pos).dist(player.rc) <= 1)
//                    path.remove(0);
            }
        return path;
    }

    public class Vertex extends NPFMap.Cell
    {
        double dist;
        Vertex prev = null;
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
