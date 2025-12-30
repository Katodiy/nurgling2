package nurgling.navigation;

import haven.Coord;
import org.json.JSONObject;

/**
 * Represents a door, staircase, or other transition point within a chunk.
 */
public class ChunkPortal {
    public String gobHash;         // Unique identifier for the gob
    public String gobName;         // Resource name (gfx/terobjs/...)
    public PortalType type;        // Type of portal
    public Coord localCoord;       // Position within chunk (tile coordinates)
    public long connectsToGridId;  // Grid ID on the other side (if known, -1 otherwise)
    public Coord exitLocalCoord;   // Where you appear in the destination chunk (recorded on traversal)
    public long lastTraversed;     // When we last went through (timestamp)

    public enum PortalType {
        DOOR,           // Regular doors (stonemansion-door, etc.)
        GATE,           // Palisade gates, brick gates
        STAIRS_UP,      // Upstairs
        STAIRS_DOWN,    // Downstairs
        CELLAR,         // Cellar entrance
        MINE_ENTRANCE,  // Mine entrance (legacy, generic)
        MINEHOLE,       // Minehole - goes DOWN into mine
        LADDER;         // Ladder - goes UP out of mine level

        public static PortalType fromString(String s) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException e) {
                // Handle legacy "MINE_ENTRANCE" - default to MINEHOLE
                if ("MINE_ENTRANCE".equals(s)) {
                    return MINEHOLE;
                }
                return DOOR;
            }
        }
    }

    public ChunkPortal() {
        this.connectsToGridId = -1;
        this.lastTraversed = 0;
    }

    public ChunkPortal(String gobHash, String gobName, PortalType type, Coord localCoord) {
        this();
        this.gobHash = gobHash;
        this.gobName = gobName;
        this.type = type;
        this.localCoord = localCoord;
    }

    /**
     * Classify a gob name into a portal type.
     * Returns null if the gob is not a portal.
     */
    public static PortalType classifyPortal(String gobName) {
        if (gobName == null) return null;

        String name = gobName.toLowerCase();

        // Stairs (check before door since some stairs have "door" in name)
        if (name.contains("upstairs") || name.contains("stairs-up")) {
            return PortalType.STAIRS_UP;
        }
        if (name.contains("downstairs") || name.contains("stairs-down")) {
            return PortalType.STAIRS_DOWN;
        }

        // Cellar
        if (name.contains("cellar")) {
            return PortalType.CELLAR;
        }

        // Mine - distinguish between minehole (down) and ladder (up)
        if (name.contains("minehole")) {
            return PortalType.MINEHOLE;
        }
        if (name.contains("ladder")) {
            return PortalType.LADDER;
        }

        // Gates are NOT portals - they're just openings in walls.
        // Walkability grid handles whether you can walk through them.

        // Doors
        if (name.contains("door")) {
            return PortalType.DOOR;
        }

        return null;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("gobHash", gobHash);
        obj.put("gobName", gobName);
        obj.put("type", type.name());
        obj.put("localX", localCoord.x);
        obj.put("localY", localCoord.y);
        obj.put("connectsToGridId", connectsToGridId);
        if (exitLocalCoord != null) {
            obj.put("exitLocalX", exitLocalCoord.x);
            obj.put("exitLocalY", exitLocalCoord.y);
        }
        obj.put("lastTraversed", lastTraversed);
        return obj;
    }

    public static ChunkPortal fromJson(JSONObject obj) {
        ChunkPortal portal = new ChunkPortal();
        portal.gobHash = obj.optString("gobHash", "unknown_" + System.nanoTime());
        portal.gobName = obj.optString("gobName", "unknown");
        portal.type = PortalType.fromString(obj.optString("type", "DOOR"));
        portal.localCoord = new Coord(obj.optInt("localX", 50), obj.optInt("localY", 50));
        portal.connectsToGridId = obj.optLong("connectsToGridId", -1);
        if (obj.has("exitLocalX") && obj.has("exitLocalY")) {
            portal.exitLocalCoord = new Coord(obj.getInt("exitLocalX"), obj.getInt("exitLocalY"));
        }
        portal.lastTraversed = obj.optLong("lastTraversed", 0);
        return portal;
    }

    @Override
    public String toString() {
        return String.format("Portal[%s, type=%s, at=(%d,%d)]", gobName, type, localCoord.x, localCoord.y);
    }
}
