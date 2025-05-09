package nurgling.routes;

import haven.Coord;
import haven.Coord2d;
import haven.MCache;

import java.util.*;

public class RoutePoint {
    public String name;
    public int id;
    public long gridId;
    public Coord localCoord;
    public boolean special = false;

    private ArrayList<Integer> neighbors = new ArrayList<>();

    public RoutePoint(Coord2d rc, MCache mcache) {
        Coord tilec = rc.div(MCache.tilesz).floor();
        MCache.Grid grid = mcache.getgridt(tilec);

        this.gridId = grid.id;
        this.localCoord = tilec.sub(grid.ul);
        this.id = hashCode();
    }

    public RoutePoint(int id, long gridId, Coord localCoord, boolean special) {
        this.name = "";
        this.id = id;
        this.gridId = gridId;
        this.localCoord = localCoord;
        this.special = special;
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

    @Override
    public int hashCode() {
        return Objects.hash(this.gridId, this.localCoord);
    }
}
