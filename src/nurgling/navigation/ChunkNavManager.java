package nurgling.navigation;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.profiles.ProfileManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * Manages the chunk navigation system lifecycle.
 * Handles initialization, persistence, and coordination between components.
 */
public class ChunkNavManager {
    private ChunkNavGraph graph;
    private ChunkNavRecorder recorder;
    private ChunkNavPlanner planner;
    private PortalTraversalTracker portalTracker;

    private boolean enabled = true;
    private boolean initialized = false;
    private String currentGenus = null;

    // Throttle saves to avoid excessive disk writes
    private long lastSaveTime = 0;
    private static final long SAVE_THROTTLE_MS = 2000; // Min 2 seconds between saves (for testing)

    // Throttle grid recording to avoid excessive CPU usage
    private long lastRecordTime = 0;
    private static final long RECORD_THROTTLE_MS = 2000; // Record visible grids every 2 seconds

    // Singleton instance
    private static ChunkNavManager instance;

    public ChunkNavManager() {
        this.graph = new ChunkNavGraph();
        this.recorder = new ChunkNavRecorder(graph);
        this.planner = new ChunkNavPlanner(graph);
        this.portalTracker = new PortalTraversalTracker(graph, recorder);

        // Register shutdown hook to save on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("ChunkNavManager: Shutdown hook triggered, saving...");
            shutdown();
        }));
    }

    public static ChunkNavManager getInstance() {
        if (instance == null) {
            instance = new ChunkNavManager();
        }
        return instance;
    }

    /**
     * Initialize the navigation system for a specific world.
     */
    public void initialize(String genus) {
        if (genus == null || genus.isEmpty()) {
            System.err.println("ChunkNavManager: Cannot initialize with null genus");
            return;
        }

        // Check if already initialized for this world
        if (initialized && genus.equals(currentGenus)) {
            return;
        }

        // Save previous world data if switching
        if (initialized && currentGenus != null && !genus.equals(currentGenus)) {
            save();
        }

        this.currentGenus = genus;
        this.graph = new ChunkNavGraph();
        this.recorder = new ChunkNavRecorder(graph);
        this.planner = new ChunkNavPlanner(graph);
        this.portalTracker = new PortalTraversalTracker(graph, recorder);

        // Load saved data
        load();

        this.initialized = true;
        System.out.println("ChunkNavManager: Initialized for world " + genus + " - " + graph.getStats());
    }

    /**
     * Called when a grid becomes visible.
     */
    public void onGridLoaded(MCache.Grid grid) {
        if (grid == null) return;

        if (!enabled || !initialized) {
            return;
        }

        try {
            recorder.recordGrid(grid);
            // Always save (throttled) since we now re-sample walkability each time
            saveThrottled();
        } catch (Exception e) {
            // Ignore recording errors
        }
    }

    /**
     * Called periodically to check for portal traversals and record visible grids.
     * Should be called from game loop or a polling task.
     */
    public void tick() {
        if (!enabled || !initialized) return;

        try {
            portalTracker.tick();
        } catch (Exception e) {
            // Ignore - player might not exist
        }

        // Periodically record all visible grids (not just newly loaded ones)
        long now = System.currentTimeMillis();
        if (now - lastRecordTime >= RECORD_THROTTLE_MS) {
            lastRecordTime = now;
            recordVisibleGrids();
        }
    }

    /**
     * Record all currently visible grids.
     * This catches grids that were already loaded when player walks through them.
     */
    private void recordVisibleGrids() {
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            if (mcache == null) return;

            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid != null && grid.ul != null) {
                        recorder.recordGrid(grid);
                    }
                }
            }
            saveThrottled();
        } catch (Exception e) {
            // Ignore - game state may not be ready
        }
    }

    /**
     * Called when player passes through a portal.
     */
    public void onPortalTraversal(String gobHash, long fromGridId, long toGridId) {
        if (!enabled || !initialized) return;

        recorder.recordPortalTraversal(gobHash, fromGridId, toGridId);
        // Save when portal connections are learned
        saveThrottled();
    }

    /**
     * Plan a path to an area.
     * Returns the chunk-level path. PathFinder handles actual navigation within grids.
     */
    public ChunkPath planToArea(NArea area) {
        if (!enabled || !initialized) return null;

        ChunkPath path = planner.planToArea(area);
        if (path == null || path.isEmpty()) {
            return null;
        }

        // No intra-chunk validation needed:
        // - Adjacent grids: you walk across the edge where they touch
        // - Building connections: you use doors/portals
        // PathFinder handles actual route within each grid

        return path;
    }

    /**
     * Navigate to an area using the chunk navigation system.
     */
    public nurgling.actions.Results navigateToArea(NArea area, NGameUI gui) throws InterruptedException {
        if (!enabled || !initialized) {
            return nurgling.actions.Results.FAIL();
        }

        ChunkPath path = planToArea(area);
        if (path == null || path.isEmpty()) {
            return nurgling.actions.Results.FAIL();
        }

        ChunkNavExecutor executor = new ChunkNavExecutor(path, area, this);
        return executor.run(gui);
    }

    /**
     * Check if we have navigation data for an area.
     */
    public boolean hasDataForArea(NArea area) {
        if (!enabled || !initialized || area == null) return false;

        Set<Long> chunks = graph.getChunksForArea(area.id);
        return !chunks.isEmpty();
    }

    /**
     * Update area reachability from visible chunks.
     */
    public void updateAreaReachability(NArea area) {
        if (!enabled || !initialized || area == null) return;

        // Get all visible chunks
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    ChunkNavData chunk = graph.getChunk(grid.id);
                    if (chunk != null) {
                        // Check if area is reachable from this chunk
                        if (isAreaReachableFromChunk(chunk, area)) {
                            chunk.reachableAreaIds.add(area.id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Check if an area is reachable from a chunk (simple distance check).
     */
    private boolean isAreaReachableFromChunk(ChunkNavData chunk, NArea area) {
        try {
            Coord2d areaCenter = area.getCenter2d();
            if (areaCenter == null) return false;

            MCache mcache = NUtils.getGameUI().map.glob.map;
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid.id == chunk.gridId) {
                        Coord2d chunkCenter = grid.ul.add(CHUNK_SIZE / 2, CHUNK_SIZE / 2).mul(MCache.tilesz);
                        return chunkCenter.dist(areaCenter) < MAX_EDGE_DISTANCE;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Save navigation data to disk (throttled version).
     * Won't save more often than SAVE_THROTTLE_MS.
     */
    public void saveThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime < SAVE_THROTTLE_MS) {
            return; // Too soon since last save
        }
        save();
    }

    /**
     * Save navigation data to disk.
     */
    public void save() {
        if (!initialized || currentGenus == null) return;

        try {
            Path filePath = getStoragePath();
            Files.createDirectories(filePath.getParent());

            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("genus", currentGenus);
            root.put("lastSaved", System.currentTimeMillis());

            // Save chunks
            JSONArray chunksArray = new JSONArray();
            for (ChunkNavData chunk : graph.getAllChunks()) {
                chunksArray.put(chunk.toJson());
            }
            root.put("chunks", chunksArray);

            // Write to file
            Files.write(filePath, root.toString(2).getBytes(StandardCharsets.UTF_8));
            lastSaveTime = System.currentTimeMillis();
            System.out.println("ChunkNavManager: Saved " + graph.getChunkCount() + " chunks to " + filePath);

        } catch (Exception e) {
            System.err.println("ChunkNavManager: Error saving: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load navigation data from disk.
     */
    public void load() {
        if (currentGenus == null) return;

        try {
            Path filePath = getStoragePath();
            if (!Files.exists(filePath)) {
                System.out.println("ChunkNavManager: No saved data found at " + filePath);
                return;
            }

            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(content);

            // Verify genus matches
            String savedGenus = root.optString("genus", "");
            if (!currentGenus.equals(savedGenus)) {
                System.out.println("ChunkNavManager: Saved data is for different world, ignoring");
                return;
            }

            // Load chunks
            JSONArray chunksArray = root.getJSONArray("chunks");
            for (int i = 0; i < chunksArray.length(); i++) {
                try {
                    ChunkNavData chunk = ChunkNavData.fromJson(chunksArray.getJSONObject(i));
                    graph.addChunk(chunk);
                } catch (Exception e) {
                    System.err.println("ChunkNavManager: Error loading chunk: " + e.getMessage());
                }
            }

            // Rebuild connections after loading all chunks
            graph.rebuildAllConnections();

            System.out.println("ChunkNavManager: Loaded " + graph.getChunkCount() + " chunks from " + filePath);

        } catch (Exception e) {
            System.err.println("ChunkNavManager: Error loading: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the storage path for navigation data.
     */
    private Path getStoragePath() {
        ProfileManager pm = new ProfileManager(currentGenus);
        return pm.getConfigPath(STORAGE_FILENAME);
    }

    /**
     * Clear all navigation data.
     */
    public void clear() {
        graph.clear();
        recorder.clearSession();
        System.out.println("ChunkNavManager: Cleared all navigation data");
    }

    /**
     * Get statistics about the navigation system.
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChunkNav Status:\n");
        sb.append("  Enabled: ").append(enabled).append("\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  World: ").append(currentGenus).append("\n");
        sb.append("  ").append(graph.getStats()).append("\n");
        sb.append("  ").append(recorder.getStats());
        return sb.toString();
    }

    // Getters

    public ChunkNavGraph getGraph() {
        return graph;
    }

    public ChunkNavRecorder getRecorder() {
        return recorder;
    }

    public ChunkNavPlanner getPlanner() {
        return planner;
    }

    public PortalTraversalTracker getPortalTracker() {
        return portalTracker;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getCurrentGenus() {
        return currentGenus;
    }

    /**
     * Shutdown and save.
     */
    public void shutdown() {
        if (initialized) {
            save();
            initialized = false;
        }
    }

    /**
     * Force update connections between all visible chunks.
     */
    public void updateAllConnections() {
        if (!initialized) return;

        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            List<ChunkNavData> visibleChunks = new ArrayList<>();

            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    ChunkNavData chunk = graph.getChunk(grid.id);
                    if (chunk != null) {
                        visibleChunks.add(chunk);
                    }
                }
            }

            // Update connections between all visible chunk pairs
            for (int i = 0; i < visibleChunks.size(); i++) {
                for (int j = i + 1; j < visibleChunks.size(); j++) {
                    recorder.updateEdgeConnectivity(visibleChunks.get(i), visibleChunks.get(j));
                }
            }

        } catch (Exception e) {
            // Ignore
        }
    }
}
