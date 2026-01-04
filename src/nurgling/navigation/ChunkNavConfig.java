package nurgling.navigation;

/**
 * Configuration constants for the chunk-based navigation system.
 */
public class ChunkNavConfig {
    // Recording - half-tile resolution (2x2 cells per game tile, matching NPFMap)
    public static final int CELLS_PER_TILE = 2;             // Cells per tile dimension (2x2 = 4 cells per tile)
    public static final int CHUNK_SIZE = 100;               // Tiles per chunk (game grid size, unchanged)
    public static final int CELLS_PER_EDGE = CHUNK_SIZE * CELLS_PER_TILE;  // 200 cells along chunk edge

    // Confidence
    public static final float INITIAL_CONFIDENCE = 1.0f;
    public static final float CONFIDENCE_HALF_LIFE_HOURS = 48f;

    // Path planning costs
    public static final float BASE_CHUNK_COST = 100f;       // Base cost per chunk
    public static final float WALKABILITY_PENALTY = 20f;    // Per walkability point
    public static final float UNCERTAINTY_PENALTY = 50f;    // For low confidence
    public static final float PORTAL_TRAVERSAL_COST = 30f;  // Door/stairs overhead

    // Execution
    public static final int MAX_REPLAN_ATTEMPTS = 3;
    public static final long PORTAL_LOAD_TIMEOUT_MS = 10000;

    // Storage
    public static final String STORAGE_FILENAME = "chunknav.nurgling.json";  // Legacy JSON file (for migration)
    public static final String STORAGE_DIRNAME = "chunknav";                  // Binary chunk directory
}
