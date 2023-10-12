package nurgling.pf;

import haven.*;

public class Utils
{
    public static Coord toPfGrid(Coord2d coord)
    {
        return coord.div(MCache.tilepfsz).floor();
    }

    public static Coord2d pfGridToWorld(Coord coord)
    {
        return coord.mul(MCache.tilepfsz).add(MCache.dtilepfsz);
    }
}
