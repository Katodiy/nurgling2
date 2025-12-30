package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of global path planning on the chunk graph.
 * Contains both high-level waypoints and detailed tile-level path.
 */
public class ChunkPath {
    public List<ChunkWaypoint> waypoints = new ArrayList<>();
    public float totalCost;
    public float confidence;       // Min confidence along path
    public boolean requiresPortals;

    /**
     * Detailed tile-level path segments.
     * Each segment is a list of TileStep from one waypoint to the next.
     * This is the actual path to follow, computed by intra-chunk A*.
     */
    public List<PathSegment> segments = new ArrayList<>();

    public ChunkPath() {
        this.totalCost = 0;
        this.confidence = 1.0f;
        this.requiresPortals = false;
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public int size() {
        return waypoints.size();
    }

    public ChunkWaypoint getFirst() {
        return waypoints.isEmpty() ? null : waypoints.get(0);
    }

    public ChunkWaypoint getLast() {
        return waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1);
    }

    /**
     * A single waypoint in the path.
     */
    public static class ChunkWaypoint {
        public long gridId;
        public Coord localCoord;       // Local tile coordinate within chunk
        public Coord2d worldCoord;     // World coordinate (if known)
        public ChunkPortal portal;     // Portal to traverse (null if just walking)
        public WaypointType type;

        public ChunkWaypoint() {
            this.type = WaypointType.WALK;
        }

        public ChunkWaypoint(long gridId, Coord localCoord, WaypointType type) {
            this.gridId = gridId;
            this.localCoord = localCoord;
            this.type = type;
        }

        public ChunkWaypoint(long gridId, Coord localCoord, ChunkPortal portal) {
            this.gridId = gridId;
            this.localCoord = localCoord;
            this.portal = portal;
            this.type = portal != null ? WaypointType.PORTAL_ENTRY : WaypointType.WALK;
        }

        public boolean isPortal() {
            return portal != null || type == WaypointType.PORTAL_ENTRY || type == WaypointType.PORTAL_EXIT;
        }

        @Override
        public String toString() {
            return String.format("Waypoint[grid=%d, local=(%d,%d), type=%s, portal=%s]",
                    gridId, localCoord.x, localCoord.y, type, portal != null ? portal.type : "none");
        }
    }

    public enum WaypointType {
        WALK,           // Just walk to this point
        PORTAL_ENTRY,   // Enter a portal here
        PORTAL_EXIT,    // Exit point after portal
        DESTINATION     // Final destination
    }

    /**
     * Check if this path has detailed tile-level segments.
     */
    public boolean hasDetailedPath() {
        return !segments.isEmpty();
    }

    /**
     * Get total number of tile steps across all segments.
     */
    public int getTotalTileSteps() {
        int total = 0;
        for (PathSegment seg : segments) {
            total += seg.steps.size();
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChunkPath[cost=").append(totalCost);
        sb.append(", confidence=").append(confidence);
        sb.append(", waypoints=").append(waypoints.size());
        sb.append(", segments=").append(segments.size());
        sb.append(", tileSteps=").append(getTotalTileSteps());
        if (requiresPortals) sb.append(", hasPortals");
        sb.append("]\n");
        for (int i = 0; i < waypoints.size(); i++) {
            sb.append("  ").append(i).append(": ").append(waypoints.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * A segment of the path within a single chunk.
     * Contains tile-level steps from one point to another.
     */
    public static class PathSegment {
        public long gridId;                    // Grid this segment is in
        public Coord worldTileOrigin;          // World tile origin for converting local to world
        public List<TileStep> steps;           // Tile-level steps to follow
        public SegmentType type;               // What kind of segment this is

        public PathSegment(long gridId, Coord worldTileOrigin) {
            this.gridId = gridId;
            this.worldTileOrigin = worldTileOrigin;
            this.steps = new ArrayList<>();
            this.type = SegmentType.WALK;
        }

        public boolean isEmpty() {
            return steps.isEmpty();
        }

        public int size() {
            return steps.size();
        }
    }

    /**
     * Type of path segment.
     */
    public enum SegmentType {
        WALK,           // Walk along these tiles
        PORTAL          // Use a portal at the end of this segment
    }

    /**
     * A single tile step in the path.
     */
    public static class TileStep {
        public Coord localCoord;      // Local tile coordinate (0-99, 0-99)
        public Coord2d worldCoord;    // World coordinate (computed from localCoord + worldTileOrigin)

        public TileStep(Coord localCoord, Coord worldTileOrigin) {
            this.localCoord = localCoord;
            // Compute world coordinate: (worldTileOrigin + localCoord) * tilesz + tilehsz
            if (worldTileOrigin != null) {
                Coord worldTile = worldTileOrigin.add(localCoord);
                this.worldCoord = worldTile.mul(haven.MCache.tilesz).add(haven.MCache.tilehsz);
            }
        }

        public TileStep(Coord localCoord, Coord2d worldCoord) {
            this.localCoord = localCoord;
            this.worldCoord = worldCoord;
        }
    }
}
