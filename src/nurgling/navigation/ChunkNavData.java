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
    // These are discovered when grids are loaded together, and we can see their spatial relationship
    public long neighborNorth = -1;  // Grid ID of neighbor to the north (gc.y - 1)
    public long neighborSouth = -1;  // Grid ID of neighbor to the south (gc.y + 1)
    public long neighborEast = -1;   // Grid ID of neighbor to the east (gc.x + 1)
    public long neighborWest = -1;   // Grid ID of neighbor to the west (gc.x - 1)

    // Layer identifier - distinguishes walkable outdoor areas from building interiors
    // "outside" = surface + mines (can walk between grids), "inside" = building interior, "cellar" = underground cellar
    public String layer = "outside";

    // Walkability grid (tile-level resolution)
    // Each cell represents 1 tile (100x100 grid for 100x100 chunk)
    // Values: 0 = walkable, 1 = partially blocked, 2 = fully blocked
    public byte[][] walkability = new byte[CELLS_PER_EDGE][CELLS_PER_EDGE];

    // Observed grid - tracks which tiles have been visually observed
    // true = tile was within visible range when recorded, false = not yet observed
    public boolean[][] observed = new boolean[CELLS_PER_EDGE][CELLS_PER_EDGE];

    // Pre-computed section observation counts for O(1) overlay rendering
    // Grid is divided into 5x5 = 25 sections for finer granularity
    // Each section is exactly 20x20 = 400 tiles (100/5 divides evenly)
    public static final int SECTIONS_PER_SIDE = 5;
    public static final int TOTAL_SECTIONS = SECTIONS_PER_SIDE * SECTIONS_PER_SIDE; // 25
    public static final int TILES_PER_SECTION_SIDE = CELLS_PER_EDGE / SECTIONS_PER_SIDE; // 20
    public static final int TILES_PER_SECTION = TILES_PER_SECTION_SIDE * TILES_PER_SECTION_SIDE; // 400
    private int[] sectionObservedCount = new int[TOTAL_SECTIONS];

    /**
     * Get section index (0-24) for a tile coordinate.
     */
    private static int getSectionIndex(int cx, int cy) {
        int sx = cx / TILES_PER_SECTION_SIDE;
        int sy = cy / TILES_PER_SECTION_SIDE;
        return sy * SECTIONS_PER_SIDE + sx;
    }

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
     * Check if a cell has been observed (within visible range during recording).
     */
    public boolean isObserved(int cx, int cy) {
        if (cx < 0 || cx >= CELLS_PER_EDGE || cy < 0 || cy >= CELLS_PER_EDGE) {
            return false;
        }
        return observed[cx][cy];
    }

    /**
     * Set the observed state for a cell and update section counts.
     */
    public void setObserved(int cx, int cy, boolean value) {
        if (cx < 0 || cx >= CELLS_PER_EDGE || cy < 0 || cy >= CELLS_PER_EDGE) {
            return;
        }
        boolean oldValue = observed[cx][cy];
        if (oldValue != value) {
            observed[cx][cy] = value;
            int section = getSectionIndex(cx, cy);
            if (value) {
                sectionObservedCount[section]++;
            } else {
                sectionObservedCount[section]--;
            }
        }
    }

    /**
     * Check if a section is fully observed (O(1) operation).
     * @param section 0-24 (5x5 grid, row-major order)
     * @return true if all tiles in the section are observed
     */
    public boolean isSectionFullyObserved(int section) {
        if (section < 0 || section >= TOTAL_SECTIONS) return false;
        return sectionObservedCount[section] >= TILES_PER_SECTION;
    }

    /**
     * Get the observation count for a section (for debugging/display).
     */
    public int getSectionObservedCount(int section) {
        if (section < 0 || section >= TOTAL_SECTIONS) return 0;
        return sectionObservedCount[section];
    }

    /**
     * Get the expected tile count for a section.
     */
    public static int getSectionTileCount(int section) {
        if (section < 0 || section >= TOTAL_SECTIONS) return 0;
        return TILES_PER_SECTION;
    }

    /**
     * Recompute section counts from the observed array.
     * Called after loading from JSON or for data repair.
     */
    public void recomputeSectionCounts() {
        for (int i = 0; i < TOTAL_SECTIONS; i++) {
            sectionObservedCount[i] = 0;
        }
        for (int x = 0; x < CELLS_PER_EDGE; x++) {
            for (int y = 0; y < CELLS_PER_EDGE; y++) {
                if (observed[x][y]) {
                    sectionObservedCount[getSectionIndex(x, y)]++;
                }
            }
        }
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

        // Layer
        data.layer = obj.optString("layer", "outside");

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

        // Recompute section counts from loaded observed data
        data.recomputeSectionCounts();

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
    }

    // ========== Utility Methods ==========

    /**
     * Get the Layer enum for this chunk's layer string.
     */
    public Layer getLayer() {
        return Layer.fromString(layer);
    }

    @Override
    public String toString() {
        return String.format("ChunkNavData[gridId=%d, confidence=%.2f, portals=%d, connected=%d]",
                gridId, getCurrentConfidence(), portals.size(), connectedChunks.size());
    }
}
