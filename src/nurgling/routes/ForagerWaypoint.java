package nurgling.routes;

import haven.*;
import org.json.JSONObject;

public class ForagerWaypoint {
    
    public long gridId;
    public Coord localCoord;
    
    public ForagerWaypoint(long gridId, Coord localCoord) {
        this.gridId = gridId;
        this.localCoord = localCoord;
    }
    
    public ForagerWaypoint(Coord2d worldPos, MCache mcache) {
        // Convert world coordinates to tile coordinates
        Coord tilec = worldPos.div(MCache.tilesz).floor();
        
        // Get the grid that contains this tile
        Coord gridCoord = tilec.div(MCache.cmaps);
        MCache.Grid grid = mcache.getgrid(gridCoord);
        
        if (grid != null) {
            this.gridId = grid.id;
            // Local coord is tile coordinates relative to grid's upper-left corner
            this.localCoord = tilec.sub(grid.ul);
        } else {
            this.gridId = 0;
            this.localCoord = Coord.z;
        }
    }
    

    
    public ForagerWaypoint(JSONObject json) {
        this.gridId = json.getLong("gridId");
        JSONObject coordJson = json.getJSONObject("localCoord");
        this.localCoord = new Coord(coordJson.getInt("x"), coordJson.getInt("y"));
    }
    
    public Coord getTileCoord(MCache mcache) {
        // Find grid by ID
        MCache.Grid grid = mcache.findGrid(gridId);
        
        if (grid != null) {
            return grid.ul.add(localCoord);
        }
        
        return null;
    }
    
    public Coord2d toWorldCoord(MCache mcache) {
        // Find grid by ID
        MCache.Grid grid = mcache.findGrid(gridId);
        
        if (grid != null) {
            Coord tilec = grid.ul.add(localCoord);
            return tilec.mul(MCache.tilesz).add(MCache.tilehsz);
        }
        
        return null;
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("gridId", gridId);
        
        JSONObject coordJson = new JSONObject();
        coordJson.put("x", localCoord.x);
        coordJson.put("y", localCoord.y);
        json.put("localCoord", coordJson);
        
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("Waypoint[Grid=%d, Local=(%d,%d)]", gridId, localCoord.x, localCoord.y);
    }
}
