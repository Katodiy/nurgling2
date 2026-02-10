package nurgling.navigation;

/**
 * Type-safe representation of world layers.
 * - OUTSIDE: Surface and mines (walkable between grids)
 * - INSIDE: Building interiors
 * - CELLAR: Cellars
 */
public enum Layer {
    OUTSIDE("outside"),
    INSIDE("inside"),
    CELLAR("cellar"),
    UNKNOWN("unknown");

    private final String stringValue;

    Layer(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Parse a string to a Layer enum value.
     */
    public static Layer fromString(String s) {
        if (s == null) return OUTSIDE;

        switch (s.toLowerCase().trim()) {
            case "outside": return OUTSIDE;
            case "inside": return INSIDE;
            case "cellar": return CELLAR;
            case "unknown": return UNKNOWN;
            default: return OUTSIDE;
        }
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
