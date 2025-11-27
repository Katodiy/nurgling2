package nurgling.tasks;

import haven.ItemInfo;
import haven.Text;
import haven.WItem;

public class WaitPotFilled extends NTask {
    
    WItem pot;
    double requiredWater;
    
    public WaitPotFilled(WItem pot, double requiredWater) {
        this.pot = pot;
        this.requiredWater = requiredWater;
    }
    
    public WaitPotFilled(WItem pot) {
        this(pot, 1.0); // Default 1 liter
    }
    
    @Override
    public boolean check() {
        if (pot == null || pot.item == null) {
            return false;
        }
        
        // Check ItemInfo$AdHoc for text content
        if (pot.item.info != null) {
            for (ItemInfo inf : pot.item.info) {
                if (inf instanceof ItemInfo.AdHoc) {
                    ItemInfo.AdHoc adHoc = (ItemInfo.AdHoc) inf;
                    if (adHoc.str != null && adHoc.str instanceof Text.Line) {
                        Text.Line line = (Text.Line) adHoc.str;
                        String text = line.text;
                        
                        // Check if text contains "Water: X.XX/1 l" pattern
                        if (text != null && text.contains("Water:")) {
                            // Parse water amount from text like "Soil: 4/4, Water: 1.00/1 l"
                            try {
                                String[] parts = text.split("Water:");
                                if (parts.length > 1) {
                                    String waterPart = parts[1].trim();
                                    // Extract "1.00" from "1.00/1 l"
                                    String[] waterValues = waterPart.split("/");
                                    if (waterValues.length > 0) {
                                        String currentWaterStr = waterValues[0].trim();
                                        double currentWater = Double.parseDouble(currentWaterStr);
                                        return currentWater >= requiredWater;
                                    }
                                }
                            } catch (Exception e) {
                                // If parsing fails, continue checking
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
}
