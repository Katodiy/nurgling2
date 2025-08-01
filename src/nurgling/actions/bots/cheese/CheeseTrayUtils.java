package nurgling.actions.bots.cheese;

import haven.WItem;
import nurgling.NGItem;

import java.util.List;

/**
 * Utilities for properly inspecting cheese tray contents
 */
public class CheeseTrayUtils {
    
    /**
     * Check if a cheese tray is empty (no content)
     */
    public static boolean isEmpty(WItem tray) {
        if (tray == null) return true;
        
        List<NGItem.NContent> contents = ((NGItem) tray.item).content();
        return contents == null || contents.isEmpty();
    }
    
    /**
     * Get the content name of what's inside a cheese tray
     * @param tray The cheese tray WItem
     * @return The name of the content (e.g., "Sheep's Curd", "Feta", etc.) or null if empty
     */
    public static String getContentName(WItem tray) {
        if (tray == null) return null;
        
        List<NGItem.NContent> contents = ((NGItem) tray.item).content();
        if (contents == null || contents.isEmpty()) {
            return null; // Empty tray
        }
        
        // Return the name of the first content item
        return contents.get(0).name();
    }
    
    /**
     * Check if a cheese tray contains a specific type of curd or cheese
     */
    public static boolean containsContent(WItem tray, String contentName) {
        String actualContent = getContentName(tray);
        return actualContent != null && actualContent.equals(contentName);
    }
    
    /**
     * Check if a cheese tray contains any type of curd
     */
    public static boolean containsCurd(WItem tray) {
        String content = getContentName(tray);
        return content != null && content.contains("Curd");
    }
    
    /**
     * Get the curd type from a tray (e.g., "Sheep's", "Cow's", "Goat's")
     */
    public static String getCurdType(WItem tray) {
        String content = getContentName(tray);
        if (content != null && content.contains("Curd")) {
            // Extract the type before "'s Curd"
            return content.replace(" Curd", "").replace("'s", "'s");
        }
        return null;
    }
    
    /**
     * Check if the tray contains cheese ready for next stage or slicing
     */
    public static boolean containsAgedCheese(WItem tray) {
        String content = getContentName(tray);
        return content != null && !content.contains("Curd"); // Not curd = aged cheese
    }
}