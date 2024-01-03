package nurgling.pf;

import haven.*;
import haven.Window;
import nurgling.*;

import java.awt.*;
import java.util.*;

import static nurgling.actions.PathFinder.scale;

public class NPFMap
{
    public CellsArray addGob(Gob gob) {
        CellsArray ca;

        if (gob.ngob != null && gob.ngob.hitBox != null && (ca = gob.ngob.getCA()) != null && NUtils.player()!=null && gob.id!=NUtils.player().id && gob.getattr(Following.class) == null)
        {
            CellsArray old = new CellsArray(ca.x_len,ca.y_len);
            old.begin = ca.begin;
            old.end = ca.end;
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
                            old.cells[i][j] = cells[ii][jj].val;
                            cells[ii][jj].val = ca.cells[i][j];
                            if(ca.cells[i][j]!=0)
                            {
                                cells[ii][jj].content.add(gob.id);
                            }
                        }
                    }
            }
            return old;
        }
        return null;
    }

    public void setCellArray(CellsArray ca) {

        if ((ca.begin.x >= begin.x && ca.begin.x <= end.x ||
                ca.end.x >= begin.x && ca.end.x <= end.x) &&
                (ca.begin.y >= begin.y && ca.begin.y <= end.y ||
                        ca.end.y >= begin.y && ca.end.y <= end.y)) {
            for (int i = 0; i < ca.x_len; i++)
                for (int j = 0; j < ca.y_len; j++) {
                    int ii = i + ca.begin.x - begin.x;
                    int jj = j + ca.begin.y - begin.y;
                    if (ii > 0 && ii < size && jj > 0 && jj < size) {
                        cells[ii][jj].val = ca.cells[i][j];
                    }
                }
        }
    }


    public static class Cell
    {
        public Cell(Coord pos)
        {
            this.pos = pos;
        }

        public Coord pos = new Coord();
        public short val;
        public ArrayList<Long> content = new ArrayList<>();
    }

    public Cell[][] cells;
    public Coord begin;
    Coord end;
    int dsize;
    public int size;

    public NPFMap(Coord2d src, Coord2d dst, int mul)
    {
        Coord2d a = new Coord2d(Math.min(src.x, dst.x), Math.min(src.y, dst.y));
        Coord2d b = new Coord2d(Math.max(src.x, dst.x), Math.max(src.y, dst.y));
        Coord center = Utils.toPfGrid(a.add((b.sub(a)).div(2)),scale);
        dsize = Math.max(8,(((int) ((b.sub(a).len() / MCache.tilehsz.x))) / 2) * 2 * mul);
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


    public Coord getBegin()
    {
        return begin;
    }

    public Coord getEnd()
    {
        return end;
    }

    public Cell[][] getCells()
    {
        return cells;
    }

    public int getSize()
    {
        return size;
    }

    public void build()
    {
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {

            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
               addGob(gob);
            }
        }
        for (int i = 0; i < size; i += 1)
        {
            for (int j = 0; j < size; j += 1)
            {

                if (cells[i][j].val == 0)
                {
                    ArrayList<Coord> cand = new ArrayList<>();
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos,scale).add(new Coord2d(-MCache.tileqsz.x,MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos,scale).add(new Coord2d(MCache.tileqsz.x,-MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos,scale).add(new Coord2d(-MCache.tileqsz.x,-MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos,scale).add(new Coord2d(MCache.tileqsz.x,MCache.tileqsz.y))).div(MCache.tilesz).floor());

                    for(Coord c : cand) {
                        String name = NUtils.getGameUI().ui.sess.glob.map.tilesetname(NUtils.getGameUI().ui.sess.glob.map.gettile(c));

                        if (name != null && (name.startsWith("gfx/tiles/cave") || name.startsWith("gfx/tiles/rocks") || name.equals("gfx/tiles/deep") || name.equals("gfx/tiles/odeep"))) {
                            cells[i][j].val = 2;
                        }
                    }
                }
            }
        }
    }

    public boolean checkCA(CellsArray ca) {
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
                        if(ca.cells[i][j] != 0 && cells[ii][jj].val !=0)
                        {
                            return false;
                        }
                    }
                }
            return true;
        }
        return false;
    }

    public static Window wnd = null;
    public static void print(int size, Cell[][] cells)
    {
        if(NUtils.getUI().core.debug)
        {
            Coord csz = new Coord(UI.scale(10), UI.scale(10));
            if(wnd!=null)
                wnd.destroy();
            wnd = NUtils.getUI().root.add(new Window(new Coord(size * UI.scale(10), size * UI.scale(10)), "PFMAP")
            {
                @Override
                public void draw(GOut g)
                {
                    super.draw(g);
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = size - 1; j >= 0; j--)
                        {
                            if (cells[i][j].val == 1)
                                g.chcolor(Color.RED);
                            else if (cells[i][j].val == 0)
                                g.chcolor(Color.GREEN);
                            else if (cells[i][j].val == 3)
                            {
                                g.chcolor(Color.YELLOW);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 7)
                                g.chcolor(Color.BLUE);
                            else if (cells[i][j].val == 8)
                            {
                                g.chcolor(Color.MAGENTA);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 9)
                            {
                                g.chcolor(Color.CYAN);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else
                                g.chcolor(Color.BLACK);
                            g.rect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                        }
                    }
                }

                public void wdgmsg(Widget sender, String msg, Object... args)
                {
                    if ((sender == this) && (msg == "close"))
                    {
                        destroy();
                    }
                    else
                    {
                        super.wdgmsg(sender, msg, args);
                    }
                }

            }, new Coord(UI.scale(100), UI.scale(100)));
            NUtils.getUI().bind(wnd, 7002);
        }
    }
}
