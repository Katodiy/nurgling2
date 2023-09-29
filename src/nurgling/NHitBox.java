package nurgling;

import haven.*;

public class NHitBox
{
    Coord2d begin, end;
    NHitBox(Coord begin, Coord end)
    {
        this.begin = new Coord2d(begin);
        this.end = new Coord2d(end);
    }

    public static NHitBox findCustom(String name)
    {
        return null;
    }
}
