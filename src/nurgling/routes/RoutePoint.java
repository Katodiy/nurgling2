package nurgling.routes;

import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.PathFinder;
import nurgling.areas.NArea;
import nurgling.tools.Finder;

import java.util.*;

public class RoutePoint {
    public String name;
    public int id;
    public long gridId;
    public Coord localCoord;
    public String hearthFirePlayerName;

    public ArrayList<Integer> neighbors = new ArrayList<>();
    public Map<Integer, Connection> connections = new HashMap<>();
    private HashMap<Integer,Double> areasDistance = new HashMap<>();

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
        this(rc, mcache, null);
    }

    public RoutePoint(Coord2d rc, MCache mcache, String hearthFirePlayerName) {
        Coord tilec = rc.div(MCache.tilesz).floor();
        MCache.Grid grid = mcache.getgridt(tilec);

        this.gridId = grid.id;
        this.localCoord = tilec.sub(grid.ul);
        this.hearthFirePlayerName = hearthFirePlayerName;
        this.id = hashCode();
    }


    public RoutePoint(long gridId, Coord localCoord, String hearthFirePlayerName) {
        this.name = "";
        this.gridId = gridId;
        this.localCoord = localCoord;
        this.hearthFirePlayerName = hearthFirePlayerName;
        this.id = hashCode();
    }

    public void addNeighbor(int id) {
        if(!neighbors.contains(Integer.valueOf(id))) {
            neighbors.add(id);
        }
    }

    public List<Integer> getNeighbors() {
        return new ArrayList<>(neighbors);
    }

    public void addConnection(int neighborHash, String connectionTo, String gobHash, String gobName, boolean isDoor) {
        connections.put(neighborHash, new Connection(connectionTo, gobHash, gobName, isDoor));
    }

    public void addConnection(int neighborHash, Connection connection) {
        connections.put(neighborHash, connection);
    }

    public Connection getConnection(int neighborHash) {
        return connections.get(neighborHash);
    }

    public Set<Integer> getConnectedNeighbors() {
        return new HashSet<>(connections.keySet());
    }

    public void removeConnection(int neighborHash) {
        connections.remove(neighborHash);
    }

    public void setLocalCoord(Coord localCoord) {
        this.localCoord = localCoord;
        this.id = hashCode();
    }

    public void setGridId(long gridId) {
        this.gridId = gridId;
    }

    public HashSet<Integer> getReachableAreas() {
        return new HashSet<>(areasDistance.keySet());
    }

    public void addReachableAreas() throws InterruptedException {
        for (NArea area : NUtils.getGameUI().map.glob.map.areas.values()) {
            Pair<Coord2d, Coord2d> rcArea = area.getRCArea();
            if (rcArea == null) continue;

            Coord2d myrc = toCoord2d(NUtils.getGameUI().map.glob.map);
            if (myrc == null) continue;

            double distance = area.getDistance(myrc);
            if (distance > 250) continue;

            ArrayList<Gob> gobs = Finder.findGobs(area);
            boolean isReachable = false;

            if (gobs.isEmpty()) {
                isReachable = PathFinder.isAvailable(area.getCenter2d());
            } else {
                for (Gob gob : gobs) {
                    if (PathFinder.isAvailable(gob)) {
                        isReachable = true;
                        break;
                    }
                }
            }

            if (isReachable) {
                areasDistance.put(area.id, distance);
            }
        }
    }

    public void addReachableArea(int reachableAreas, double distance) {
        this.areasDistance.put(reachableAreas,distance);

    }

    public void deleteReachableArea(Integer areaId) {
        areasDistance.remove(areaId);
    }

    public Coord2d toCoord2d(MCache mcache) {
        synchronized(mcache.grids) {
            for (MCache.Grid grid : mcache.grids.values()) {
                if (grid.id == gridId) {
                    Coord tilec = grid.ul.add(localCoord);
                    return tilec.mul(MCache.tilesz).add(MCache.tilehsz);
                }
            }
            return null;
        }
    }

    public Coord3f toCoord3f(MCache mcache) {
        synchronized (mcache.grids) {
            for (MCache.Grid grid : mcache.grids.values()) {
                if (grid.id == gridId) {
                    boolean canContinue = false;
                    for(MCache.Grid.Cut cut : grid.cuts) {
                        canContinue = cut.mesh.isReady() && cut.fo.isReady();
                    }
                    if (canContinue) {
                        return mcache.getzp(grid.ul.add(localCoord).mul(MCache.tilesz).add(MCache.tilehsz));
                    }
                }
            }
            return null;
        }
    }

    public Collection<Connection> getConnections() {
        return this.connections.values();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.gridId, this.localCoord);
    }

    public int updateHashCode() {
        int newHashCode = Objects.hash(this.gridId, this.localCoord);

        if(this.id != newHashCode) {
            ((NMapView) NUtils.getGameUI().map).routeGraphManager.updateConnections(this, newHashCode);
            this.id = newHashCode;
        }
        return newHashCode;
    }

    public double getDistanceToArea(int reachableArea) {
        return areasDistance.get(reachableArea);
    }
}
