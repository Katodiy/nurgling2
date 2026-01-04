package nurgling.navigation;

import haven.*;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.overlays.map.MinimapChunkNavRenderer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

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
    private ChunkNavFileStore fileStore;

    private boolean enabled = true;
    private boolean initialized = false;
    private String currentGenus = null;

    // Throttle saves to avoid excessive disk writes
    private long lastSaveTime = 0;
    private static final long SAVE_THROTTLE_MS = 2000; // Min 2 seconds between saves (for testing)

    // Throttle grid recording to avoid excessive CPU usage
    private long lastRecordTime = 0;
    private static final long RECORD_THROTTLE_MS = 2000; // Record visible grids every 2 seconds

    // Portal visualization
    private boolean showPortalsEnabled = true; // Show portals by default
    private long lastPortalRefreshTime = 0;
    private static final long PORTAL_REFRESH_MS = 2000; // Refresh portal display every 3 seconds

    // Background thread for recording (to avoid FPS drops)
    private ExecutorService recordingExecutor;
    private volatile boolean recordingInProgress = false;
    private volatile boolean saveInProgress = false;

    // Guard flag to prevent saves during initialization (prevents Bug #3 - empty graph race condition)
    private volatile boolean initializationInProgress = false;

    // Instance reference - managed by NMapView, not a traditional singleton
    // This static reference exists for backward compatibility with code that
    // cannot easily access gui.map.getChunkNavManager()
    private static ChunkNavManager instance;

    public ChunkNavManager() {
        this.graph = new ChunkNavGraph();
        this.recorder = new ChunkNavRecorder(graph);
        this.planner = new ChunkNavPlanner(graph);
        this.portalTracker = new PortalTraversalTracker(graph, recorder);

        // Create single-thread executor for background recording
        this.recordingExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ChunkNav-Recorder");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY); // Low priority to not affect game
            return t;
        });

        // NOTE: No shutdown hook - we save every 2 seconds, so losing at most 2 seconds
        // of data on abrupt shutdown is acceptable. Shutdown hooks caused more problems
        // than they solved (race conditions with background saves).

        // Set static reference for backward compatibility
        instance = this;
    }

    /**
     * Get the ChunkNavManager instance.
     *
     * @deprecated Prefer using gui.map.getChunkNavManager() when you have access
     *             to the game UI. This static accessor exists for code paths that
     *             don't have easy access to the UI (e.g., MCache callbacks).
     */
    @Deprecated
    public static ChunkNavManager getInstance() {
        return instance;
    }

    /**
     * Initialize the navigation system for a specific world.
     */
    public void initialize(String genus) {
        if (genus == null || genus.isEmpty()) {
            return;
        }

        // Check if already initialized for this world
        if (initialized && genus.equals(currentGenus)) {
            return;
        }

        // Set guard flag BEFORE any state changes to prevent saves during init
        initializationInProgress = true;

        try {
            // Save previous world data if switching
            if (initialized && currentGenus != null) {
                save();
            }

            // Clear renderer cache to avoid stale textures from previous genus
            MinimapChunkNavRenderer.clearCache();

            this.currentGenus = genus;
            this.graph = new ChunkNavGraph();
            this.recorder = new ChunkNavRecorder(graph);
            this.planner = new ChunkNavPlanner(graph);
            this.portalTracker = new PortalTraversalTracker(graph, recorder);
            this.fileStore = new ChunkNavFileStore(genus);

            // Load saved data (with migration if needed)
            load();

            this.initialized = true;
        } finally {
            // Clear guard flag AFTER load completes (even if exception occurs)
            initializationInProgress = false;
        }
    }

    /**
     * Called periodically to check for portal traversals and record visible grids.
     * Should be called from game loop or a polling task (NMapView.tick).
     */
    public void tick() {
        if (!enabled || !initialized) return;

        portalTracker.tick();

        // Save after portal tracking (throttled - won't save every tick)
        saveThrottled();

        // Periodically record all visible grids (not just newly loaded ones)
        long now = System.currentTimeMillis();
        if (now - lastRecordTime >= RECORD_THROTTLE_MS) {
            lastRecordTime = now;
            recordVisibleGrids();
        }

        // Periodically refresh portal visualization
        if (showPortalsEnabled && now - lastPortalRefreshTime >= PORTAL_REFRESH_MS) {
            lastPortalRefreshTime = now;
            refreshPortalVisualization();
        }
    }

    /**
     * Record all currently visible grids.
     * This catches grids that were already loaded when player walks through them.
     * Runs in a background thread to avoid FPS drops.
     */
    private void recordVisibleGrids() {
        // Skip if ChunkNav overlay is disabled
        Object val = NConfig.get(NConfig.Key.chunkNavOverlay);
        if (!(val instanceof Boolean) || !(Boolean) val) {
            return;
        }

        // Skip if recording is already in progress
        if (recordingInProgress) {
            return;
        }

        // Capture grid data on main thread (quick), then process in background
        try {
            MCache mcache = NUtils.getGameUI().map.glob.map;
            if (mcache == null) return;

            // Capture list of grids to record (quick operation on main thread)
            List<MCache.Grid> gridsToRecord = new ArrayList<>();
            synchronized (mcache.grids) {
                for (MCache.Grid grid : mcache.grids.values()) {
                    if (grid != null && grid.ul != null) {
                        gridsToRecord.add(grid);
                    }
                }
            }

            if (gridsToRecord.isEmpty()) return;

            // Submit recording task to background thread
            recordingInProgress = true;
            recordingExecutor.submit(() -> {
                try {
                    for (MCache.Grid grid : gridsToRecord) {
                        recorder.recordGrid(grid);
                    }
                    saveThrottled();
                } catch (Exception e) {
                    // Ignore background recording errors
                } finally {
                    recordingInProgress = false;
                }
            });
        } catch (Exception e) {
            recordingInProgress = false;
            // Ignore - game state may not be ready
        }
    }

    /**
     * Ensure the player's current chunk is recorded in the graph.
     * This is called before path planning to handle cases where the player
     * teleported (e.g., via Hearth Fire skill) to an unrecorded chunk.
     */
    private void ensurePlayerChunkRecorded() {
        // Skip if ChunkNav overlay is disabled
        Object val = NConfig.get(NConfig.Key.chunkNavOverlay);
        if (!(val instanceof Boolean) || !(Boolean) val) {
            return;
        }

        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui == null || gui.map == null || gui.map.glob == null || gui.map.glob.map == null) {
                return;
            }

            Gob player = NUtils.player();
            if (player == null) return;

            MCache mcache = gui.map.glob.map;
            Coord tileCoord = player.rc.floor(MCache.tilesz);
            MCache.Grid playerGrid = mcache.getgridt(tileCoord);

            if (playerGrid == null) return;

            // Check if this chunk is already recorded
            if (!graph.hasChunk(playerGrid.id)) {
                recorder.recordGrid(playerGrid);
            }
        } catch (Exception e) {
            // Ignore - best effort
        }
    }

    /**
     * Plan a path to an area.
     * Returns the chunk-level path. PathFinder handles actual navigation within grids.
     */
    public ChunkPath planToArea(NArea area) {
        if (!enabled || !initialized) return null;

        // Ensure player's current chunk is recorded before planning
        // This handles cases where the player teleported to an unrecorded chunk
        ensurePlayerChunkRecorded();

        ChunkPath path = planner.planToArea(area);
        if (path == null) {
            return null;
        }

        // Export path visualization
        exportPathVisualization(path, area);

        // Note: path.isEmpty() is valid when already in the target chunk
        // The executor handles this case by navigating directly to the area
        return path;
    }

    /**
     * Export path visualization data to JSON file for the Python visualizer.
     */
    private void exportPathVisualization(ChunkPath path, NArea area) {
        if (path == null) return;

        try {
            JSONObject pathJson = new JSONObject();
            pathJson.put("timestamp", System.currentTimeMillis());
            pathJson.put("targetArea", area != null ? area.name : "unknown");
            pathJson.put("totalCost", path.totalCost);
            pathJson.put("confidence", path.confidence);
            pathJson.put("requiresPortals", path.requiresPortals);

            // Export waypoints
            JSONArray waypointsArray = new JSONArray();
            for (ChunkPath.ChunkWaypoint wp : path.waypoints) {
                JSONObject wpJson = new JSONObject();
                wpJson.put("gridId", wp.gridId);
                wpJson.put("localX", wp.localCoord != null ? wp.localCoord.x : CHUNK_SIZE / 2);
                wpJson.put("localY", wp.localCoord != null ? wp.localCoord.y : CHUNK_SIZE / 2);
                wpJson.put("type", wp.type.name());
                if (wp.portal != null) {
                    wpJson.put("portalName", wp.portal.gobName);
                    wpJson.put("portalType", wp.portal.type.name());
                }
                waypointsArray.put(wpJson);
            }
            pathJson.put("waypoints", waypointsArray);

            // Export segments with tile-level detail
            JSONArray segmentsArray = new JSONArray();
            for (ChunkPath.PathSegment seg : path.segments) {
                JSONObject segJson = new JSONObject();
                segJson.put("gridId", seg.gridId);
                segJson.put("worldTileOriginX", seg.worldTileOrigin != null ? seg.worldTileOrigin.x : 0);
                segJson.put("worldTileOriginY", seg.worldTileOrigin != null ? seg.worldTileOrigin.y : 0);
                segJson.put("type", seg.type.name());

                // Export tile steps
                JSONArray stepsArray = new JSONArray();
                for (ChunkPath.TileStep step : seg.steps) {
                    JSONObject stepJson = new JSONObject();
                    stepJson.put("localX", step.localCoord.x);
                    stepJson.put("localY", step.localCoord.y);
                    stepsArray.put(stepJson);
                }
                segJson.put("steps", stepsArray);
                segmentsArray.put(segJson);
            }
            pathJson.put("segments", segmentsArray);

            // Write to file next to chunknav directory
            Path pathFile = fileStore.getChunkDirectory().getParent().resolve("chunknav_path.json");
            Files.write(pathFile, pathJson.toString(2).getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            // Ignore export errors
        }
    }

    /**
     * Navigate to an area using the chunk navigation system.
     */
    public nurgling.actions.Results navigateToArea(NArea area, NGameUI gui) throws InterruptedException {
        if (!enabled || !initialized) {
            return nurgling.actions.Results.FAIL();
        }

        ChunkPath path = planToArea(area);
        if (path == null) {
            return nurgling.actions.Results.FAIL();
        }

        // Note: path.isEmpty() is valid when we're already in the target chunk
        // The executor handles this case by navigating directly to the area
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
     * Save navigation data to disk (throttled version).
     * Won't save more often than SAVE_THROTTLE_MS.
     * Runs on background thread to avoid FPS drops.
     */
    public void saveThrottled() {
        // Skip save during initialization to prevent Bug #3 (empty graph race condition)
        if (initializationInProgress) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSaveTime < SAVE_THROTTLE_MS) {
            return; // Too soon since last save
        }
        if (saveInProgress) {
            return; // Already saving
        }
        lastSaveTime = now; // Update immediately to prevent rapid re-triggers

        saveInProgress = true;
        recordingExecutor.submit(() -> {
            try {
                saveInternal();
            } finally {
                saveInProgress = false;
            }
        });
    }

    /**
     * Save navigation data to disk synchronously.
     * Used for shutdown - blocks until complete.
     */
    public void save() {
        if (saveInProgress) {
            return; // Background save in progress
        }
        saveInternal();
    }

    /**
     * Internal save implementation.
     * Only saves chunks that were updated within the save throttle window.
     */
    private void saveInternal() {
        // Double-check guard flag as extra safety against Bug #3
        if (initializationInProgress) return;
        if (!initialized || currentGenus == null || fileStore == null) return;

        try {
            // Get chunks updated in the last save window
            List<ChunkNavData> recentChunks = graph.getRecentlyUpdatedChunks(SAVE_THROTTLE_MS);

            if (recentChunks.isEmpty()) {
                return; // Nothing to save
            }

            // Save each recently updated chunk
            for (ChunkNavData chunk : recentChunks) {
                try {
                    fileStore.saveChunk(chunk);
                } catch (IOException e) {
                    System.err.println("ChunkNav: Failed to save chunk " + chunk.gridId + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("ChunkNav: Failed to save data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load navigation data from disk.
     * Loads from binary chunk files, with migration from old JSON format if needed.
     */
    public void load() {
        if (currentGenus == null || fileStore == null) {
            return;
        }

        try {
            // Clean up any orphaned temp files
            fileStore.cleanupTempFiles();

            // Check if we need to migrate from old JSON format
            if (fileStore.needsMigration()) {
                System.out.println("ChunkNav: Migrating from JSON to binary format...");
                migrateFromJson();
            }

            // Load all chunk files from directory
            List<ChunkNavData> loadedChunks = fileStore.loadAllChunks();

            // Add chunks to graph
            for (ChunkNavData chunk : loadedChunks) {
                graph.addChunk(chunk);
            }

            // Rebuild connections after loading all chunks
            graph.rebuildAllConnections();

            System.out.println("ChunkNav: Loaded " + loadedChunks.size() + " chunks from binary format");

        } catch (Exception e) {
            System.err.println("ChunkNav: Failed to load data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migrate from old JSON format to new binary format.
     */
    private void migrateFromJson() {
        Path oldJsonPath = fileStore.getOldJsonFilePath();
        if (!Files.exists(oldJsonPath)) {
            return;
        }

        try {
            String content = new String(Files.readAllBytes(oldJsonPath), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(content);

            // Verify genus matches
            String savedGenus = root.optString("genus", "");
            if (!currentGenus.equals(savedGenus)) {
                System.err.println("ChunkNav: Migration skipped - genus mismatch");
                return;
            }

            // Load and convert chunks
            JSONArray chunksArray = root.getJSONArray("chunks");
            int migratedCount = 0;
            int errorCount = 0;

            for (int i = 0; i < chunksArray.length(); i++) {
                try {
                    ChunkNavData chunk = ChunkNavData.fromJson(chunksArray.getJSONObject(i));
                    fileStore.saveChunk(chunk);
                    migratedCount++;
                } catch (Exception e) {
                    errorCount++;
                    if (errorCount <= 3) {
                        System.err.println("ChunkNav: Failed to migrate chunk " + i + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("ChunkNav: Migrated " + migratedCount + " chunks" +
                (errorCount > 0 ? " (" + errorCount + " errors)" : ""));

            // Delete old JSON file after successful migration
            if (migratedCount > 0 && errorCount == 0) {
                fileStore.deleteOldJsonFile();
            }

        } catch (Exception e) {
            System.err.println("ChunkNav: Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear all navigation data.
     */
    public void clear() {
        graph.clear();
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

    public ChunkNavPlanner getPlanner() {
        return planner;
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

    /**
     * Shutdown the executor (for explicit cleanup if needed).
     * Note: No automatic save here - we save every 2 seconds via tick(),
     * so losing at most 2 seconds of data is acceptable.
     */
    public void shutdown() {
        initialized = false;

        // Shutdown recording executor
        if (recordingExecutor != null) {
            recordingExecutor.shutdown();
            try {
                if (!recordingExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    recordingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                recordingExecutor.shutdownNow();
            }
        }
    }

    /**
     * Refresh portal visualization (called from tick).
     */
    private void refreshPortalVisualization() {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map != null) {
                ((nurgling.NMapView) gui.map).createPortalLabels();
            }
        } catch (Exception e) {
            // Ignore - UI might not be ready
        }
    }
}
