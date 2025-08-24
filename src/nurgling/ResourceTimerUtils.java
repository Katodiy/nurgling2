package nurgling;

import haven.*;
import nurgling.widgets.ResourceTimerWidget;

/**
 * Utility class for resource timer functionality
 */
public class ResourceTimerUtils {
    
    /**
     * Show resource timer dialog - uses persistent widget in NGameUI
     */
    public static void showResourceTimerDialog(MapFile.SMarker marker, MiniMap.Location loc) {
        // Use the marker's name (.nm) for display, fallback to processed resource name if null
        String displayName = marker.nm != null ? marker.nm : marker.res.name;
        
        // Get the persistent widget from NGameUI
        NGameUI gui = (NGameUI) NUtils.getGameUI();
        if(gui != null) {
            ResourceTimerWidget widget = gui.getResourceTimerWidget();
            if(widget != null) {
                widget.showForMarker(marker, loc, displayName);
            }
        }
    }
    
    /**
     * Check if a resource type is a localized resource that supports timers
     */
    public static boolean isTimerSupportedResource(String resourceType) {
        return resourceType != null && resourceType.startsWith("gfx/terobjs/mm");
    }
}