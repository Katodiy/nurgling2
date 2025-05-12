package nurgling.routes;

import haven.Coord;
import haven.Coord2d;
import haven.Coord3f;
import haven.MCache;

import java.util.*;

public class RoutePoint {
    public String name;
    public int id;
    public long gridId;
    public Coord localCoord;
    public boolean isDoor = false;
    public String gobHash = "";

    private ArrayList<Integer> neighbors = new ArrayList<>();

    public RoutePoint(Coord2d rc, MCache mcache) {
        Coord tilec = rc.div(MCache.tilesz).floor();
        MCache.Grid grid = mcache.getgridt(tilec);

        this.gridId = grid.id;
        this.localCoord = tilec.sub(grid.ul);
        this.id = hashCode();
    }

    public RoutePoint(long gridId, Coord localCoord, boolean isDoor, String gobHash) {
        this.name = "";
        this.gridId = gridId;
        this.localCoord = localCoord;
        this.isDoor = isDoor;
        this.gobHash = gobHash;
        this.id = hashCode();
    }

    public void addNeighbor(int id) {
        neighbors.add(id);
    }

    public List<Integer> getNeighbors() {
        return new ArrayList<>(neighbors);
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

    public Coord3f toCoord3f(MCache mcache) {
        for (MCache.Grid grid : mcache.grids.values()) {
            if (grid.id == gridId) {
                return mcache.getzp(grid.ul.add(localCoord).mul(MCache.tilesz));
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.gridId, this.localCoord);
    }
}
