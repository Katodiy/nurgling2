package nurgling.translation;

/**
 * Enumeration of translation types for the hybrid translation system
 */
public enum TranslationType {
    /**
     * Static UI elements (menus, buttons, labels)
     * Uses resource bundle approach with properties files
     */
    UI_STATIC,

    /**
     * Dynamic item names from server (VSpec items, etc.)
     * Uses JSON dictionary lookup
     */
    ITEM_DYNAMIC,

    /**
     * Dynamic skill names from server
     * Uses JSON dictionary lookup
     */
    SKILL_DYNAMIC,

    /**
     * Auto-detect translation type based on content
     * Tries multiple translation methods
     */
    AUTO_DETECT
}