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
        Coord center = Utils.toPfGrid(a.add((b.sub(a)).div(2)));
        dsize = (int) (b.sub(a).len() / MCache.tilepfsz.x);
        size = 2*dsize;

        cells = new Cell[size][size];
        begin = new Coord(center.x - dsize, center.y - dsize);
        end = new Coord(center.x + dsize, center.y + dsize);
        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                cells[i][j] = new Cell(new Coord(begin.x + i, begin.y+j));
            }
        }
    }

    public void build()
    {
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if (gob.ngob != null && gob.ngob.ca != null)
                {
                    CellsArray ca = gob.ngob.ca;
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
                        else
                            g.chcolor(Color.GREEN);
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
//        try {
//            File file = new File("notes3.txt");
//            file.createNewFile(); // если файл существует - команда игнорируется
//            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
//            Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
//            for ( int i = 0; i < size ; i++ )
//            {
//                for (int j = 0; j < size; j++)
//                {
//                    if (cells[i][j].val == 1)
//                        writer.write("◙");
//                    else
//                        writer.write("⊡");
//                }
//                writer.write('\n');
//            }
//            writer.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
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
