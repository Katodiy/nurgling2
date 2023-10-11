package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.pf.*;
import nurgling.pf.Utils;

import java.util.*;

public class PathFinder implements Action
{
    NPFMap pfmap;
    Coord start_pos;
    Coord end_pos;
    boolean isHardMode = false;

    public PathFinder(Coord2d begin, Coord2d end)
    {
        pfmap = new NPFMap(begin,end);
        pfmap.build();
        start_pos = Utils.toPfGrid(begin).sub(pfmap.getBegin());
        end_pos = Utils.toPfGrid(end).sub(pfmap.getBegin());
        fixStartEnd();
        pfmap.print();
    }

    private void fixStartEnd()
    {
        NPFMap.Cell[][] cells = pfmap.getCells();
        if(cells[start_pos.x][start_pos.y].val!=0)
        {
            start_pos = findFreeNear(start_pos).get(0);
        }
        cells[start_pos.x][start_pos.y].val = 7;
        if(cells[end_pos.x][end_pos.y].val!=0)
        {
            ArrayList<Coord> coords = findFreeNear(end_pos);
            for(Coord coord: coords)
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
        this(NUtils.getGameUI().map.player().rc,end);
    }

    PathFinder(Gob target)
    {
        this(target.rc);
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

        return Results.SUCCESS();
    }
}
