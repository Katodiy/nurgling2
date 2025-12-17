package nurgling.navigation;

import haven.Coord;
import haven.Coord2d;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of global path planning on the chunk graph.
 */
public class ChunkPath {
    public List<ChunkWaypoint> waypoints = new ArrayList<>();
    public float totalCost;
    public float confidence;       // Min confidence along path
    public boolean requiresPortals;

    public ChunkPath() {
        this.totalCost = 0;
        this.confidence = 1.0f;
        this.requiresPortals = false;
    }

    public void addWaypoint(ChunkWaypoint waypoint) {
        waypoints.add(waypoint);
        if (waypoint.portal != null) {
            requiresPortals = true;
        }
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChunkPath[cost=").append(totalCost);
        sb.append(", confidence=").append(confidence);
        sb.append(", waypoints=").append(waypoints.size());
        if (requiresPortals) sb.append(", hasPortals");
        sb.append("]\n");
        for (int i = 0; i < waypoints.size(); i++) {
            sb.append("  ").append(i).append(": ").append(waypoints.get(i)).append("\n");
        }
        return sb.toString();
    }
}
