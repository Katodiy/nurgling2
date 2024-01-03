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

//       20.01)) {
//            Coord2d a1 = hb.begin.rotate(angl).shift(rc);
//            Coord a = Utils.toPfGrid(a1, scale);
//            Coord b = Utils.toPfGrid(hb.end.rotate(angl).shift(rc), scale);
//            begin = new Coord(Math.min(a.x, b.x), Math.min(a.y, b.y));
//            end = new Coord(Math.max(a.x, b.x), Math.max(a.y, b.y));
//            x_len = end.x - begin.x;
//            y_len = end.y - begin.y;
//            cells = new short[x_len][y_len];
//            for (int i = 0; i < x_len; i++) {
//                for (int j = 0; j < y_len; j++) {
//                    cells[i][j] = 1;
//                }
//            }
//        } else {
        AreaD objToApproach = new AreaD(hb.end, hb.begin, rc, angl);
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
                AreaD tile = new AreaD(dBegin.add(MCache.tilehsz.mul(i, j)), pos.add(MCache.tilehsz.mul(i + 1, j + 1)));
                cells[i][j] = 0;
                for (int k = 0; k < 4; k++)
                {
                    if (tile.containsLoosely(objToApproach.c[k]) )
                    {
                        cells[i][j] = (short) 1;
                    }
                    if ( objToApproach.containsLoosely(tile.c[k]))
                    {
                        cells[i][j] = (short) 1;
                    }
                }
            }
        }
    }
}
