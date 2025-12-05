package nurgling.routes;

import haven.*;
import org.json.JSONObject;

public class ForagerWaypoint {
    
    // Segment-based coordinates (same as MapFile.Marker)
    public long seg;  // Segment ID
    public Coord tc;  // Tile coordinates within segment
    
    public ForagerWaypoint(long seg, Coord tc) {
        this.seg = seg;
        this.tc = tc;
    }
    
    // Create from MiniMap.Location (when clicking on minimap)
    public ForagerWaypoint(MiniMap.Location loc) {
        this.seg = loc.seg.id;
        this.tc = loc.tc;
    }
    
    public ForagerWaypoint(JSONObject json) {
        this.seg = json.getLong("seg");
        JSONObject coordJson = json.getJSONObject("tc");
        this.tc = new Coord(coordJson.getInt("x"), coordJson.getInt("y"));
    }
    
    // Get tile coordinates for minimap display (always works, just returns tc)
    public Coord getTileCoord(MCache mcache) {
        return tc;
    }
    
    // Get world coordinates for pathfinding (needs sessloc for proper conversion)
    public Coord2d toWorldCoord(MiniMap.Location sessloc) {
        if(sessloc == null || sessloc.seg.id != this.seg) {
            return null; // Can't convert if not in same segment
        }
        // Convert segment tile coords to world coords relative to sessloc
        // Same formula as in MiniMap.mvclick: loc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2))
        return tc.sub(sessloc.tc).mul(MCache.tilesz).add(MCache.tilehsz);
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("seg", seg);
        
        JSONObject coordJson = new JSONObject();
        coordJson.put("x", tc.x);
        coordJson.put("y", tc.y);
        json.put("tc", coordJson);
        
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("Waypoint[Seg=%d, TC=(%d,%d)]", seg, tc.x, tc.y);
    }
}
