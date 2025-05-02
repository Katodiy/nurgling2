package nurgling.routes;

import haven.Coord;
import haven.Coord2d;
import haven.MCache;

public class RoutePoint {
    public String name;
    public long gridId;
    public Coord localCoord;


    public RoutePoint(long gridId, Coord localCoord) {
        this.name = "";
        this.gridId = gridId;
        this.localCoord = localCoord;
    }

    public RoutePoint(Coord2d rc, MCache mcache) {
        Coord tilec = rc.div(MCache.tilesz).floor();
        MCache.Grid grid = mcache.getgridt(tilec);
        this.gridId = grid.id;
        this.localCoord = tilec.sub(grid.ul);
    }

    public Coord2d toCoord2d(MCache mcache) {
        for (MCache.Grid grid : mcache.grids.values()) {
            if (grid.id == gridId) {
                Coord tilec = grid.ul.add(localCoord);
                return tilec.mul(MCache.tilesz);
            }
        }
        return null;
    }
}
