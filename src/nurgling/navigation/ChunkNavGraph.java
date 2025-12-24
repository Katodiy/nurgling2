package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.MCache;
import nurgling.NGameUI;
import nurgling.NUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static nurgling.navigation.ChunkNavConfig.*;
import static nurgling.navigation.ChunkNavData.Direction;
import static nurgling.navigation.ChunkNavDebug.*;

/**
 * The main navigation graph structure containing all known chunks.
 */
public class ChunkNavGraph {
    // All known chunks indexed by grid ID
    private final Map<Long, ChunkNavData> chunks = new ConcurrentHashMap<>();

    // Spatial index: grid coordinate -> grid ID
    private final Map<Coord, Long> gridCoordToId = new ConcurrentHashMap<>();

    // Portal index (gobHash -> ChunkPortal)
    private final Map<String, ChunkPortal> portalIndex = new ConcurrentHashMap<>();

    // Portal to chunk mapping (gobHash -> gridId)
    private final Map<String, Long> portalToChunk = new ConcurrentHashMap<>();

    public ChunkNavGraph() {
    }

    /**
     * Add or update a chunk in the graph.
     */
    public void addChunk(ChunkNavData chunk) {
        chunks.put(chunk.gridId, chunk);
        if (chunk.gridCoord != null) {
            gridCoordToId.put(chunk.gridCoord, chunk.gridId);
        }
        // Index portals
        for (ChunkPortal portal : chunk.portals) {
            portalIndex.put(portal.gobHash, portal);
            portalToChunk.put(portal.gobHash, chunk.gridId);
        }
    }

    /**
     * Get a chunk by grid ID.
     */
    public ChunkNavData getChunk(long gridId) {
        return chunks.get(gridId);
    }

    /**
     * Get a chunk by grid coordinate.
     */
    public ChunkNavData getChunkByCoord(Coord gridCoord) {
        Long gridId = gridCoordToId.get(gridCoord);
        return gridId != null ? chunks.get(gridId) : null;
    }

    /**
     * Check if we have data for a chunk.
     */
    public boolean hasChunk(long gridId) {
        return chunks.containsKey(gridId);
    }

    /**
     * Get all chunks.
     */
    public Collection<ChunkNavData> getAllChunks() {
        return new ArrayList<>(chunks.values());
    }

    /**
     * Get the number of recorded chunks.
     */
    public int getChunkCount() {
        return chunks.size();
    }

    /**
     * Find chunk containing a world coordinate.
     */
    public ChunkNavData findChunkAtWorldCoord(Coord2d worldCoord, MCache mcache) {
        if (mcache == null) {
            return null;
        }

        try {
            Coord tileCoord = worldCoord.floor(MCache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            if (grid != null) {
                return chunks.get(grid.id);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get chunk containing the player.
     */
    public ChunkNavData getPlayerChunk() {
        long gridId = getPlayerChunkId();
        return gridId != -1 ? chunks.get(gridId) : null;
    }

    /**
     * Get the grid ID for the player's current chunk.
     * Returns -1 if the grid ID cannot be determined.
     */
    public long getPlayerChunkId() {
        try {
            Gob player = NUtils.player();
            if (player == null) {
                return -1;
            }

            // Try NGob.grid_id first (cached and efficient)
            if (player.ngob != null && player.ngob.grid_id != 0) {
                return player.ngob.grid_id;
            }

            // Fallback: direct lookup via MCache
            NGameUI gui = NUtils.getGameUI();
            if (gui == null || gui.map == null || gui.map.glob == null || gui.map.glob.map == null) {
                return -1;
            }
            MCache mcache = gui.map.glob.map;
            Coord tileCoord = player.rc.floor(MCache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            return grid != null ? grid.id : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get the player's current grid, recording it if necessary.
     */
    public long getPlayerChunkIdAndRecord(ChunkNavRecorder recorder) {
        try {
            if (NUtils.getGameUI() == null || NUtils.getGameUI().map == null) return -1;
            MCache mcache = NUtils.getGameUI().map.glob.map;
            Gob player = NUtils.player();
            if (player == null) return -1;

            Coord tileCoord = player.rc.floor(mcache.tilesz);
            MCache.Grid grid = mcache.getgridt(tileCoord);
            if (grid == null) return -1;

            // Record if not already recorded
            if (!hasChunk(grid.id) && recorder != null) {
                recorder.recordGrid(grid);
            }

            return grid.id;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Find a portal by its gob hash.
     */
    public ChunkPortal findPortal(String gobHash) {
        return portalIndex.get(gobHash);
    }

    /**
     * Get chunk containing a portal.
     */
    public ChunkNavData getChunkForPortal(String gobHash) {
        Long gridId = portalToChunk.get(gobHash);
        return gridId != null ? chunks.get(gridId) : null;
    }

    /**
     * Get all edges from a chunk (for A* expansion).
     */
    public List<ChunkEdge> getEdges(long fromGridId) {
        List<ChunkEdge> edges = new ArrayList<>();
        ChunkNavData fromChunk = chunks.get(fromGridId);
        if (fromChunk == null) {
            log("getEdges(" + fromGridId + ") - chunk not found");
            return edges;
        }

        // Add edges to connected chunks via edges
        for (Long toGridId : fromChunk.connectedChunks) {
            ChunkNavData toChunk = chunks.get(toGridId);
            if (toChunk != null) {
                // Find best crossing point
                EdgeCrossing crossing = findBestCrossing(fromChunk, toChunk);
                if (crossing != null) {
                    ChunkEdge edge = new ChunkEdge();
                    edge.fromGridId = fromGridId;
                    edge.toGridId = toGridId;
                    edge.crossingPoint = crossing.crossingPoint;
                    edge.cost = calculateEdgeCost(fromChunk, toChunk, null);
                    edges.add(edge);
                }
            }
        }

        // Add edges via portals (grid IDs can be negative, -1 means unknown)
        for (ChunkPortal portal : fromChunk.portals) {
            if (portal.connectsToGridId != -1 && chunks.containsKey(portal.connectsToGridId)) {
                ChunkNavData toChunk = chunks.get(portal.connectsToGridId);
                ChunkEdge edge = new ChunkEdge();
                edge.fromGridId = fromGridId;
                edge.toGridId = portal.connectsToGridId;
                edge.crossingPoint = portal.localCoord;
                edge.portal = portal;
                edge.cost = calculateEdgeCost(fromChunk, toChunk, portal);
                edges.add(edge);
                log("getEdges(" + fromGridId + ") - portal edge to " + portal.connectsToGridId + " via " + portal.gobName);
            } else if (portal.connectsToGridId == -1) {
                log("getEdges(" + fromGridId + ") - portal " + portal.gobName + " has no connectsToGridId");
            }
        }

        log("getEdges(" + fromGridId + ") - " + fromChunk.connectedChunks.size() + " adjacent + " +
            fromChunk.portals.size() + " portals = " + edges.size() + " edges");

        return edges;
    }

    /**
     * Find the best crossing point between two adjacent chunks.
     * Uses a lenient approach: if edge tiles are not observed, assume they're passable.
     * This prevents blocking paths due to incomplete exploration data.
     */
    private EdgeCrossing findBestCrossing(ChunkNavData fromChunk, ChunkNavData toChunk) {
        // Determine which direction toChunk is relative to fromChunk
        Direction dir = getDirectionTo(fromChunk, toChunk);
        if (dir == null) return null;

        EdgePoint[] fromEdge = fromChunk.getEdge(dir);
        EdgePoint[] toEdge = toChunk.getEdge(dir.opposite());

        // Find best walkable crossing
        // Use lenient check: unobserved edge tiles are assumed passable
        int bestIndex = -1;
        int centerIndex = CELLS_PER_EDGE / 2;

        for (int i = 0; i < CELLS_PER_EDGE; i++) {
            // Check if edge is walkable, with fallback for unobserved tiles
            boolean fromWalkable = isEdgeWalkable(fromChunk, dir, i);
            boolean toWalkable = isEdgeWalkable(toChunk, dir.opposite(), i);

            if (fromWalkable && toWalkable) {
                if (bestIndex < 0 || Math.abs(i - centerIndex) < Math.abs(bestIndex - centerIndex)) {
                    bestIndex = i;
                }
            }
        }

        if (bestIndex >= 0) {
            EdgeCrossing crossing = new EdgeCrossing();
            crossing.crossingPoint = fromEdge[bestIndex].localCoord;
            crossing.direction = dir;
            return crossing;
        }

        // Fallback: if no walkable crossing found, assume center is passable
        // This prevents isolation of chunks due to incomplete edge data
        EdgeCrossing fallback = new EdgeCrossing();
        fallback.crossingPoint = fromEdge[centerIndex].localCoord;
        fallback.direction = dir;
        return fallback;
    }

    /**
     * Check if an edge tile is walkable, with lenient handling of unobserved tiles.
     * If the tile was never observed, assume it's walkable.
     */
    private boolean isEdgeWalkable(ChunkNavData chunk, Direction dir, int index) {
        // Get the tile coordinates for this edge position
        int x, y;
        switch (dir) {
            case NORTH: x = index; y = 0; break;
            case SOUTH: x = index; y = CELLS_PER_EDGE - 1; break;
            case WEST: x = 0; y = index; break;
            case EAST: x = CELLS_PER_EDGE - 1; y = index; break;
            default: return true;
        }

        // If tile was never observed, assume it's walkable
        if (!chunk.isObserved(x, y)) {
            return true;
        }

        // Otherwise use actual walkability
        return chunk.getWalkability(x, y) <= 1;
    }

    /**
     * Determine direction from one chunk to another.
     * Uses stored neighbor relationships (persistent) or falls back to session-based coordinates.
     */
    private Direction getDirectionTo(ChunkNavData from, ChunkNavData to) {
        // Check stored neighbor relationships first (persistent across sessions)
        if (from.neighborNorth == to.gridId) return Direction.NORTH;
        if (from.neighborSouth == to.gridId) return Direction.SOUTH;
        if (from.neighborEast == to.gridId) return Direction.EAST;
        if (from.neighborWest == to.gridId) return Direction.WEST;

        // Fallback to session-based worldTileOrigin (only works within same session)
        if (from.worldTileOrigin == null || to.worldTileOrigin == null) return null;

        int dx = to.worldTileOrigin.x - from.worldTileOrigin.x;
        int dy = to.worldTileOrigin.y - from.worldTileOrigin.y;

        // Chunks are 100 tiles apart
        if (dx == CHUNK_SIZE && dy == 0) return Direction.EAST;
        if (dx == -CHUNK_SIZE && dy == 0) return Direction.WEST;
        if (dx == 0 && dy == CHUNK_SIZE) return Direction.SOUTH;
        if (dx == 0 && dy == -CHUNK_SIZE) return Direction.NORTH;

        return null; // Not adjacent
    }

    /**
     * Calculate cost to traverse from one chunk to another.
     */
    public float calculateEdgeCost(ChunkNavData from, ChunkNavData to, ChunkPortal portal) {
        float cost = BASE_CHUNK_COST;

        // Walkability penalty
        cost += from.averageWalkability() * WALKABILITY_PENALTY;
        cost += to.averageWalkability() * WALKABILITY_PENALTY;

        // Confidence penalty
        float minConfidence = Math.min(from.getCurrentConfidence(), to.getCurrentConfidence());
        cost += (1.0f - minConfidence) * UNCERTAINTY_PENALTY;

        // Portal cost
        if (portal != null) {
            cost += PORTAL_TRAVERSAL_COST;
        }

        return cost;
    }

    /**
     * Update connections between chunks after recording.
     * Uses stored neighbor relationships (persistent) or falls back to session-based coordinates.
     * Only connects chunks on the same layer (surface, inside, cellar, etc.)
     */
    public void updateConnections(ChunkNavData chunk) {
        // Check neighbors by stored relationships first (persistent across sessions)
        long[] neighborIds = {chunk.neighborNorth, chunk.neighborSouth, chunk.neighborEast, chunk.neighborWest};

        for (long neighborId : neighborIds) {
            if (neighborId == -1) continue;

            ChunkNavData other = chunks.get(neighborId);
            if (other == null) continue;

            // Only connect chunks on the same layer
            if (!chunk.layer.equals(other.layer)) continue;

            // Check if they can connect (have walkable crossing)
            EdgeCrossing crossing = findBestCrossing(chunk, other);
            if (crossing != null) {
                chunk.connectedChunks.add(other.gridId);
                other.connectedChunks.add(chunk.gridId);
            }
        }

        // Fallback: also check session-based coordinates for chunks without neighbor data
        if (chunk.worldTileOrigin != null) {
            for (ChunkNavData other : chunks.values()) {
                if (other.gridId == chunk.gridId) continue;
                if (chunk.connectedChunks.contains(other.gridId)) continue; // Already connected
                if (!chunk.layer.equals(other.layer)) continue;
                if (other.worldTileOrigin == null) continue;

                int dx = other.worldTileOrigin.x - chunk.worldTileOrigin.x;
                int dy = other.worldTileOrigin.y - chunk.worldTileOrigin.y;

                boolean isAdjacent = (Math.abs(dx) == CHUNK_SIZE && dy == 0) ||
                                     (dx == 0 && Math.abs(dy) == CHUNK_SIZE);

                if (isAdjacent) {
                    EdgeCrossing crossing = findBestCrossing(chunk, other);
                    if (crossing != null) {
                        chunk.connectedChunks.add(other.gridId);
                        other.connectedChunks.add(chunk.gridId);
                    }
                }
            }
        }
    }

    /**
     * Rebuild all connections between chunks.
     * Call this after loading chunks from storage to ensure connectivity is up to date.
     */
    public void rebuildAllConnections() {
        // Clear existing connections
        for (ChunkNavData chunk : chunks.values()) {
            chunk.connectedChunks.clear();
        }

        // Rebuild connections for all chunks
        for (ChunkNavData chunk : chunks.values()) {
            updateConnections(chunk);
        }
    }

    /**
     * Get chunks that contain a specific area.
     */
    public Set<Long> getChunksForArea(int areaId) {
        Set<Long> result = new HashSet<>();
        for (ChunkNavData chunk : chunks.values()) {
            if (chunk.reachableAreaIds.contains(areaId)) {
                result.add(chunk.gridId);
            }
        }
        return result;
    }

    /**
     * Clear all data.
     */
    public void clear() {
        chunks.clear();
        gridCoordToId.clear();
        portalIndex.clear();
        portalToChunk.clear();
    }

    /**
     * Get statistics about the graph.
     */
    public String getStats() {
        int totalPortals = portalIndex.size();
        int totalConnections = 0;
        float avgConfidence = 0;

        for (ChunkNavData chunk : chunks.values()) {
            totalConnections += chunk.connectedChunks.size();
            avgConfidence += chunk.getCurrentConfidence();
        }

        if (!chunks.isEmpty()) {
            avgConfidence /= chunks.size();
        }

        return String.format("ChunkNavGraph[chunks=%d, portals=%d, connections=%d, avgConfidence=%.2f]",
                chunks.size(), totalPortals, totalConnections / 2, avgConfidence);
    }

    /**
     * Edge in the chunk graph.
     */
    public static class ChunkEdge {
        public long fromGridId;
        public long toGridId;
        public Coord crossingPoint;
        public ChunkPortal portal;
        public float cost;
    }

    /**
     * Crossing point between chunks.
     */
    private static class EdgeCrossing {
        public Coord crossingPoint;
        public Direction direction;
    }
}
