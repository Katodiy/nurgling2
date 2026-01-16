package nurgling;

import haven.*;
import nurgling.profiles.ConfigFactory;
import nurgling.profiles.ProfileAwareService;
import nurgling.widgets.LabeledMinimapMark;
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
import java.awt.image.BufferedImage;

/**
 * Service for managing labeled minimap marks (water/soil quality marks from Checker bots).
 * Supports persistence and world-specific profiles via ProfileAwareService.
 */
public class LabeledMarkService implements ProfileAwareService {
    private final Map<String, LabeledMinimapMark> labeledMarks = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private String dataFile;
    private final NGameUI gui;
    private String genus;

    public LabeledMarkService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = NUtils.getDataFile("labeled_marks.nurgling.json");
        loadLabeledMarks();
    }

    /**
     * Constructor for profile-aware initialization
     */
    public LabeledMarkService(NGameUI gui, String genus) {
        this.gui = gui;
        this.genus = genus;
        initializeForProfile(genus);
    }

    // ProfileAwareService implementation

    @Override
    public void initializeForProfile(String genus) {
        this.genus = genus;
        NConfig config = ConfigFactory.getConfig(genus);
        this.dataFile = config.getLabeledMarksPath();
        load();
    }

    @Override
    public String getGenus() {
        return genus;
    }

    @Override
    public void load() {
        loadLabeledMarks();
    }

    @Override
    public void save() {
        lock.writeLock().lock();
        try {
            saveLabeledMarks();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add a labeled mark (e.g., water or soil quality).
     * Removes any existing mark at the same location.
     */
    public void addLabeledMark(String label, String resourceType, long segmentId, 
                               Coord tileCoords, BufferedImage iconImage) {
        lock.writeLock().lock();
        try {
            // Remove any existing mark at similar location
            final Coord tc = tileCoords;
            final long segId = segmentId;
            labeledMarks.entrySet().removeIf(e -> e.getValue().isNear(segId, tc, 2));
            
            // Create and add new mark
            LabeledMinimapMark mark = new LabeledMinimapMark(label, resourceType, segmentId, tileCoords, iconImage);
            labeledMarks.put(mark.getLocationId(), mark);
            saveLabeledMarks();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get all labeled marks for a segment (for map rendering).
     */
    public List<LabeledMinimapMark> getMarksForSegment(long segmentId) {
        lock.readLock().lock();
        try {
            List<LabeledMinimapMark> result = new ArrayList<>();
            for (LabeledMinimapMark mark : labeledMarks.values()) {
                if (mark.isInSegment(segmentId)) {
                    result.add(mark);
                }
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all labeled marks.
     */
    public Collection<LabeledMinimapMark> getAllMarks() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(labeledMarks.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a labeled mark by location ID.
     */
    public boolean removeMark(String locationId) {
        lock.writeLock().lock();
        try {
            boolean removed = labeledMarks.remove(locationId) != null;
            if (removed) {
                saveLabeledMarks();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove a labeled mark object.
     */
    public boolean removeMark(LabeledMinimapMark mark) {
        if (mark == null) return false;
        return removeMark(mark.getLocationId());
    }

    /**
     * Find a mark at given segment and tile coordinates.
     */
    public LabeledMinimapMark findMarkAt(long segmentId, Coord tileCoords, int radiusTiles) {
        lock.readLock().lock();
        try {
            for (LabeledMinimapMark mark : labeledMarks.values()) {
                if (mark.isNear(segmentId, tileCoords, radiusTiles)) {
                    return mark;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Load labeled marks from JSON.
     */
    private void loadLabeledMarks() {
        lock.writeLock().lock();
        try {
            labeledMarks.clear();
            File file = new File(dataFile);
            if (file.exists()) {
                StringBuilder contentBuilder = new StringBuilder();
                try (Stream<String> stream = Files.lines(Paths.get(dataFile), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> contentBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    System.err.println("Failed to load labeled marks: " + e.getMessage());
                    return;
                }

                if (!contentBuilder.toString().trim().isEmpty()) {
                    try {
                        JSONObject main = new JSONObject(contentBuilder.toString());
                        JSONArray array = main.getJSONArray("labeledMarks");
                        for (int i = 0; i < array.length(); i++) {
                            try {
                                LabeledMinimapMark mark = new LabeledMinimapMark(array.getJSONObject(i));
                                labeledMarks.put(mark.getLocationId(), mark);
                            } catch (Exception e) {
                                System.err.println("Failed to parse labeled mark: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse labeled marks JSON: " + e.getMessage());
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save labeled marks to JSON.
     */
    private void saveLabeledMarks() {
        // Called within write lock - don't lock again
        try {
            JSONObject main = new JSONObject();
            JSONArray jMarks = new JSONArray();
            for (LabeledMinimapMark mark : labeledMarks.values()) {
                jMarks.put(mark.toJson());
            }
            main.put("labeledMarks", jMarks);
            main.put("version", 1);
            main.put("lastSaved", java.time.Instant.now().toString());

            try (FileWriter writer = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
                writer.write(main.toString(2)); // Pretty print with indent
            }
        } catch (IOException e) {
            System.err.println("Failed to save labeled marks: " + e.getMessage());
        }
    }

    /**
     * Dispose the service and cleanup resources.
     */
    public void dispose() {
        lock.writeLock().lock();
        try {
            saveLabeledMarks();
        } finally {
            lock.writeLock().unlock();
        }
    }
}

