package nurgling.pf;

import haven.*;
import nurgling.NUtils;

public class Utils
{
    public static Coord toPfGrid(Coord2d coord)
    {
        return coord.div(MCache.tilehsz).round();
    }

    public static Coord2d pfGridToWorld(Coord coord)
    {
        return coord.mul(MCache.tilehsz);
    }

    public static boolean inVisibleArea(Coord2d coord2d) {
        Gob player = NUtils.player();
        if(player!=null) {
            Coord2d cc = player.rc;
            Coord2d cmap = new Coord2d(MCache.cmaps);
            Coord2d fixator = cc.floor(cmap).mul(cmap).add(cmap.div(2));
            Coord2d ul = fixator.add(450,450);
            Coord2d br = fixator.sub(450,450);
            return coord2d.x >= br.x && coord2d.y >= br.y && coord2d.x <= ul.x && coord2d.y <= ul.y;
        }
        return false;
    }
}
