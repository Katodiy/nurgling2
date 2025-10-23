package nurgling.tools;

import haven.Coord;
import haven.Coord2d;

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

    public DirectionalVector(Coord originTileCoords, Coord targetTileCoords, String targetName, long targetGobId) {
        this.originTileCoords = originTileCoords;
        this.targetTileCoords = targetTileCoords;
        this.targetName = targetName;
        this.targetGobId = targetGobId;
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

    /**
     * Check if this vector is pointing toward the same target as another vector
     */
    public boolean sameTarget(DirectionalVector other) {
        if(targetTileCoords != null && other.targetTileCoords != null) {
            return targetTileCoords.equals(other.targetTileCoords);
        }
        if(targetGobId >= 0 && other.targetGobId >= 0) {
            return targetGobId == other.targetGobId;
        }
        return false;
    }
}
