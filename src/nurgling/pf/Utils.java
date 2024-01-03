package nurgling.pf;

import haven.*;

public class Utils
{
    public static Coord toPfGrid(Coord2d coord, byte scale)
    {
        return coord.mul(scale /4.).div(MCache.tilehsz).round();
    }

    public static Coord2d pfGridToWorld(Coord coord, byte scale)
    {
        return coord.mul(MCache.tilehsz).mul(4./scale);
    }
}
