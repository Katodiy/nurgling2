package nurgling.pf;

import haven.*;
import nurgling.*;

import java.util.*;

public class CellsArray
{
    Coord begin;
    Coord end;
    public short [][] cells;

    int x_len;
    int y_len;

    public CellsArray(Gob gob)
    {
        // DON`T USE IT WITHOUT HITBOXES!!!!
        assert (gob.ngob.hitBox != null);
        NHitBox hb = gob.ngob.hitBox;


        if ((Math.abs((gob.a * 180 / Math.PI) / 90) % 1 < 0.01))
        {
            Coord2d a1 = hb.begin.rotate(gob.a).shift(gob.rc);
            Coord a = Utils.toPfGrid(a1);
            Coord b = Utils.toPfGrid(hb.end.rotate(gob.a).shift(gob.rc));
            begin = new Coord(Math.min(a.x, b.x), Math.min(a.y, b.y));
            end = new Coord(Math.max(a.x, b.x), Math.max(a.y, b.y));
            x_len = end.x - begin.x + 1;
            y_len = end.y - begin.y + 1;
            cells = new short[x_len][y_len];
            for (int i = 0; i < x_len; i++)
            {
                for (int j = 0; j < y_len; j++)
                {
                    cells[i][j] = 1;
                }
            }
        }
        else
        {
            Coord2d ad = hb.begin.rotate(gob.a).shift(gob.rc);
            Coord2d bd = new Coord2d(hb.end.x, hb.begin.y).rotate(gob.a).shift(gob.rc);
            Coord2d cd = hb.end.rotate(gob.a).shift(gob.rc);
            Coord2d dd = new Coord2d(hb.begin.x, hb.end.y).rotate(gob.a).shift(gob.rc);
            Coord a = Utils.toPfGrid(ad);
            Coord b = Utils.toPfGrid(bd);
            Coord c = Utils.toPfGrid(cd);
            Coord d = Utils.toPfGrid(dd);
            begin = new Coord(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)), Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)));
            end = new Coord(Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)), Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)));
            x_len = end.x - begin.x + 1;
            y_len = end.y - begin.y + 1;
            cells = new short[x_len][y_len];
            Coord2d start = begin.mul(MCache.tilepfsz);
            Coord2d pos = new Coord2d(start.x, start.y);
            Coord2d a_b = bd.sub(ad);
            Coord2d b_c = cd.sub(bd);
            Coord2d c_d = dd.sub(cd);
            Coord2d d_a = ad.sub(dd);
            short[][] dcells = new short[x_len + 1][y_len + 1];
            for (int i = 0; i < x_len + 1; i++)
            {
                for (int j = 0; j < y_len + 1; j++)
                {
                    dcells[i][j] = (short) ((a_b.dot(pos.sub(ad)) >= 0 && b_c.dot(pos.sub(bd)) >= 0 && c_d.dot(pos.sub(cd)) >= 0 && d_a.dot(pos.sub(dd)) >= 0) ? 1 : 0);
                    pos.y += MCache.tilepfsz.y;
                }
                pos.x += MCache.tilepfsz.x;
                pos.y = start.y;
            }
            for (int i = 0; i < x_len; i++)
            {
                for (int j = 0; j < y_len; j++)
                {
                    cells[i][j] = (short) ((dcells[i][j] == 1 || dcells[i + 1][j] == 1 || dcells[i][j + 1] == 1 || dcells[i + 1][j + 1] == 1) ? 1 : 0);
                }
            }
            cells[a.x -begin.x][a.y -begin.y] = 1;
            cells[b.x -begin.x][b.y -begin.y] = 1;
            cells[c.x -begin.x][c.y -begin.y] = 1;
            cells[d.x -begin.x][d.y -begin.y] = 1;
        }
    }
}
