package nurgling.translation;

import haven.*;

/**
 * Hook for translating item names in tooltips and displays
 * Integrates with the existing item display system
 */
public class ItemTranslationHook {

    /**
     * Translate item name if translation is available
     */
    public static String translateItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return itemName;
        }

        // Use the translation manager to translate the item
        return TranslationManager.getInstance().translateItem(itemName);
    }

    /**
     * Hook into WItem tooltip to translate item names
     * This method can be called from WItem.tooltip() or similar methods
     */
    public static String translateTooltip(String originalTooltip) {
        if (originalTooltip == null) {
            return null;
        }

        // For now, just translate the entire string as an item name
        // This is a simple implementation - more sophisticated parsing could be added later
        return translateItemName(originalTooltip);
    }

    /**
     * Translate item name in inventory display
     */
    public static String translateInventoryItem(String itemName) {
        return translateItemName(itemName);
    }
}