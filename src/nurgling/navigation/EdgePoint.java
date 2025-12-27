package nurgling.navigation;

import haven.Coord;
import org.json.JSONObject;

/**
 * Represents a potential crossing point at chunk boundaries.
 */
public class EdgePoint {
    public int index;              // 0-24 position along edge
    public boolean walkable;       // Can cross here?
    public Coord localCoord;       // Precise tile coordinate
    public String portalHash;      // If this edge leads to a portal (null otherwise)

    public EdgePoint() {
        this.index = 0;
        this.walkable = false;
        this.localCoord = new Coord(0, 0);
        this.portalHash = null;
    }

    public EdgePoint(int index, boolean walkable, Coord localCoord) {
        this.index = index;
        this.walkable = walkable;
        this.localCoord = localCoord;
        this.portalHash = null;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("index", index);
        obj.put("walkable", walkable);
        obj.put("localX", localCoord.x);
        obj.put("localY", localCoord.y);
        if (portalHash != null) {
            obj.put("portalHash", portalHash);
        }
        return obj;
    }

    public static EdgePoint fromJson(JSONObject obj) {
        EdgePoint ep = new EdgePoint();
        ep.index = obj.getInt("index");
        ep.walkable = obj.getBoolean("walkable");
        ep.localCoord = new Coord(obj.getInt("localX"), obj.getInt("localY"));
        if (obj.has("portalHash")) {
            ep.portalHash = obj.getString("portalHash");
        }
        return ep;
    }
}
