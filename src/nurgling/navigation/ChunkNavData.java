package nurgling.navigation;

import haven.Coord;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Stores navigation data for a single 100x100 tile grid chunk.
 */
public class ChunkNavData {
    // Identity
    public long gridId;
    public long lastUpdated;        // System.currentTimeMillis()
    public float confidence;        // 0.0 - 1.0, decays over time

    // Grid position (for spatial queries)
    public Coord gridCoord;         // Grid coordinate (grid.gc) - SESSION-BASED, unreliable across sessions
    public Coord worldTileOrigin;   // Actual world tile origin (grid.ul) - SESSION-BASED, unreliable across sessions

    // Neighbor relationships (PERSISTENT - grid IDs never change)
    // These are discovered when grids are loaded together and we can see their spatial relationship
    public long neighborNorth = -1;  // Grid ID of neighbor to the north (gc.y - 1)
    public long neighborSouth = -1;  // Grid ID of neighbor to the south (gc.y + 1)
    public long neighborEast = -1;   // Grid ID of neighbor to the east (gc.x + 1)
    public long neighborWest = -1;   // Grid ID of neighbor to the west (gc.x - 1)

    // Layer identifier - different physical spaces that may share coordinates
    // "surface" = outside world (default), "inside" = building interior, "cellar" = underground cellar, etc.
    public String layer = "surface";

    // Walkability grid (tile-level resolution)
    // Each cell represents 1 tile (100x100 grid for 100x100 chunk)
    // Values: 0 = walkable, 1 = partially blocked, 2 = fully blocked
    public byte[][] walkability = new byte[CELLS_PER_EDGE][CELLS_PER_EDGE];

    // Observed grid - tracks which tiles have been visually observed
    // true = tile was within visible range when recorded, false = not yet observed
    public boolean[][] observed = new boolean[CELLS_PER_EDGE][CELLS_PER_EDGE];

    // Edge connections to adjacent chunks
    // Each array has 100 entries (one per tile along edge)
    public EdgePoint[] northEdge = new EdgePoint[CELLS_PER_EDGE];
    public EdgePoint[] southEdge = new EdgePoint[CELLS_PER_EDGE];
    public EdgePoint[] eastEdge = new EdgePoint[CELLS_PER_EDGE];
    public EdgePoint[] westEdge = new EdgePoint[CELLS_PER_EDGE];

    // Portals (doors, stairs, cellars) within this chunk
    public List<ChunkPortal> portals = new ArrayList<>();

    // Which areas are reachable from this chunk (cached)
    public Set<Integer> reachableAreaIds = new HashSet<>();

    // Connected chunks (grid IDs we can reach from edges)
    public Set<Long> connectedChunks = new HashSet<>();

    public ChunkNavData() {
        this.confidence = INITIAL_CONFIDENCE;
        this.lastUpdated = System.currentTimeMillis();
        initializeEdges();
        initializeWalkability();
    }

    /**
     * Initialize walkability to blocked (2) by default.
     * Tiles are only marked walkable (0) when actually observed as walkable.
     * This prevents unvisited tiles from being treated as walkable.
     */
    private void initializeWalkability() {
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                walkability[x][y] = 2;  // Blocked until observed
            }
        }
    }

    public ChunkNavData(long gridId) {
        this();
        this.gridId = gridId;
    }

    public ChunkNavData(long gridId, Coord gridCoord) {
        this(gridId);
        this.gridCoord = gridCoord;
    }

    public ChunkNavData(long gridId, Coord gridCoord, Coord worldTileOrigin) {
        this(gridId, gridCoord);
        this.worldTileOrigin = worldTileOrigin;
    }

    private void initializeEdges() {
        for (int i = 0; i < CELLS_PER_EDGE; i++) {
            // With tile-level resolution, edge points are at exact tile positions
            northEdge[i] = new EdgePoint(i, false, new Coord(i, 0));
            southEdge[i] = new EdgePoint(i, false, new Coord(i, CHUNK_SIZE - 1));
            westEdge[i] = new EdgePoint(i, false, new Coord(0, i));
            eastEdge[i] = new EdgePoint(i, false, new Coord(CHUNK_SIZE - 1, i));
        }
    }

    /**
     * Get edge array for a given direction.
     */
    public EdgePoint[] getEdge(Direction dir) {
        switch (dir) {
            case NORTH: return northEdge;
            case SOUTH: return southEdge;
            case EAST: return eastEdge;
            case WEST: return westEdge;
            default: return null;
        }
    }

    /**
     * Calculate average walkability score for this chunk.
     * 0.0 = fully walkable, 1.0 = fully blocked
     */
    public float averageWalkability() {
        int total = 0;
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                total += walkability[x][y];
            }
        }
        return total / (float) (CELLS_PER_EDGE * CELLS_PER_EDGE * 2); // Normalize to 0-1
    }

    /**
     * Get walkability at a specific coarse cell.
     */
    public byte getWalkability(int cx, int cy) {
        if (cx < 0 || cx >= CELLS_PER_EDGE || cy < 0 || cy >= CELLS_PER_EDGE) {
            return 2; // Out of bounds = blocked
        }
        return walkability[cx][cy];
    }

    /**
     * Set walkability at a specific coarse cell.
     */
    public void setWalkability(int cx, int cy, byte value) {
        if (cx >= 0 && cx < CELLS_PER_EDGE && cy >= 0 && cy < CELLS_PER_EDGE) {
            walkability[cx][cy] = value;
        }
    }

    /**
     * Check if a cell has been observed (within visible range during recording).
     */
    public boolean isObserved(int cx, int cy) {
        if (cx < 0 || cx >= CELLS_PER_EDGE || cy < 0 || cy >= CELLS_PER_EDGE) {
            return false;
        }
        return observed[cx][cy];
    }

    /**
     * Mark a cell as observed.
     */
    public void setObserved(int cx, int cy, boolean value) {
        if (cx >= 0 && cx < CELLS_PER_EDGE && cy >= 0 && cy < CELLS_PER_EDGE) {
            observed[cx][cy] = value;
        }
    }

    /**
     * Count how many cells have been observed in this chunk.
     */
    public int countObservedCells() {
        int count = 0;
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                if (observed[x][y]) count++;
            }
        }
        return count;
    }

    /**
     * Calculate current confidence based on time decay.
     */
    public float getCurrentConfidence() {
        long now = System.currentTimeMillis();
        long ageMs = now - lastUpdated;
        double ageHours = ageMs / (1000.0 * 60 * 60);
        return (float) (confidence * Math.pow(0.5, ageHours / CONFIDENCE_HALF_LIFE_HOURS));
    }

    /**
     * Mark this chunk as recently updated, resetting confidence.
     */
    public void markUpdated() {
        this.lastUpdated = System.currentTimeMillis();
        this.confidence = INITIAL_CONFIDENCE;
    }

    /**
     * Find a portal by its gob hash.
     */
    public ChunkPortal findPortal(String gobHash) {
        if (gobHash == null) return null;
        for (ChunkPortal portal : portals) {
            if (gobHash.equals(portal.gobHash)) {
                return portal;
            }
        }
        return null;
    }

    /**
     * Find a portal by its gob name.
     */
    public ChunkPortal findPortalByName(String gobName) {
        if (gobName == null) return null;
        for (ChunkPortal portal : portals) {
            if (gobName.equals(portal.gobName)) {
                return portal;
            }
        }
        return null;
    }

    /**
     * Find a portal by its position (localCoord).
     * Two portals are considered the same if they're within a few tiles of each other.
     */
    public ChunkPortal findPortalByPosition(Coord localCoord, int tolerance) {
        if (localCoord == null) return null;
        for (ChunkPortal portal : portals) {
            if (portal.localCoord != null) {
                int dx = Math.abs(portal.localCoord.x - localCoord.x);
                int dy = Math.abs(portal.localCoord.y - localCoord.y);
                if (dx <= tolerance && dy <= tolerance) {
                    return portal;
                }
            }
        }
        return null;
    }

    /**
     * Find a portal by its position AND name.
     * Only returns a portal if BOTH the position matches (within tolerance) AND the name matches.
     * This prevents different portal types at the same location from being merged.
     */
    public ChunkPortal findPortalByPositionAndName(Coord localCoord, String gobName, int tolerance) {
        if (localCoord == null || gobName == null) return null;
        for (ChunkPortal portal : portals) {
            if (portal.localCoord != null && gobName.equals(portal.gobName)) {
                int dx = Math.abs(portal.localCoord.x - localCoord.x);
                int dy = Math.abs(portal.localCoord.y - localCoord.y);
                if (dx <= tolerance && dy <= tolerance) {
                    return portal;
                }
            }
        }
        return null;
    }

    /**
     * Add or update a portal. Updates existing portal with same hash OR same position+name.
     * Each building at a different position is a separate portal entry.
     * Different portal types at the same location (e.g., cellardoor and stonemansion-door) are separate.
     * Preserves connectsToGridId if the existing portal has a valid connection.
     */
    public void addOrUpdatePortal(ChunkPortal portal) {
        // First try to find by hash (exact match)
        ChunkPortal existing = findPortal(portal.gobHash);
        // If not found by hash, try by position AND name
        // This handles cases where gobHash changes but building is still there
        // Using name ensures we don't merge different portal types at same location
        if (existing == null && portal.localCoord != null && portal.gobName != null) {
            existing = findPortalByPositionAndName(portal.localCoord, portal.gobName, 3); // 3 tile tolerance
        }
        if (existing != null) {
            // Preserve existing connection if new portal doesn't have one
            if (portal.connectsToGridId == -1 && existing.connectsToGridId != -1) {
                portal.connectsToGridId = existing.connectsToGridId;
                portal.lastTraversed = existing.lastTraversed;
            }
            portals.remove(existing);
        }
        portals.add(portal);
    }

    /**
     * Get walkable edge points for connecting to adjacent chunks.
     */
    public List<EdgePoint> getWalkableEdgePoints(Direction dir) {
        List<EdgePoint> result = new ArrayList<>();
        EdgePoint[] edge = getEdge(dir);
        if (edge != null) {
            for (EdgePoint ep : edge) {
                if (ep.walkable) {
                    result.add(ep);
                }
            }
        }
        return result;
    }

    /**
     * Count walkable edge points in a direction.
     */
    public int countWalkableEdgePoints(Direction dir) {
        int count = 0;
        EdgePoint[] edge = getEdge(dir);
        if (edge != null) {
            for (EdgePoint ep : edge) {
                if (ep.walkable) count++;
            }
        }
        return count;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("gridId", gridId);
        obj.put("lastUpdated", lastUpdated);
        obj.put("confidence", confidence);

        // NOTE: gridCoord and worldTileOrigin are NOT saved - they're session-based and become invalid after restart
        // These fields are populated when the grid is seen in the current session
        // Cross-session pathfinding uses neighbor relationships instead

        // Neighbor relationships (persistent)
        if (neighborNorth != -1) obj.put("neighborNorth", neighborNorth);
        if (neighborSouth != -1) obj.put("neighborSouth", neighborSouth);
        if (neighborEast != -1) obj.put("neighborEast", neighborEast);
        if (neighborWest != -1) obj.put("neighborWest", neighborWest);

        // Layer
        obj.put("layer", layer);

        // Walkability as flat array
        JSONArray walkArr = new JSONArray();
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                walkArr.put(walkability[x][y]);
            }
        }
        obj.put("walkability", walkArr);

        // Observed as flat array (compact: store as bitmask in ints)
        JSONArray obsArr = new JSONArray();
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                obsArr.put(observed[x][y] ? 1 : 0);
            }
        }
        obj.put("observed", obsArr);

        // Edges
        obj.put("northEdge", edgeToJson(northEdge));
        obj.put("southEdge", edgeToJson(southEdge));
        obj.put("eastEdge", edgeToJson(eastEdge));
        obj.put("westEdge", edgeToJson(westEdge));

        // Portals
        JSONArray portalsArr = new JSONArray();
        for (ChunkPortal portal : portals) {
            portalsArr.put(portal.toJson());
        }
        obj.put("portals", portalsArr);

        // Reachable areas
        JSONArray areasArr = new JSONArray();
        for (Integer areaId : reachableAreaIds) {
            areasArr.put(areaId);
        }
        obj.put("reachableAreaIds", areasArr);

        // Connected chunks
        JSONArray connectedArr = new JSONArray();
        for (Long chunkId : connectedChunks) {
            connectedArr.put(chunkId);
        }
        obj.put("connectedChunks", connectedArr);

        return obj;
    }

    private JSONArray edgeToJson(EdgePoint[] edge) {
        JSONArray arr = new JSONArray();
        for (EdgePoint ep : edge) {
            arr.put(ep.toJson());
        }
        return arr;
    }

    public static ChunkNavData fromJson(JSONObject obj) {
        ChunkNavData data = new ChunkNavData();
        data.gridId = obj.getLong("gridId");
        data.lastUpdated = obj.getLong("lastUpdated");
        data.confidence = (float) obj.getDouble("confidence");

        // NOTE: gridCoord and worldTileOrigin are NOT loaded - they're session-based and stale values cause pathfinding bugs
        // These fields will be populated when the grid is seen in the current session
        // Legacy data in JSON is ignored

        // Neighbor relationships (persistent)
        data.neighborNorth = obj.optLong("neighborNorth", -1);
        data.neighborSouth = obj.optLong("neighborSouth", -1);
        data.neighborEast = obj.optLong("neighborEast", -1);
        data.neighborWest = obj.optLong("neighborWest", -1);

        // Layer (default to "surface" for backwards compatibility)
        data.layer = obj.optString("layer", "surface");

        // Walkability
        JSONArray walkArr = obj.getJSONArray("walkability");
        int idx = 0;
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                data.walkability[x][y] = (byte) walkArr.getInt(idx++);
            }
        }

        // Observed (optional for backwards compatibility)
        if (obj.has("observed")) {
            JSONArray obsArr = obj.getJSONArray("observed");
            idx = 0;
            for (int x = 0; x < CELLS_PER_EDGE; x++) {
                for (int y = 0; y < CELLS_PER_EDGE; y++) {
                    data.observed[x][y] = obsArr.getInt(idx++) != 0;
                }
            }
        } else {
            // Old data without observed array - infer from walkability
            // Cells with walkability < 2 were likely observed (we saw them as walkable)
            // Cells with walkability = 2 might be blocked or just unobserved
            // For safety, mark walkable cells as observed, blocked cells as unobserved
            // This will cause re-recording of blocked areas on next visit
            for (int x = 0; x < CELLS_PER_EDGE; x++) {
                for (int y = 0; y < CELLS_PER_EDGE; y++) {
                    data.observed[x][y] = data.walkability[x][y] < 2;
                }
            }
        }

        // Edges
        data.northEdge = edgeFromJson(obj.getJSONArray("northEdge"));
        data.southEdge = edgeFromJson(obj.getJSONArray("southEdge"));
        data.eastEdge = edgeFromJson(obj.getJSONArray("eastEdge"));
        data.westEdge = edgeFromJson(obj.getJSONArray("westEdge"));

        // Portals
        JSONArray portalsArr = obj.getJSONArray("portals");
        for (int i = 0; i < portalsArr.length(); i++) {
            data.portals.add(ChunkPortal.fromJson(portalsArr.getJSONObject(i)));
        }

        // Reachable areas
        JSONArray areasArr = obj.getJSONArray("reachableAreaIds");
        for (int i = 0; i < areasArr.length(); i++) {
            data.reachableAreaIds.add(areasArr.getInt(i));
        }

        // Connected chunks
        JSONArray connectedArr = obj.getJSONArray("connectedChunks");
        for (int i = 0; i < connectedArr.length(); i++) {
            data.connectedChunks.add(connectedArr.getLong(i));
        }

        return data;
    }

    private static EdgePoint[] edgeFromJson(JSONArray arr) {
        EdgePoint[] edge = new EdgePoint[CELLS_PER_EDGE];
        for (int i = 0; i < arr.length() && i < CELLS_PER_EDGE; i++) {
            edge[i] = EdgePoint.fromJson(arr.getJSONObject(i));
        }
        return edge;
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST;

        public Direction opposite() {
            switch (this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                case WEST: return EAST;
                default: return this;
            }
        }

        public Coord toOffset() {
            switch (this) {
                case NORTH: return new Coord(0, -1);
                case SOUTH: return new Coord(0, 1);
                case EAST: return new Coord(1, 0);
                case WEST: return new Coord(-1, 0);
                default: return new Coord(0, 0);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("ChunkNavData[gridId=%d, confidence=%.2f, portals=%d, connected=%d]",
                gridId, getCurrentConfidence(), portals.size(), connectedChunks.size());
    }
}
