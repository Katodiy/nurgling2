package nurgling.tools;

import haven.Coord;
import haven.Coord2d;

import java.awt.Color;

/**
 * Represents a fixed directional vector for triangulation
 * Vector is stored in absolute tile coordinates so it doesn't move when traveling
 */
public class DirectionalVector {
    /** Fixed tile coordinate where the vector originates (segment-relative) */
    public final Coord originTileCoords;

    /** Tile coordinate of the target (segment-relative) */
    public final Coord targetTileCoords;

    /** Name of the target (for display purposes) */
    public final String targetName;

    /** Target gob ID if known, -1 otherwise */
    public final long targetGobId;

    /** Color for this vector */
    public final Color color;

    /** Colors to cycle through for different vector pairs */
    private static final Color[] COLORS = {
        new Color(100, 150, 255, 200),  // Blue
        new Color(255, 100, 100, 200),  // Red
        new Color(100, 255, 100, 200),  // Green
        new Color(255, 200, 50, 200),   // Orange/Yellow
        new Color(200, 100, 255, 200),  // Purple
        new Color(100, 255, 255, 200),  // Cyan
        new Color(255, 100, 200, 200),  // Pink
        new Color(255, 255, 100, 200),  // Yellow
    };

    /** Index for color cycling */
    private static int colorIndex = 0;

    /** Get next color in cycle (use same color for both edges of a pair) */
    public static synchronized Color getNextColor() {
        Color c = COLORS[colorIndex];
        colorIndex = (colorIndex + 1) % COLORS.length;
        return c;
    }

    /** Reset color cycle (called when vectors are cleared) */
    public static synchronized void resetColorCycle() {
        colorIndex = 0;
    }

    public DirectionalVector(Coord originTileCoords, Coord targetTileCoords, String targetName, long targetGobId, Color color) {
        this.originTileCoords = originTileCoords;
        this.targetTileCoords = targetTileCoords;
        this.targetName = targetName;
        this.targetGobId = targetGobId;
        this.color = color;
    }

    /** Constructor without color (uses default blue) */
    public DirectionalVector(Coord originTileCoords, Coord targetTileCoords, String targetName, long targetGobId) {
        this(originTileCoords, targetTileCoords, targetName, targetGobId, COLORS[0]);
    }

    /**
     * Get direction vector in tile space (normalized)
     */
    public Coord2d getDirection() {
        Coord diff = targetTileCoords.sub(originTileCoords);
        return new Coord2d(diff).norm();
    }

    /**
     * Get a tile coordinate along the vector at a given distance from origin
     */
    public Coord2d getTilePointAt(double distance) {
        Coord2d dir = getDirection();
        return new Coord2d(originTileCoords).add(dir.mul(distance));
    }
}
