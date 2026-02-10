package nurgling.widgets;

import haven.*;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Represents a labeled icon mark on the minimap.
 * Used by Checker bots (Water, Soil) to display resource quality on the map.
 * Shows an icon with a label underneath (e.g., "q20" for quality 20).
 */
public class LabeledMinimapMark {
    private final String locationId;     // Unique ID for this mark
    public final String label;           // The text label (e.g., "q20", "q95")
    public final String resourceType;    // Resource type (e.g., "Water", "Clay", "Soil")
    public final long segmentId;
    public final Coord tileCoords;        // Tile coordinates within the segment
    public final BufferedImage iconImage; // The icon to display
    public final long timestamp;          // When it was created
    public final Color labelColor;        // Color for the label text
    
    // Cached textures for rendering
    private TexI iconTex;
    private Text labelText;
    
    // Text furnace for rendering labels (like quest giver names)
    private static final Text.Furnace labelFurnace = new PUtils.BlurFurn(
        new Text.Foundry(Text.sans, 10, Color.WHITE).aa(true), 
        2, 1, new Color(60, 30, 30)
    );
    
    /**
     * Create a labeled minimap mark.
     * 
     * @param label The text to display under the icon (e.g., "q20")
     * @param resourceType The type of resource (e.g., "Water", "Clay")
     * @param segmentId The map segment ID
     * @param tileCoords The tile coordinates within the segment
     * @param iconImage The icon image to display
     * @param labelColor Optional color for the label (null = white)
     */
    public LabeledMinimapMark(String label, String resourceType, long segmentId, Coord tileCoords, 
                              BufferedImage iconImage, Color labelColor) {
        this.label = label;
        this.resourceType = resourceType != null ? resourceType : "Unknown";
        this.segmentId = segmentId;
        this.tileCoords = tileCoords;
        this.iconImage = iconImage;
        this.labelColor = labelColor != null ? labelColor : Color.WHITE;
        this.timestamp = System.currentTimeMillis();
        this.locationId = generateLocationId(segmentId, tileCoords, label);
        
        // Pre-render textures
        if (iconImage != null) {
            this.iconTex = new TexI(iconImage);
        }
        this.labelText = createLabelText();
    }
    
    /**
     * Create a labeled minimap mark with default white label color.
     */
    public LabeledMinimapMark(String label, String resourceType, long segmentId, Coord tileCoords, 
                              BufferedImage iconImage) {
        this(label, resourceType, segmentId, tileCoords, iconImage, null);
    }
    
    /**
     * Create from JSON (for loading from file).
     */
    public LabeledMinimapMark(JSONObject json) {
        this.locationId = json.getString("locationId");
        this.label = json.getString("label");
        this.resourceType = json.optString("resourceType", "Unknown");
        this.segmentId = json.getLong("segmentId");
        this.tileCoords = new Coord(json.getInt("tileX"), json.getInt("tileY"));
        this.timestamp = json.getLong("timestamp");
        
        // Load label color
        if (json.has("labelColor")) {
            this.labelColor = new Color(json.getInt("labelColor"));
        } else {
            this.labelColor = Color.WHITE;
        }
        
        // Load icon image from base64
        BufferedImage loadedIcon = null;
        if (json.has("iconBase64")) {
            try {
                String base64 = json.getString("iconBase64");
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                loadedIcon = ImageIO.read(new ByteArrayInputStream(imageBytes));
            } catch (Exception e) {
                System.err.println("Failed to load icon from base64: " + e.getMessage());
            }
        }
        this.iconImage = loadedIcon;
        
        // Pre-render textures
        if (iconImage != null) {
            this.iconTex = new TexI(iconImage);
        }
        this.labelText = createLabelText();
    }
    
    /**
     * Convert to JSON (for saving to file).
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("locationId", locationId);
        json.put("label", label);
        json.put("resourceType", resourceType);
        json.put("segmentId", segmentId);
        json.put("tileX", tileCoords.x);
        json.put("tileY", tileCoords.y);
        json.put("timestamp", timestamp);
        json.put("labelColor", labelColor.getRGB());
        
        // Save icon image as base64
        if (iconImage != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(iconImage, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                json.put("iconBase64", base64);
            } catch (Exception e) {
                System.err.println("Failed to save icon to base64: " + e.getMessage());
            }
        }
        
        return json;
    }
    
    private static String generateLocationId(long segmentId, Coord tileCoords, String label) {
        return String.format("labeled_%d_%d_%d_%s", segmentId, tileCoords.x, tileCoords.y,
                           label.replaceAll("[^a-zA-Z0-9]", "_"));
    }
    
    /**
     * Create the text render for the label with the appropriate color.
     */
    private Text createLabelText() {
        if (labelColor.equals(Color.WHITE)) {
            return labelFurnace.render(label);
        } else {
            // Create custom furnace with the specified color
            Text.Furnace customFurnace = new PUtils.BlurFurn(
                new Text.Foundry(Text.sans, 10, labelColor).aa(true), 
                2, 1, new Color(60, 30, 30)
            );
            return customFurnace.render(label);
        }
    }
    
    /**
     * Get the icon texture for rendering.
     */
    public TexI getIconTex() {
        return iconTex;
    }
    
    /**
     * Get the label text for rendering.
     */
    public Text getLabelText() {
        return labelText;
    }
    
    /**
     * Check if this mark is in the specified segment.
     */
    public boolean isInSegment(long segId) {
        return this.segmentId == segId;
    }
    
    /**
     * Get a unique identifier for this mark.
     */
    public String getLocationId() {
        return locationId;
    }
    
    /**
     * Check if this mark is at the same location as another.
     * Used to avoid duplicate marks at the same spot.
     */
    public boolean isSameLocation(LabeledMinimapMark other) {
        return this.segmentId == other.segmentId && 
               this.tileCoords.equals(other.tileCoords);
    }
    
    /**
     * Check if a coordinate is near this mark (within given tile radius).
     */
    public boolean isNear(long segId, Coord tc, int radiusTiles) {
        if (this.segmentId != segId) return false;
        int dx = Math.abs(this.tileCoords.x - tc.x);
        int dy = Math.abs(this.tileCoords.y - tc.y);
        return dx <= radiusTiles && dy <= radiusTiles;
    }
}

