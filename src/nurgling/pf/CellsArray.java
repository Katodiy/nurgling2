package nurgling.pf;

import haven.*;
import nurgling.*;

public class CellsArray
{
    public Coord begin;
    public Coord end;
    public short[][] cells;

    public int x_len;
    public int y_len;

    public CellsArray(Gob gob)
    {
        this(gob.ngob.hitBox, gob.a, gob.rc);
    }


    public CellsArray(int x_len, int y_len)
    {
        this.cells = new short[x_len][y_len];
        this.x_len = x_len;
        this.y_len = y_len;
    }

    public CellsArray(NHitBox hb, double angl, Coord2d rc)
    {
        NHitBoxD objToApproach = new NHitBoxD(hb.begin, hb.end, rc, angl);
        begin = Utils.toPfGrid(objToApproach.getCircumscribedUL());
        NHitBoxD tile = new NHitBoxD(begin);
        end = Utils.toPfGrid(objToApproach.getCircumscribedBR());
        x_len = end.x - begin.x + 1;
        y_len = end.y - begin.y + 1;
        cells = new short[x_len][y_len];
        for (int i = 0; i < x_len; i++)
        {
            for (int j = 0; j < y_len; j++)
            {
                tile.move(begin.add(i,j));
                cells[i][j] = (tile.intersectsLoosely(objToApproach)) ? (short) 1 : 0;
            }
        }
    }
}
