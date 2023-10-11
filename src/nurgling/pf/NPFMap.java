package nurgling.pf;

import haven.*;
import haven.Window;
import nurgling.*;

import java.awt.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class NPFMap
{
    public class Cell{
        public Cell(Coord pos)
        {
            this.pos = pos;
        }

        public Coord pos = new Coord();
        public short val;
    }

    Cell[][] cells;

    Coord begin, end;
    int dsize;
    int size;

    public NPFMap(Coord2d a, Coord2d b)
    {
        // Последнее деление умножение нужно чтобы сопоставить сетку пф с сеткой лофтара по углу (ускорение запроса поверхности тайлов)
        Coord center = Utils.toPfGrid(a.add((b.sub(a)).div(2))).div(2).mul(2);
        dsize = (((int) ((b.sub(a).len() / MCache.tilepfsz.x)))/2)*2;
        size = 2 * dsize;

        cells = new Cell[size][size];
        begin = new Coord(center.x - dsize, center.y - dsize);
        end = new Coord(center.x + dsize, center.y + dsize);
        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                cells[i][j] = new Cell(new Coord(begin.x + i, begin.y + j));
            }
        }
    }

    public void build()
    {
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {
            CellsArray ca;
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if (gob.ngob != null && gob.ngob.hitBox != null && (ca = gob.ngob.getCA()) != null)
                {
                    if ((ca.begin.x >= begin.x && ca.begin.x <= end.x ||
                            ca.end.x >= begin.x && ca.end.x <= end.x) &&
                            (ca.begin.y >= begin.y && ca.begin.y <= end.y ||
                                    ca.end.y >= begin.y && ca.end.y <= end.y))
                    {
                        for (int i = 0; i < ca.x_len; i++)
                            for (int j = 0; j < ca.y_len; j++)
                            {
                                int ii = i + ca.begin.x - begin.x;
                                int jj = j + ca.begin.y - begin.y;
                                if (ii > 0 && ii < size && jj > 0 && jj < size)
                                {
                                    cells[ii][jj].val = ca.cells[i][j];
                                }
                            }
                    }
                }
            }
        }
        for (int i = 0; i < size; i += 2)
        {
            for (int j = 0; j < size; j += 2)
            {

                if (cells[i][j].val == 0)
                {
                    String name = NUtils.getGameUI().ui.sess.glob.map.tilesetname(NUtils.getGameUI().ui.sess.glob.map.gettile(cells[i][j].pos.div(2)));
                    if (name != null && (name.startsWith("gfx/tiles/cave") || name.startsWith("gfx/tiles/rocks") || name.equals("gfx/tiles/deep") || name.equals("gfx/tiles/odeep")))
                    {
                        cells[i][j].val = 2;
                        cells[i+1][j].val = 2;
                        cells[i][j+1].val = 2;
                        cells[i+1][j+1].val = 2;
                    }
                }
            }
        }
        print();
    }


    public void print (){
        Coord csz = new Coord(UI.scale(10),UI.scale(10));
        Window wnd = NUtils.getUI().root.add(new Window(new Coord(size*UI.scale(10),size*UI.scale(10)),"PFMAP"){
            @Override
            public void draw(GOut g)
            {
                super.draw(g);
                for ( int i = 0; i < size ; i++ )
                {
                    for (int j = size-1; j >= 0; j--)
                    {
                        if (cells[i][j].val == 1)
                            g.chcolor(Color.RED);
                        else if (cells[i][j].val == 0)
                            g.chcolor(Color.GREEN);
                        else
                            g.chcolor(Color.BLACK);
                        g.rect(new Coord(i*UI.scale(10),j*UI.scale(10)).add(deco.contarea().ul),csz);
                    }
                }
            }

            public void wdgmsg(Widget sender, String msg, Object... args) {
                if((sender == this) && (msg == "close")) {
                    destroy();
                } else {
                    super.wdgmsg(sender, msg, args);
                }
            }

        }, new Coord(UI.scale(100),UI.scale(100)));
        NUtils.getUI().bind(wnd, 7002);
    }

    public PFGraph getGraph()
    {
        return null;
    }

    class PFGraph
    {
        class Vertex{
            // Нужен только список соседей в которые можно пойти
            ArrayList<Long> neighbours = new ArrayList<>();
            boolean isVisited;
            double distance;
        }

        HashMap<Long,Vertex> data = new HashMap();
    }
}
