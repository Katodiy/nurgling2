package nurgling.conf;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration settings for item quality overlay display.
 * Allows customizing corner position, font family, font size, and color.
 */
public class ItemQualityOverlaySettings implements JConf {
    
    /**
     * Quality threshold with associated color.
     * Items with quality >= threshold will use this color.
     */
    public static class QualityThreshold {
        public int threshold;
        public Color color;
        
        public QualityThreshold(int threshold, Color color) {
            this.threshold = threshold;
            this.color = color;
        }
        
        public QualityThreshold(Map<String, Object> map) {
            this.threshold = ((Number) map.get("threshold")).intValue();
            Map<String, Object> colorMap = (Map<String, Object>) map.get("color");
            this.color = new Color(
                ((Number) colorMap.get("red")).intValue(),
                ((Number) colorMap.get("green")).intValue(),
                ((Number) colorMap.get("blue")).intValue(),
                ((Number) colorMap.get("alpha")).intValue()
            );
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("threshold", threshold);
            Map<String, Object> colorMap = new HashMap<>();
            colorMap.put("red", color.getRed());
            colorMap.put("green", color.getGreen());
            colorMap.put("blue", color.getBlue());
            colorMap.put("alpha", color.getAlpha());
            ret.put("color", colorMap);
            return ret;
        }
        
        public QualityThreshold copy() {
            return new QualityThreshold(threshold, color);
        }
    }
    
    /**
     * Corner position for the quality overlay on items
     */
    public enum Corner {
        TOP_LEFT("Top Left"),
        TOP_RIGHT("Top Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_RIGHT("Bottom Right");
        
        public final String displayName;
        
        Corner(String displayName) {
            this.displayName = displayName;
        }
        
        public static Corner fromString(String name) {
            for (Corner c : values()) {
                if (c.name().equals(name) || c.displayName.equals(name)) {
                    return c;
                }
            }
            return BOTTOM_RIGHT; // default
        }
    }
    
    // Default values
    public Corner corner = Corner.BOTTOM_RIGHT;
    public String fontFamily = "Sans";
    public int fontSize = 10;
    public Color defaultColor = new Color(35, 245, 245, 255); // Default cyan color
    public Color contentColor = new Color(97, 121, 227, 255); // Blue for content quality
    public boolean showBackground = true;
    public Color backgroundColor = new Color(0, 0, 0, 115);   // Semi-transparent black
    public boolean showDecimal = false;                        // Show one decimal place
    public boolean showOutline = true;                         // Show text outline
    public Color outlineColor = Color.BLACK;                   // Outline color
    public int outlineWidth = 1;                               // Outline width (1-3)
    public boolean useThresholds = true;                       // Use quality thresholds for coloring
    public List<QualityThreshold> thresholds = new ArrayList<>(); // Quality thresholds (sorted by threshold desc)
    
    public ItemQualityOverlaySettings() {
        // Initialize with default thresholds
        thresholds.add(new QualityThreshold(100, new Color(255, 215, 0, 255)));   // Gold for Q >= 100
        thresholds.add(new QualityThreshold(50, new Color(50, 205, 50, 255)));    // Green for Q >= 50
        thresholds.add(new QualityThreshold(0, new Color(35, 245, 245, 255)));    // Cyan for Q >= 0 (default)
    }
    
    public ItemQualityOverlaySettings(Map<String, Object> map) {
        // Don't call default constructor - we'll load thresholds from map
        if (map.containsKey("corner")) {
            this.corner = Corner.fromString((String) map.get("corner"));
        }
        if (map.containsKey("fontFamily")) {
            this.fontFamily = (String) map.get("fontFamily");
        }
        if (map.containsKey("fontSize")) {
            this.fontSize = ((Number) map.get("fontSize")).intValue();
        }
        if (map.containsKey("defaultColor")) {
            this.defaultColor = colorFromMap((Map<String, Object>) map.get("defaultColor"));
        }
        // Legacy support for old itemColor field
        if (map.containsKey("itemColor") && !map.containsKey("defaultColor")) {
            this.defaultColor = colorFromMap((Map<String, Object>) map.get("itemColor"));
        }
        if (map.containsKey("contentColor")) {
            this.contentColor = colorFromMap((Map<String, Object>) map.get("contentColor"));
        }
        if (map.containsKey("showBackground")) {
            this.showBackground = (Boolean) map.get("showBackground");
        }
        if (map.containsKey("backgroundColor")) {
            this.backgroundColor = colorFromMap((Map<String, Object>) map.get("backgroundColor"));
        }
        if (map.containsKey("showDecimal")) {
            this.showDecimal = (Boolean) map.get("showDecimal");
        }
        if (map.containsKey("showOutline")) {
            this.showOutline = (Boolean) map.get("showOutline");
        }
        if (map.containsKey("outlineColor")) {
            this.outlineColor = colorFromMap((Map<String, Object>) map.get("outlineColor"));
        }
        if (map.containsKey("outlineWidth")) {
            this.outlineWidth = ((Number) map.get("outlineWidth")).intValue();
        }
        if (map.containsKey("useThresholds")) {
            this.useThresholds = (Boolean) map.get("useThresholds");
        }
        if (map.containsKey("thresholds")) {
            List<Map<String, Object>> thresholdsList = (List<Map<String, Object>>) map.get("thresholds");
            for (Map<String, Object> t : thresholdsList) {
                thresholds.add(new QualityThreshold(t));
            }
        } else {
            // Default thresholds if not in config
            thresholds.add(new QualityThreshold(100, new Color(255, 215, 0, 255)));
            thresholds.add(new QualityThreshold(50, new Color(50, 205, 50, 255)));
            thresholds.add(new QualityThreshold(0, new Color(35, 245, 245, 255)));
        }
    }
    
    private Color colorFromMap(Map<String, Object> map) {
        return new Color(
            ((Number) map.get("red")).intValue(),
            ((Number) map.get("green")).intValue(),
            ((Number) map.get("blue")).intValue(),
            ((Number) map.get("alpha")).intValue()
        );
    }
    
    private Map<String, Object> colorToMap(Color color) {
        Map<String, Object> map = new HashMap<>();
        map.put("red", color.getRed());
        map.put("green", color.getGreen());
        map.put("blue", color.getBlue());
        map.put("alpha", color.getAlpha());
        return map;
    }
    
    @Override
    public JSONObject toJson() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("type", "ItemQualityOverlaySettings");
        ret.put("corner", corner.name());
        ret.put("fontFamily", fontFamily);
        ret.put("fontSize", fontSize);
        ret.put("defaultColor", colorToMap(defaultColor));
        ret.put("contentColor", colorToMap(contentColor));
        ret.put("showBackground", showBackground);
        ret.put("backgroundColor", colorToMap(backgroundColor));
        ret.put("showDecimal", showDecimal);
        ret.put("showOutline", showOutline);
        ret.put("outlineColor", colorToMap(outlineColor));
        ret.put("outlineWidth", outlineWidth);
        ret.put("useThresholds", useThresholds);
        List<Map<String, Object>> thresholdsList = new ArrayList<>();
        for (QualityThreshold t : thresholds) {
            thresholdsList.add(t.toMap());
        }
        ret.put("thresholds", thresholdsList);
        return new JSONObject(ret);
    }
    
    /**
     * Create a copy of settings for preview/temporary modifications
     */
    public ItemQualityOverlaySettings copy() {
        ItemQualityOverlaySettings copy = new ItemQualityOverlaySettings();
        copy.corner = this.corner;
        copy.fontFamily = this.fontFamily;
        copy.fontSize = this.fontSize;
        copy.defaultColor = this.defaultColor;
        copy.contentColor = this.contentColor;
        copy.showBackground = this.showBackground;
        copy.backgroundColor = this.backgroundColor;
        copy.showDecimal = this.showDecimal;
        copy.showOutline = this.showOutline;
        copy.outlineColor = this.outlineColor;
        copy.outlineWidth = this.outlineWidth;
        copy.useThresholds = this.useThresholds;
        copy.thresholds = new ArrayList<>();
        for (QualityThreshold t : this.thresholds) {
            copy.thresholds.add(t.copy());
        }
        return copy;
    }
    
    /**
     * Get color for a given quality value based on thresholds
     */
    public Color getColorForQuality(double quality) {
        if (!useThresholds || thresholds.isEmpty()) {
            return defaultColor;
        }
        
        // Thresholds should be sorted by threshold descending
        // Find the first threshold where quality >= threshold
        for (QualityThreshold t : thresholds) {
            if (quality >= t.threshold) {
                return t.color;
            }
        }
        
        return defaultColor;
    }
    
    /**
     * Sort thresholds by threshold value descending
     */
    public void sortThresholds() {
        thresholds.sort((a, b) -> Integer.compare(b.threshold, a.threshold));
    }
}

