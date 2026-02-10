package nurgling.widgets;

import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.UUID;

/**
 * Represents a saved custom icon configuration.
 * Contains background ID and item resource for generating the icon.
 */
public class CustomIcon {
    private String id;
    private String name;
    private String backgroundId;
    private JSONObject itemResource;

    // Cached generated images
    private transient BufferedImage[] cachedImages;

    public CustomIcon() {
        this.id = UUID.randomUUID().toString();
    }

    public CustomIcon(String name, String backgroundId, JSONObject itemResource) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.backgroundId = backgroundId;
        this.itemResource = itemResource;
    }

    public CustomIcon(JSONObject json) {
        this.id = json.optString("id", UUID.randomUUID().toString());
        this.name = json.optString("name", "Unnamed");
        this.backgroundId = json.optString("backgroundId", "1");
        if (json.has("itemResource")) {
            this.itemResource = json.getJSONObject("itemResource");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBackgroundId() {
        return backgroundId;
    }

    public void setBackgroundId(String backgroundId) {
        this.backgroundId = backgroundId;
        invalidateCache();
    }

    public JSONObject getItemResource() {
        return itemResource;
    }

    public void setItemResource(JSONObject itemResource) {
        this.itemResource = itemResource;
        invalidateCache();
    }

    /**
     * Invalidates the cached images, forcing regeneration on next access.
     */
    public void invalidateCache() {
        cachedImages = null;
    }

    /**
     * Gets the background for this icon.
     */
    public CustomIconGenerator.IconBackground getBackground() {
        return new CustomIconGenerator.IconBackground(backgroundId);
    }

    /**
     * Generates or returns cached icon images for all 3 states.
     * @return Array of [up, down, hover] images
     */
    public BufferedImage[] getImages() {
        if (cachedImages == null) {
            cachedImages = CustomIconGenerator.generateIconSet(getBackground(), itemResource);
        }
        return cachedImages;
    }

    /**
     * Gets a single state image.
     * @param state 0=up, 1=down, 2=hover
     */
    public BufferedImage getImage(int state) {
        BufferedImage[] images = getImages();
        if (state >= 0 && state < images.length) {
            return images[state];
        }
        return images[0];
    }

    /**
     * Gets the "up" state image for preview purposes.
     */
    public BufferedImage getPreviewImage() {
        return getImage(0);
    }

    /**
     * Serializes this icon to JSON.
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("backgroundId", backgroundId);
        if (itemResource != null) {
            json.put("itemResource", itemResource);
        }
        return json;
    }

    @Override
    public String toString() {
        return name != null ? name : "Unnamed Icon";
    }
}
