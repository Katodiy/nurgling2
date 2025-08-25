package nurgling;

import haven.*;
import nurgling.widgets.AddResourceTimerWidget;

/**
 * Utility class for resource timer functionality
 */
public class ResourceTimerUtils {
    
    /**
     * Show resource timer dialog - uses persistent widget in NGameUI
     */
    public static void showResourceTimerDialog(MapFile.SMarker marker, MiniMap.Location loc) {
        String displayName = marker.nm != null ? marker.nm : marker.res.name;
        
        NGameUI gui = (NGameUI) NUtils.getGameUI();
        if(gui != null) {
            AddResourceTimerWidget widget = gui.getAddResourceTimerWidget();
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