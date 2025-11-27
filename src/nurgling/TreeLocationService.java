package nurgling;

import haven.*;
import nurgling.profiles.ConfigFactory;
import nurgling.profiles.ProfileAwareService;
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
 * Supports world-specific profiles via ProfileAwareService
 */
public class TreeLocationService implements ProfileAwareService {
    private final Map<String, TreeLocation> treeLocations = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private String dataFile;
    private final NGameUI gui;
    private String genus;

    public TreeLocationService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = ((HashDirCache) ResCache.global).base + "\\..\\" + "tree_locations.nurgling.json";
        loadTreeLocations();
    }

    /**
     * Constructor for profile-aware initialization
     */
    public TreeLocationService(NGameUI gui, String genus) {
        this.gui = gui;
        this.genus = genus;
        initializeForProfile(genus);
    }

    // ProfileAwareService implementation

    @Override
    public void initializeForProfile(String genus) {
        this.genus = genus;
        NConfig config = ConfigFactory.getConfig(genus);
        this.dataFile = config.getTreeLocationsPath();
        load();
    }

    @Override
    public void migrateFromGlobal() {
        // Migration is handled automatically by ProfileManager when profile is created
        // Just reload data from the new profile location
        load();
    }

    @Override
    public String getConfigFileName() {
        return "tree_locations.nurgling.json";
    }

    @Override
    public String getGenus() {
        return genus;
    }

    @Override
    public void load() {
        loadTreeLocations();
    }

    @Override
    public void save() {
        lock.writeLock().lock();
        try {
            saveTreeLocations();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getConfigPath() {
        return dataFile;
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
                TreeLocation location = new TreeLocation(segmentId, segmentCoord, treeName, treeResource, quantity);
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
