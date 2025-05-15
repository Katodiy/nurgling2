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

    private ArrayList<Integer> neighbors = new ArrayList<>();
    private Map<Integer, Connection> connections = new HashMap<>();

    public class Connection {
        public String connectionTo;
        public String gobHash;
        public String gobName;
        public boolean isDoor;
        
        public Connection(String connectionTo, String gobHash, String gobName, boolean isDoor) {
            this.connectionTo = connectionTo;
            this.gobHash = gobHash;
            this.gobName = gobName;
            this.isDoor = isDoor;
        }
    }

    public RoutePoint(Coord2d rc, MCache mcache) {
        Coord tilec = rc.div(MCache.tilesz).floor();
        MCache.Grid grid = mcache.getgridt(tilec);

        this.gridId = grid.id;
        this.localCoord = tilec.sub(grid.ul);
        this.id = hashCode();
    }

    public RoutePoint(long gridId, Coord localCoord) {
        this.name = "";
        this.gridId = gridId;
        this.localCoord = localCoord;
        this.id = hashCode();
    }

    public void addNeighbor(int id) {
        neighbors.add(id);
    }

    public List<Integer> getNeighbors() {
        return new ArrayList<>(neighbors);
    }

    public void addConnection(int neighborHash, String connectionTo, String gobHash, String gobName, boolean isDoor) {
        connections.put(neighborHash, new Connection(connectionTo, gobHash, gobName, isDoor));
    }

    public Connection getConnection(int neighborHash) {
        return connections.get(neighborHash);
    }

    public Set<Integer> getConnectedNeighbors() {
        return new HashSet<>(connections.keySet());
    }

    public boolean hasConnection(int neighborHash) {
        return connections.containsKey(neighborHash);
    }

    public void removeConnection(int neighborHash) {
        connections.remove(neighborHash);
    }

    public Coord2d toCoord2d(MCache mcache) {
        for (MCache.Grid grid : mcache.grids.values()) {
            if (grid.id == gridId) {
                Coord tilec = grid.ul.add(localCoord);
                return tilec.mul(MCache.tilesz).add(MCache.tilehsz);
            }
        }
        return null;
    }

    public Coord3f toCoord3f(MCache mcache) {
        for (MCache.Grid grid : mcache.grids.values()) {
            if (grid.id == gridId) {
                return mcache.getzp(grid.ul.add(localCoord).mul(MCache.tilesz).add(MCache.tilehsz));
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.gridId, this.localCoord);
    }
}
