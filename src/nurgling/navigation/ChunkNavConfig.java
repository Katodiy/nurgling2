package nurgling.navigation;

/**
 * Configuration constants for the chunk-based navigation system.
 */
public class ChunkNavConfig {
    // Recording
    public static final int COARSE_CELL_SIZE = 4;           // Tiles per walkability cell
    public static final int CELLS_PER_EDGE = 25;            // Cells along chunk edge (100 / 4)
    public static final int CHUNK_SIZE = 100;               // Tiles per chunk

    // Confidence
    public static final float INITIAL_CONFIDENCE = 1.0f;
    public static final float CONFIDENCE_HALF_LIFE_HOURS = 48f;
    public static final float MIN_CONFIDENCE_FOR_PLANNING = 0.1f;

    // Path planning costs
    public static final float BASE_CHUNK_COST = 100f;       // Base cost per chunk
    public static final float WALKABILITY_PENALTY = 20f;    // Per walkability point
    public static final float UNCERTAINTY_PENALTY = 50f;    // For low confidence
    public static final float PORTAL_TRAVERSAL_COST = 30f;  // Door/stairs overhead

    // Execution
    public static final int MAX_REPLAN_ATTEMPTS = 3;
    public static final long PORTAL_LOAD_TIMEOUT_MS = 10000;

    // Storage
    public static final String STORAGE_FILENAME = "chunknav.nurgling.json";

    // Edge connectivity
    public static final double MAX_EDGE_DISTANCE = 250.0;   // Max distance for edge connections
}
