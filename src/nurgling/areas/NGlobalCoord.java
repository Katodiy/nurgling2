package nurgling.areas;

import haven.Coord;
import haven.Coord2d;
import haven.MCache;
import nurgling.NUtils;

import static haven.MCache.cmaps;
import static haven.OCache.posres;

public class NGlobalCoord {
    private Coord oldCoord = null;
    private long grid_id;

    NGlobalCoord(Coord2d coord2d)
    {
        Coord pltc = (new Coord2d(coord2d.x / MCache.tilesz.x, coord2d.y / MCache.tilesz.y)).floor();
        synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
            if (NUtils.getGameUI().ui.sess.glob.map.grids.containsKey(pltc.div(cmaps))) {
                MCache.Grid g = NUtils.getGameUI().ui.sess.glob.map.getgridt(pltc);
                oldCoord = (coord2d.sub(g.ul.mul(Coord2d.of(11, 11)))).floor(posres);
                grid_id = g.id;
            }
        }
    }

    public Coord2d getCurrentCoord()
    {
        if(oldCoord!=null) {
            synchronized (NUtils.getGameUI().ui.sess.glob.map.grids) {
                for (MCache.Grid g : NUtils.getGameUI().ui.sess.glob.map.grids.values()) {
                    if (g.id == grid_id) {
                        return oldCoord.mul(posres).add(g.ul.mul(Coord2d.of(11, 11)));
                    }
                }
            }
        }
        return null;
    }
}
