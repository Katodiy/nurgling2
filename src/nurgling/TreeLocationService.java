package nurgling;

import haven.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Service for managing saved tree locations
 * Simplified version of FishLocationService - no equipment, percentage, moon phase
 */
public class TreeLocationService {
    private final Map<String, TreeLocation> treeLocations = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String dataFile;
    private final NGameUI gui;

    // Auto-discovery fields
    private final Set<String> seenTreeIds = Collections.synchronizedSet(new HashSet<>());
    private static final long DEDUPE_DISTANCE = 5; // Tiles
    private long lastAutoSaveTime = 0;
    private static final long AUTO_SAVE_INTERVAL = 30000; // 30 seconds
    private boolean autoDiscoveryInitialized = false;
    private OCache.ChangeCallback changeCallback; // Keep strong reference to prevent GC

    public TreeLocationService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = ((HashDirCache) ResCache.global).base + "\\..\\" + "tree_locations.nurgling.json";
        loadTreeLocations();
        // Don't call registerAutoDiscovery() here - gui.ui is not ready yet
        // Will be called via tryInitAutoDiscovery() after UI is fully initialized
    }

    /**
     * Try to initialize auto-discovery if not already done.
     * Safe to call multiple times - will only register once.
     * Call this from tick() or other method that runs after UI is ready.
     */
    public void tryInitAutoDiscovery() {
        if (autoDiscoveryInitialized) {
            return; // Already initialized
        }

        // Check if UI session is ready
        if (gui.ui == null || gui.ui.sess == null || gui.ui.sess.glob == null) {
            return; // Not ready yet, will try again next time
        }

        // Check if config enabled
        if ((Boolean) NConfig.get(NConfig.Key.autoRecordTrees)) {
            registerAutoDiscovery();
            System.out.println("[TreeLocationService] Auto-discovery initialized successfully!");
        } else {
            System.out.println("[TreeLocationService] Auto-discovery disabled in config");
        }

        autoDiscoveryInitialized = true;
    }

    /**
     * Register OCache callback for automatic tree discovery
     */
    private void registerAutoDiscovery() {
        // Store callback in field to prevent garbage collection (OCache uses WeakList)
        changeCallback = new OCache.ChangeCallback() {
            @Override
            public void added(Gob gob) {
                autoSaveTreeIfNeeded(gob);
            }

            @Override
            public void removed(Gob gob) {
                // Optional: could track removed trees
            }
        };
        gui.ui.sess.glob.oc.callback(changeCallback);
        System.out.println("[TreeLocationService] Registered OCache callback");
    }

    /**
     * Automatically save trees as they appear in view
     */
    private void autoSaveTreeIfNeeded(Gob gob) {
        try {
            if (gui.map == null) return;

            Resource res = gob.getres();
            if (res == null) return;

            // Check if tree or bush
            if (!res.name.startsWith("gfx/terobjs/trees/") &&
                !res.name.startsWith("gfx/terobjs/bushes/")) {
                return;
            }

            // Exclude logs, stumps, trunks
            if (res.name.contains("log") || res.name.contains("stump") ||
                res.name.contains("trunk")) {
                return;
            }

            System.out.println("[TreeLocationService] Checking tree: " + res.name);

            // Deduplication: check if we've seen this tree recently
            String treeId = generateTreeId(gob);
            if (seenTreeIds.contains(treeId)) {
                System.out.println("[TreeLocationService] Already seen: " + treeId);
                return; // Already recorded
            }

            // Get grid and segment info
            MCache mcache = gui.map.glob.map;
            Coord tc = gob.rc.floor(MCache.tilesz);
            Coord gridCoord = tc.div(MCache.cmaps);
            MCache.Grid grid = mcache.getgrid(gridCoord);
            long gridId = grid.id;

            MapFile mapFile = gui.mmap.file;
            MapFile.GridInfo info = mapFile.gridinfo.get(grid.id);
            if (info == null) return;

            long segmentId = info.seg;
            Coord segmentCoord = tc.add(info.sc.sub(grid.gc).mul(MCache.cmaps));

            String treeResource = res.name;
            String treeName = getTreeName(treeResource);

            // Save the location
            lock.writeLock().lock();
            try {
                TreeLocation location = new TreeLocation(segmentId, gridId, segmentCoord,
                                                        treeName, treeResource, 1);
                treeLocations.put(location.getLocationId(), location);
                seenTreeIds.add(treeId);

                System.out.println("[TreeLocationService] Saved tree: " + treeName + " at " + segmentCoord + " (treeId: " + treeId + ")");

                // Debounced auto-save
                long now = System.currentTimeMillis();
                if (now - lastAutoSaveTime > AUTO_SAVE_INTERVAL) {
                    saveTreeLocations();
                    lastAutoSaveTime = now;
                    System.out.println("[TreeLocationService] Auto-saved to disk");
                }

            } finally {
                lock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("[TreeLocationService] Error in autoSaveTreeIfNeeded: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate unique tree ID for deduplication
     */
    private String generateTreeId(Gob gob) {
        Coord tc = gob.rc.floor(MCache.tilesz);
        // Round to DEDUPE_DISTANCE to avoid near-duplicates
        int gridX = tc.x / (int)DEDUPE_DISTANCE;
        int gridY = tc.y / (int)DEDUPE_DISTANCE;
        String resName = gob.getres().name;
        return String.format("%s_%d_%d", resName, gridX, gridY);
    }

    /**
     * Save a tree/bush location from a tree or bush gob on the map
     */
    public void saveTreeLocation(Gob treeGob) {
        try {
            if (gui.map == null) return;

            // Get tree/bush resource
            Resource res = treeGob.getres();
            if (res == null || (!res.name.startsWith("gfx/terobjs/trees/") && !res.name.startsWith("gfx/terobjs/bushes/"))) {
                return;
            }

            String treeResource = res.name;
            String treeName = getTreeName(treeResource);

            // Get current grid and segment info (same as FishLocationService)
            MCache mcache = gui.map.glob.map;
            Coord tc = treeGob.rc.floor(MCache.tilesz);  // Tile coordinate in world
            Coord gridCoord = tc.div(MCache.cmaps);  // Grid coordinate
            MCache.Grid grid = mcache.getgrid(gridCoord);
            long gridId = grid.id;  // NEW: Get grid ID

            MapFile mapFile = gui.mmap.file;
            MapFile.GridInfo info = mapFile.gridinfo.get(grid.id);
            if (info == null) return;

            long segmentId = info.seg;

            // Calculate segment-relative coordinate (same as SMarker creation in MiniMap.java:773)
            Coord segmentCoord = tc.add(info.sc.sub(grid.gc).mul(MCache.cmaps));

            // Count nearby trees/bushes of the same type
            int quantity = countNearbyTrees(treeGob, treeResource);

            lock.writeLock().lock();
            try {
                TreeLocation location = new TreeLocation(segmentId, gridId, segmentCoord, treeName, treeResource, quantity);
                treeLocations.put(location.getLocationId(), location);
                saveTreeLocations();
                gui.msg("Saved " + treeName + " location (quantity: " + quantity + ")", java.awt.Color.GREEN);
            } finally {
                lock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error saving tree location: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Count all trees/bushes of the same type on the map
     */
    private int countNearbyTrees(Gob centerGob, String resourceName) {
        // Use Finder to find all gobs matching this resource name
        NAlias alias = new NAlias(resourceName);
        ArrayList<Gob> matchingGobs = Finder.findGobs(alias);
        return matchingGobs.size();
    }

    /**
     * Convert tree/bush resource path to friendly name
     * e.g., "gfx/terobjs/trees/oak" -> "Oak Tree"
     * e.g., "gfx/terobjs/bushes/arrowwood" -> "Arrowwood Bush"
     */
    private String getTreeName(String resourcePath) {
        boolean isTree = resourcePath.startsWith("gfx/terobjs/trees/");
        boolean isBush = resourcePath.startsWith("gfx/terobjs/bushes/");

        if (!isTree && !isBush) {
            return "Unknown";
        }

        // Extract type from path
        String type;
        String suffix;
        if (isTree) {
            type = resourcePath.substring("gfx/terobjs/trees/".length());
            suffix = " Tree";
        } else {
            type = resourcePath.substring("gfx/terobjs/bushes/".length());
            suffix = " Bush";
        }

        // Handle special cases with compound words
        String friendlyName = splitCamelCase(type);

        // Capitalize first letter and add suffix
        if (!friendlyName.isEmpty()) {
            friendlyName = Character.toUpperCase(friendlyName.charAt(0)) + friendlyName.substring(1);
            if (!friendlyName.endsWith("Tree") && !friendlyName.endsWith("Bush")) {
                friendlyName += suffix;
            }
        }

        return friendlyName;
    }

    /**
     * Split camelCase words for tree names
     * e.g., "appletree" -> "apple tree", "baywillow" -> "bay willow"
     */
    private String splitCamelCase(String input) {
        // First handle common tree name patterns
        input = input.replaceAll("tree$", ""); // Remove trailing "tree" if present

        // Split camelCase
        String result = input.replaceAll("([a-z])([A-Z])", "$1 $2");

        // Handle lowercase compound words by inserting space before known tree types
        result = result.replaceAll("(apple|almond|bay|birch|cedar|cherry|elm|fir|oak|pine|willow)", "$1 ");

        return result.trim();
    }

    /**
     * Get all tree locations for a segment (for map rendering)
     */
    public List<TreeLocation> getTreeLocationsForSegment(long segmentId) {
        lock.readLock().lock();
        try {
            List<TreeLocation> result = new ArrayList<>();
            for (TreeLocation loc : treeLocations.values()) {
                if (loc.getSegmentId() == segmentId) {
                    result.add(loc);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all tree locations
     */
    public Collection<TreeLocation> getAllTreeLocations() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(treeLocations.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a tree location
     */
    public boolean removeTreeLocation(String locationId) {
        lock.writeLock().lock();
        try {
            boolean removed = treeLocations.remove(locationId) != null;
            if (removed) {
                saveTreeLocations();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get all tree locations for a specific grid (NEW: for grid-based queries)
     */
    public List<TreeLocation> getTreeLocationsForGrid(long gridId) {
        lock.readLock().lock();
        try {
            List<TreeLocation> result = new ArrayList<>();
            for (TreeLocation loc : treeLocations.values()) {
                if (loc.getGridId() == gridId) {
                    result.add(loc);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all grids that contain a specific tree type (NEW: for grid-based queries)
     */
    public Set<Long> getGridsWithTreeType(String treeResource) {
        lock.readLock().lock();
        try {
            Set<Long> grids = new HashSet<>();
            for (TreeLocation loc : treeLocations.values()) {
                if (loc.getTreeResource().equals(treeResource)) {
                    grids.add(loc.getGridId());
                }
            }
            return grids;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Load tree locations from JSON
     */
    private void loadTreeLocations() {
        lock.writeLock().lock();
        try {
            treeLocations.clear();
            File file = new File(dataFile);
            if (file.exists()) {
                StringBuilder contentBuilder = new StringBuilder();
                try (Stream<String> stream = Files.lines(Paths.get(dataFile), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> contentBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    System.err.println("Failed to load tree locations: " + e.getMessage());
                    return;
                }

                if (!contentBuilder.toString().trim().isEmpty()) {
                    try {
                        JSONObject main = new JSONObject(contentBuilder.toString());
                        JSONArray array = main.getJSONArray("treeLocations");
                        for (int i = 0; i < array.length(); i++) {
                            TreeLocation location = new TreeLocation(array.getJSONObject(i));
                            treeLocations.put(location.getLocationId(), location);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse tree locations JSON: " + e.getMessage());
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save tree locations to JSON
     */
    private void saveTreeLocations() {
        // Called within write lock - don't lock again
        try {
            JSONObject main = new JSONObject();
            JSONArray jLocations = new JSONArray();
            for (TreeLocation location : treeLocations.values()) {
                jLocations.put(location.toJson());
            }
            main.put("treeLocations", jLocations);
            main.put("version", 1);
            main.put("lastSaved", java.time.Instant.now().toString());

            try (FileWriter writer = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
                writer.write(main.toString(2)); // Pretty print with indent
            }
        } catch (IOException e) {
            System.err.println("Failed to save tree locations: " + e.getMessage());
        }
    }

    /**
     * Dispose the service and cleanup resources
     */
    public void dispose() {
        lock.writeLock().lock();
        try {
            saveTreeLocations();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
