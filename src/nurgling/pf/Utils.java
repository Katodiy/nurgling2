package nurgling.pf;

import haven.*;

public class Utils
{
    public static Coord toPfGrid(Coord2d coord)
    {
        return coord.div(MCache.tilepfsz).floor();
    }
}
