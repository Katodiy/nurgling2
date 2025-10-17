package nurgling;

import org.json.JSONObject;
import haven.Coord;

/**
 * Represents a saved fish location on the map
 * Follows the same pattern as LocalizedResourceTimer for consistency
 */
public class FishLocation {
    private final String locationId;  // Unique ID combining segment + coordinates + fish name
    private final long segmentId;
    private final Coord tileCoords;
    private final String fishName;        // e.g., "Asp", "Salmon"
    private final String fishResource;    // e.g., "gfx/invobjs/fish-asp"
    private final long timestamp;         // When it was saved

    public FishLocation(long segmentId, Coord tileCoords, String fishName, String fishResource) {
        this.segmentId = segmentId;
        this.tileCoords = tileCoords;
        this.fishName = fishName;
        this.fishResource = fishResource;
        this.timestamp = System.currentTimeMillis();
        this.locationId = generateLocationId(segmentId, tileCoords, fishName);
    }

    public FishLocation(JSONObject json) {
        this.locationId = json.getString("locationId");
        this.segmentId = json.getLong("segmentId");
        this.tileCoords = new Coord(json.getInt("tileX"), json.getInt("tileY"));
        this.fishName = json.getString("fishName");
        this.fishResource = json.getString("fishResource");
        this.timestamp = json.getLong("timestamp");
    }

    private static String generateLocationId(long segmentId, Coord tileCoords, String fishName) {
        return String.format("fish_%d_%d_%d_%s", segmentId, tileCoords.x, tileCoords.y,
                           fishName.replaceAll("[^a-zA-Z0-9]", "_"));
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("locationId", locationId);
        json.put("segmentId", segmentId);
        json.put("tileX", tileCoords.x);
        json.put("tileY", tileCoords.y);
        json.put("fishName", fishName);
        json.put("fishResource", fishResource);
        json.put("timestamp", timestamp);
        return json;
    }

    // Getters
    public String getLocationId() { return locationId; }
    public long getSegmentId() { return segmentId; }
    public Coord getTileCoords() { return tileCoords; }
    public String getFishName() { return fishName; }
    public String getFishResource() { return fishResource; }
    public long getTimestamp() { return timestamp; }
}
