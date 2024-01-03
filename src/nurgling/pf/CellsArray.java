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

    public CellsArray(Gob gob, byte scale)
    {
        this(gob.ngob.hitBox, gob.a, gob.rc, scale);
    }


    public CellsArray(NHitBox hb, double angl, Coord2d rc, byte scale)
    {
        NHitBoxD objToApproach = new NHitBoxD(hb.begin, hb.end, rc, angl);
        begin = Utils.toPfGrid(objToApproach.getCircumscribedUL(), scale);
        Coord2d dBegin = Utils.pfGridToWorld(begin, scale).sub(MCache.tileqsz);
        end = Utils.toPfGrid(objToApproach.getCircumscribedBR(), scale);
        x_len = end.x - begin.x + 1;
        y_len = end.y - begin.y + 1;
        cells = new short[x_len][y_len];
        Coord2d pos = Coord2d.of(dBegin.x, dBegin.y);
        for (int i = 0; i < x_len; i++)
        {
            for (int j = 0; j < y_len; j++)
            {
                NHitBoxD tile = new NHitBoxD(dBegin.add(MCache.tilehsz.mul(i, j)), pos.add(MCache.tilehsz.mul(i + 1, j + 1)));
                cells[i][j] = (tile.intersectsLoosely(objToApproach))? (short)1:0;
            }
        }
    }
}
