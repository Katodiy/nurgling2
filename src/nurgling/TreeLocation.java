package nurgling;

import org.json.JSONObject;
import haven.Coord;

/**
 * Represents a saved tree location on the map
 * Simplified version of FishLocation - no percentage, moon, time, or equipment
 */
public class TreeLocation {
    private final String locationId;  // Unique ID combining segment + coordinates + tree name
    private final long segmentId;
    private final Coord tileCoords;
    private final String treeName;        // e.g., "Oak Tree", "Birch Tree"
    private final String treeResource;    // e.g., "gfx/terobjs/trees/oak"
    private final long timestamp;         // When it was saved
    private final int quantity;           // Number of nearby trees/bushes of the same type

    public TreeLocation(long segmentId, Coord tileCoords, String treeName, String treeResource, int quantity) {
        this.segmentId = segmentId;
        this.tileCoords = tileCoords;
        this.treeName = treeName;
        this.treeResource = treeResource;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
        this.locationId = generateLocationId(segmentId, tileCoords, treeName);
    }

    public TreeLocation(JSONObject json) {
        this.locationId = json.getString("locationId");
        this.segmentId = json.getLong("segmentId");
        this.tileCoords = new Coord(json.getInt("tileX"), json.getInt("tileY"));
        this.treeName = json.getString("treeName");
        this.treeResource = json.getString("treeResource");
        this.timestamp = json.getLong("timestamp");
        this.quantity = json.optInt("quantity", 1);  // Default to 1 for backward compatibility
    }

    private static String generateLocationId(long segmentId, Coord tileCoords, String treeName) {
        return String.format("tree_%d_%d_%d_%s", segmentId, tileCoords.x, tileCoords.y,
                           treeName.replaceAll("[^a-zA-Z0-9]", "_"));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("locationId", locationId);
        json.put("segmentId", segmentId);
        json.put("tileX", tileCoords.x);
        json.put("tileY", tileCoords.y);
        json.put("treeName", treeName);
        json.put("treeResource", treeResource);
        json.put("timestamp", timestamp);
        json.put("quantity", quantity);
        return json;
    }

    // Getters
    public String getLocationId() { return locationId; }
    public long getSegmentId() { return segmentId; }
    public Coord getTileCoords() { return tileCoords; }
    public String getTreeName() { return treeName; }
    public String getTreeResource() { return treeResource; }
    public long getTimestamp() { return timestamp; }
    public int getQuantity() { return quantity; }
}
