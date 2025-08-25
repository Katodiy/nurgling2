package nurgling;

import org.json.JSONObject;
import java.time.Instant;

/**
 * Represents a timer for a localized resource node
 */
public class ResourceTimer {
    private final String resourceId;  // Unique ID combining segment + coordinates + resource type
    private final long segmentId;
    private final haven.Coord tileCoords;
    private final String resourceName;
    private final String resourceType; // e.g., "gfx/terobjs/map/tarpit"
    private final long startTime;     // Unix timestamp when timer was set
    private final long duration;      // Duration in milliseconds
    private final String description; // User-friendly description like "Tar Pit"
    
    public ResourceTimer(long segmentId, haven.Coord tileCoords, String resourceName, 
                        String resourceType, long duration, String description) {
        this.segmentId = segmentId;
        this.tileCoords = tileCoords;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.duration = duration;
        this.description = description;
        this.startTime = Instant.now().toEpochMilli();
        this.resourceId = generateResourceId(segmentId, tileCoords, resourceType);
    }
    
    public ResourceTimer(JSONObject json) {
        this.resourceId = json.getString("resourceId");
        this.segmentId = json.getLong("segmentId");
        this.tileCoords = new haven.Coord(json.getInt("tileX"), json.getInt("tileY"));
        this.resourceName = json.getString("resourceName");
        this.resourceType = json.getString("resourceType");
        this.startTime = json.getLong("startTime");
        this.duration = json.getLong("duration");
        this.description = json.getString("description");
    }
    
    private static String generateResourceId(long segmentId, haven.Coord tileCoords, String resourceType) {
        return String.format("res_%d_%d_%d_%s", segmentId, tileCoords.x, tileCoords.y, 
                           resourceType.replaceAll("[^a-zA-Z0-9]", "_"));
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("resourceId", resourceId);
        json.put("segmentId", segmentId);
        json.put("tileX", tileCoords.x);
        json.put("tileY", tileCoords.y);
        json.put("resourceName", resourceName);
        json.put("resourceType", resourceType);
        json.put("startTime", startTime);
        json.put("duration", duration);
        json.put("description", description);
        return json;
    }
    
    /**
     * Check if the timer has expired
     */
    public boolean isExpired() {
        return getRemainingTime() <= 0;
    }
    
    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTime() {
        long elapsed = Instant.now().toEpochMilli() - startTime;
        return Math.max(0, duration - elapsed);
    }
    
    /**
     * Get remaining time formatted as "Xh Ym" or "Expired"
     */
    public String getFormattedRemainingTime() {
        long remaining = getRemainingTime();
        if (remaining <= 0) {
            return "Ready";
        }
        
        long hours = remaining / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    // Getters
    public String getResourceId() { return resourceId; }
    public long getSegmentId() { return segmentId; }
    public haven.Coord getTileCoords() { return tileCoords; }
    public String getDescription() { return description; }
}