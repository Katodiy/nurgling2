package nurgling;

import haven.*;
import nurgling.widgets.ResourceTimerWidget;
import nurgling.NUtils;

/**
 * Shared utility class for resource timer dialog functionality
 */
public class ResourceTimerDialogs {
    
    /**
     * Show resource timer dialog - creates in-game widget
     */
    public static void showResourceTimerDialog(MapFile.SMarker marker, MiniMap.Location loc) {
        String resourceDisplayName = getResourceDisplayName(marker.res.name);
        
        // Create and show the in-game widget
        ResourceTimerWidget widget = new ResourceTimerWidget(marker, loc, resourceDisplayName);
        
        // Get the game UI and add the widget
        GameUI gui = NUtils.getGameUI();
        if(gui != null) {
            gui.add(widget, new Coord(200, 200)); // Position the widget
        }
    }
    
    /**
     * Convert resource type to user-friendly display name
     */
    public static String getResourceDisplayName(String resourceType) {
        // Convert resource paths to user-friendly names
        String name = resourceType;
        if(name.startsWith("gfx/terobjs/map/")) {
            name = name.substring("gfx/terobjs/map/".length());
        }
        
        // Convert common resource names to proper display names
        switch(name) {
            case "tarpit": return "Tar Pit";
            case "jotunclam": return "Jotun Clam";
            case "cavepuddle": return "Cave Puddle";
            case "squirrelcache": return "Squirrel Cache";
            case "naturalminesupport": return "Natural Mine Support";
            case "saltbasin": return "Salt Basin";
            case "wellspring": return "Wellspring";
            case "coralreef": return "Coral Reef";
            case "claypit": return "Clay Pit";
            case "rockcrystal": return "Rock Crystal";
            case "lilypadlotus": return "Lilypad Lotus";
            case "abyssalchasm": return "Abyssal Chasm";
            case "ancientwindthrow": return "Ancient Windthrow";
            default:
                // Default formatting: capitalize and replace underscores
                name = name.replace("_", " ");
                if(name.length() > 0) {
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
                return name;
        }
    }
    
    /**
     * Check if a resource type is a localized resource that supports timers
     */
    public static boolean isTimerSupportedResource(String resourceType) {
        return resourceType != null && resourceType.startsWith("gfx/terobjs/mm");
    }
}