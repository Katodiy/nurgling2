package nurgling;

import haven.*;
import nurgling.profiles.ConfigFactory;
import nurgling.profiles.ProfileAwareService;
import nurgling.tools.VSpec;
import nurgling.widgets.NEquipory;
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
 * Supports world-specific profiles via ProfileAwareService
 */
public class FishLocationService implements ProfileAwareService {
    private final Map<String, FishLocation> fishLocations = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private String dataFile;
    private final NGameUI gui;
    private String genus;

    public FishLocationService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = ((HashDirCache) ResCache.global).base + "\\..\\" + "fish_locations.nurgling.json";
        loadFishLocations();
    }

    /**
     * Constructor for profile-aware initialization
     */
    public FishLocationService(NGameUI gui, String genus) {
        this.gui = gui;
        this.genus = genus;
        initializeForProfile(genus);
    }

    // ProfileAwareService implementation

    @Override
    public void initializeForProfile(String genus) {
        this.genus = genus;
        NConfig config = ConfigFactory.getConfig(genus);
        this.dataFile = config.getFishLocationsPath();
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
        return "fish_locations.nurgling.json";
    }

    @Override
    public String getGenus() {
        return genus;
    }

    @Override
    public void load() {
        loadFishLocations();
    }

    @Override
    public void save() {
        lock.writeLock().lock();
        try {
            saveFishLocations();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getConfigPath() {
        return dataFile;
    }

    /**
     * Save a fish location from the fishing menu
     */
    public void saveFishLocation(String fishName, String percentage, Coord2d playerPosition) {
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

            // Extract fishing equipment information
            FishingEquipment equipment = getFishingEquipment();

            // Get current in-game time and moon phase
            String gameTime = "Unknown";
            String moonPhase = "Unknown";
            try {
                if (gui.map != null && gui.map.glob != null && gui.map.glob.ast != null) {
                    haven.Astronomy ast = gui.map.glob.ast;
                    gameTime = String.format("%02d:%02d", ast.hh, ast.mm);

                    // Calculate moon phase
                    haven.Resource moon = haven.Resource.local().loadwait("gfx/hud/calendar/moon");
                    haven.Resource.Anim moonAnim = moon.layer(haven.Resource.animc);
                    int moonPhaseIndex = (int)Math.round(ast.mp * (double)moonAnim.f.length) % moonAnim.f.length;
                    moonPhase = haven.Astronomy.phase[moonPhaseIndex];
                }
            } catch (Exception e) {
                System.err.println("Error getting time/moon phase: " + e);
            }

            lock.writeLock().lock();
            try {
                FishLocation location = new FishLocation(segmentId, segmentCoord, fishName, fishResource,
                    percentage, gameTime, moonPhase, equipment.fishingRod, equipment.hook, equipment.line, equipment.bait);
                fishLocations.put(location.getLocationId(), location);
                saveFishLocations();
                gui.msg("Saved " + fishName + " location (" + percentage + ")", java.awt.Color.GREEN);
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

    /**
     * Helper class to hold fishing equipment information
     */
    private static class FishingEquipment {
        String fishingRod = "Unknown";
        String hook = "Unknown";
        String line = "Unknown";
        String bait = "Unknown";
    }

    /**
     * Extract fishing equipment information from equipped items
     */
    private FishingEquipment getFishingEquipment() {
        FishingEquipment equipment = new FishingEquipment();

        try {
            // Get fishing rod from equipment (same pattern as RepairFishingRot)
            NEquipory eq = NUtils.getEquipment();
            if (eq == null) return equipment;

            // Find fishing rod in equipment (check for both types)
            WItem rod = eq.findItem("Primitive Casting-Rod");
            if (rod == null) {
                rod = eq.findItem("Bushcraft Fishingpole");
            }

            if (rod != null && rod.item instanceof NGItem) {
                NGItem rodItem = (NGItem) rod.item;

                // Get fishing rod name
                equipment.fishingRod = rodItem.name() != null ? rodItem.name() : "Unknown";

                // Get fishing rod contents (hook, line, bait)
                ArrayList<NGItem.NContent> contents = rodItem.content();
                for (NGItem.NContent content : contents) {
                    String contentName = content.name();
                    if (contentName == null) continue;

                    // Identify item type based on name patterns
                    if (contentName.contains("Hook")) {
                        equipment.hook = contentName;
                    } else if (contentName.contains("line") || contentName.contains("Line")) {
                        equipment.line = contentName;
                    } else {
                        // Assume anything else is bait/lure
                        equipment.bait = contentName;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error extracting fishing equipment: " + e);
            e.printStackTrace();
        }

        return equipment;
    }
}
