package nurgling;

import haven.*;
import nurgling.tools.VSpec;
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
 * Service for managing saved fish locations
 * Follows the same pattern as LocalizedResourceTimerService
 */
public class FishLocationService {
    private final Map<String, FishLocation> fishLocations = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String dataFile;
    private final NGameUI gui;

    public FishLocationService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = ((HashDirCache) ResCache.global).base + "\\..\\" + "fish_locations.nurgling.json";
        loadFishLocations();
    }

    /**
     * Save a fish location from the fishing menu
     */
    public void saveFishLocation(String fishName, Coord2d playerPosition) {
        try {
            if (gui.map == null) return;

            // Get current grid and segment info
            MCache mcache = gui.map.glob.map;
            Coord tc = playerPosition.floor(MCache.tilesz);  // Tile coordinate in world
            Coord gridCoord = tc.div(MCache.cmaps);  // Grid coordinate
            MCache.Grid grid = mcache.getgrid(gridCoord);

            MapFile mapFile = gui.mmap.file;
            MapFile.GridInfo info = mapFile.gridinfo.get(grid.id);
            if (info == null) return;

            long segmentId = info.seg;

            // Calculate segment-relative coordinate (same as SMarker creation in MiniMap.java:773)
            // sc = tc + (info.sc - grid.gc) * cmaps
            Coord segmentCoord = tc.add(info.sc.sub(grid.gc).mul(MCache.cmaps));

            // Get fish resource path from VSpec
            String fishResource = getFishResourcePath(fishName);
            if (fishResource == null) {
                gui.msg("Unknown fish: " + fishName, java.awt.Color.RED);
                return;
            }

            lock.writeLock().lock();
            try {
                FishLocation location = new FishLocation(segmentId, segmentCoord, fishName, fishResource);
                fishLocations.put(location.getLocationId(), location);
                saveFishLocations();
                gui.msg("Saved " + fishName + " location", java.awt.Color.GREEN);
            } finally {
                lock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error saving fish location: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Get fish resource path from VSpec
     */
    private String getFishResourcePath(String fishName) {
        try {
            ArrayList<JSONObject> fishList = VSpec.categories.get("Fish");
            if (fishList == null) return null;

            for (JSONObject fish : fishList) {
                if (fish.getString("name").equals(fishName)) {
                    return fish.getString("static");
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting fish resource path: " + e);
        }
        return null;
    }

    /**
     * Get all fish locations for a segment (for map rendering)
     */
    public List<FishLocation> getFishLocationsForSegment(long segmentId) {
        lock.readLock().lock();
        try {
            List<FishLocation> result = new ArrayList<>();
            for (FishLocation loc : fishLocations.values()) {
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
     * Get all fish locations
     */
    public Collection<FishLocation> getAllFishLocations() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(fishLocations.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a fish location
     */
    public boolean removeFishLocation(String locationId) {
        lock.writeLock().lock();
        try {
            boolean removed = fishLocations.remove(locationId) != null;
            if (removed) {
                saveFishLocations();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove all fish locations at a specific coordinate (for cleanup)
     */
    public void removeFishLocationsAt(long segmentId, Coord tileCoords, int radius) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            Iterator<Map.Entry<String, FishLocation>> iter = fishLocations.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, FishLocation> entry = iter.next();
                FishLocation loc = entry.getValue();
                if (loc.getSegmentId() == segmentId && loc.getTileCoords().dist(tileCoords) <= radius) {
                    iter.remove();
                    changed = true;
                }
            }
            if (changed) {
                saveFishLocations();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Load fish locations from JSON
     */
    private void loadFishLocations() {
        lock.writeLock().lock();
        try {
            fishLocations.clear();
            File file = new File(dataFile);
            if (file.exists()) {
                StringBuilder contentBuilder = new StringBuilder();
                try (Stream<String> stream = Files.lines(Paths.get(dataFile), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> contentBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    System.err.println("Failed to load fish locations: " + e.getMessage());
                    return;
                }

                if (!contentBuilder.toString().trim().isEmpty()) {
                    try {
                        JSONObject main = new JSONObject(contentBuilder.toString());
                        JSONArray array = main.getJSONArray("fishLocations");
                        for (int i = 0; i < array.length(); i++) {
                            FishLocation location = new FishLocation(array.getJSONObject(i));
                            fishLocations.put(location.getLocationId(), location);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse fish locations JSON: " + e.getMessage());
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save fish locations to JSON
     */
    private void saveFishLocations() {
        // Called within write lock - don't lock again
        try {
            JSONObject main = new JSONObject();
            JSONArray jLocations = new JSONArray();
            for (FishLocation location : fishLocations.values()) {
                jLocations.put(location.toJson());
            }
            main.put("fishLocations", jLocations);
            main.put("version", 1);
            main.put("lastSaved", java.time.Instant.now().toString());

            try (FileWriter writer = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
                writer.write(main.toString(2)); // Pretty print with indent
            }
        } catch (IOException e) {
            System.err.println("Failed to save fish locations: " + e.getMessage());
        }
    }

    /**
     * Dispose the service and cleanup resources
     */
    public void dispose() {
        lock.writeLock().lock();
        try {
            saveFishLocations();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
