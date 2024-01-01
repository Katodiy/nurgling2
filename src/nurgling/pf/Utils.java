package nurgling.pf;

import haven.*;

public class Utils
{
    public static Coord toPfGrid(Coord2d coord)
    {
        return coord.sub(MCache.tileqsz).div(MCache.tilehsz).floor();
    }

    public static Coord2d pfGridToWorld(Coord coord)
    {
        return coord.mul(MCache.tilehsz).add(MCache.tilehsz);
    }
}
