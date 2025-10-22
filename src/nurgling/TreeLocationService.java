package nurgling;

import haven.*;
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

    public TreeLocationService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = ((HashDirCache) ResCache.global).base + "\\..\\" + "tree_locations.nurgling.json";
        loadTreeLocations();
    }

    /**
     * Save a tree location from a tree gob on the map
     */
    public void saveTreeLocation(Gob treeGob) {
        try {
            if (gui.map == null) return;

            // Get tree resource
            Resource res = treeGob.getres();
            if (res == null || !res.name.startsWith("gfx/terobjs/trees/")) {
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

            lock.writeLock().lock();
            try {
                TreeLocation location = new TreeLocation(segmentId, segmentCoord, treeName, treeResource);
                treeLocations.put(location.getLocationId(), location);
                saveTreeLocations();
                gui.msg("Saved " + treeName + " location", java.awt.Color.GREEN);
            } finally {
                lock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error saving tree location: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Convert tree resource path to friendly name
     * e.g., "gfx/terobjs/trees/oak" -> "Oak Tree"
     */
    private String getTreeName(String resourcePath) {
        if (!resourcePath.startsWith("gfx/terobjs/trees/")) {
            return "Unknown Tree";
        }

        // Extract tree type from path
        String treeType = resourcePath.substring("gfx/terobjs/trees/".length());

        // Handle special cases with compound words
        String friendlyName = splitCamelCase(treeType);

        // Capitalize first letter and add "Tree" suffix
        if (!friendlyName.isEmpty()) {
            friendlyName = Character.toUpperCase(friendlyName.charAt(0)) + friendlyName.substring(1);
            if (!friendlyName.endsWith("Tree")) {
                friendlyName += " Tree";
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
