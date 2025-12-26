package nurgling.navigation;

/**
 * Type-safe representation of world layers (surface, inside, cellar, mines, etc.)
 */
public enum Layer {
    SURFACE("surface"),
    INSIDE("inside"),
    CELLAR("cellar"),
    MINE_1("mine1"),
    MINE_2("mine2"),
    MINE_3("mine3"),
    MINE_4("mine4"),
    MINE_5("mine5"),
    MINE_6("mine6"),
    MINE_7("mine7"),
    MINE_8("mine8"),
    MINE_9("mine9"),
    MINE_10("mine10"),
    UNKNOWN("unknown");

    private final String stringValue;

    Layer(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Check if this layer is a mine layer.
     */
    public boolean isMine() {
        return this.name().startsWith("MINE_");
    }

    /**
     * Get the mine level (1-10) or 0 if not a mine.
     */
    public int getMineLevel() {
        if (!isMine()) return 0;
        String numStr = this.name().substring(5); // After "MINE_"
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    /**
     * Parse a string to a Layer enum value.
     * Handles legacy string formats like "mine1", "mine2", etc.
     */
    public static Layer fromString(String s) {
        if (s == null) return SURFACE;

        String lower = s.toLowerCase().trim();

        switch (lower) {
            case "surface": return SURFACE;
            case "inside": return INSIDE;
            case "cellar": return CELLAR;
            case "unknown": return UNKNOWN;
        }

        // Handle mine layers with variable numbers
        if (lower.startsWith("mine")) {
            String numStr = lower.substring(4);
            if (numStr.isEmpty()) return MINE_1;
            try {
                int level = Integer.parseInt(numStr);
                return fromMineLevel(level);
            } catch (NumberFormatException e) {
                return MINE_1;
            }
        }

        return SURFACE; // Default
    }

    /**
     * Get the Layer for a specific mine level.
     */
    public static Layer fromMineLevel(int level) {
        switch (level) {
            case 1: return MINE_1;
            case 2: return MINE_2;
            case 3: return MINE_3;
            case 4: return MINE_4;
            case 5: return MINE_5;
            case 6: return MINE_6;
            case 7: return MINE_7;
            case 8: return MINE_8;
            case 9: return MINE_9;
            case 10: return MINE_10;
            default:
                if (level < 1) return MINE_1;
                return MINE_10;
        }
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
