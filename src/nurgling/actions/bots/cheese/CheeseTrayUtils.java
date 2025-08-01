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
}