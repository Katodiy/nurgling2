package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Custom area matcher that can distinguish between empty and filled cheese trays
 * by checking their resource paths instead of just display names.
 */
public class CheeseAreaMatcher {
    
    /**
     * Find the appropriate output area for a cheese tray based on its actual resource path
     */
    public static NArea findAreaForCheeseTray(WItem tray, double quality) {
        if (tray == null) return null;
        
        String resourcePath = tray.item.res.get().name;
        
        // Determine the appropriate area name based on resource path
        String targetAreaName;
        if (resourcePath.contains("cheesetray-curd")) {
            targetAreaName = "Full Cheese Tray";
        } else if (resourcePath.equals("gfx/invobjs/cheesetray")) {
            targetAreaName = "Empty Cheese Tray"; 
        } else {
            // Handle other cheese tray variants if they exist
            targetAreaName = "Cheese Tray";
        }
        
        return findAreaByResourceMatch(resourcePath, quality);
    }
    
    /**
     * Find area that matches the specific resource path for cheese trays
     */
    private static NArea findAreaByResourceMatch(String resourcePath, double quality) {
        if (nurgling.NUtils.getGameUI() == null || nurgling.NUtils.getGameUI().map == null) {
            return null;
        }
        
        for (Integer areaId : nurgling.NUtils.getGameUI().map.nols.keySet()) {
            if (areaId > 0) {
                NArea area = nurgling.NUtils.getGameUI().map.glob.map.areas.get(areaId);
                if (area != null && area.jout != null) {
                    // Check each output configuration in this area
                    for (int i = 0; i < area.jout.length(); i++) {
                        JSONObject output = (JSONObject) area.jout.get(i);
                        if (output.has("static")) {
                            String staticResource = output.getString("static");
                            // Match by resource path instead of name
                            if (staticResource.equals(resourcePath)) {
                                // Check quality threshold if specified
                                if (output.has("th")) {
                                    double threshold = output.getDouble("th");
                                    if (quality >= threshold) {
                                        return area;
                                    }
                                } else {
                                    return area;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Transfer a cheese tray to its appropriate area based on resource path
     */
    public static class TransferCheeseTraysToCorrectAreas implements Action {
        
        @Override
        public Results run(NGameUI gui) throws InterruptedException {
            ArrayList<WItem> cheeseTrays = gui.getInventory().getItems(new NAlias("Cheese Tray"));
            
            for (WItem tray : cheeseTrays) {
                double quality = ((NGItem) tray.item).quality != null ? 
                    ((NGItem) tray.item).quality : 1.0;
                    
                NArea targetArea = findAreaForCheeseTray(tray, quality);
                if (targetArea != null) {
                    // Transfer this specific tray to the found area
                    transferTrayToArea(gui, tray, targetArea);
                }
            }
            
            return Results.SUCCESS();
        }
        
        private void transferTrayToArea(NGameUI gui, WItem tray, NArea area) throws InterruptedException {
            // Implementation for transferring a specific tray to a specific area
            // This would need to be integrated with your existing transfer system
            String resourcePath = tray.item.res.get().name;
            gui.msg("Would transfer tray (" + resourcePath + ") to area: " + area.name);
        }
    }
}