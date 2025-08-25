package nurgling;

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
 * Manages resource timers with persistence and thread-safe access
 */
public class ResourceTimerManager {
    private final Map<String, ResourceTimer> timers = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String dataFile;
    
    public ResourceTimerManager() {
        this.dataFile = ((haven.HashDirCache) haven.ResCache.global).base + "\\..\\" + "resource_timers.nurgling.json";
        loadTimers();
    }
    
    /**
     * Add a timer for a resource
     */
    public void addTimer(long segmentId, haven.Coord tileCoords, String resourceName, 
                        String resourceType, long duration, String description) {
        lock.writeLock().lock();
        try {
            ResourceTimer timer = new ResourceTimer(segmentId, tileCoords, resourceName, 
                                                   resourceType, duration, description);
            timers.put(timer.getResourceId(), timer);
            saveTimers();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove a timer
     */
    public boolean removeTimer(String resourceId) {
        lock.writeLock().lock();
        try {
            boolean removed = timers.remove(resourceId) != null;
            if (removed) {
                saveTimers();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get timer for a specific resource location
     */
    public ResourceTimer getTimer(long segmentId, haven.Coord tileCoords, String resourceType) {
        String resourceId = generateResourceId(segmentId, tileCoords, resourceType);
        return getTimer(resourceId);
    }
    
    /**
     * Get timer by resource ID
     */
    public ResourceTimer getTimer(String resourceId) {
        lock.readLock().lock();
        try {
            return timers.get(resourceId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all active timers
     */
    public Collection<ResourceTimer> getAllTimers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(timers.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get timers for a specific segment (for map display)
     */
    public List<ResourceTimer> getTimersForSegment(long segmentId) {
        lock.readLock().lock();
        try {
            return timers.values().stream()
                    .filter(timer -> timer.getSegmentId() == segmentId)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private static String generateResourceId(long segmentId, haven.Coord tileCoords, String resourceType) {
        return String.format("res_%d_%d_%d_%s", segmentId, tileCoords.x, tileCoords.y, 
                           resourceType.replaceAll("[^a-zA-Z0-9]", "_"));
    }
    
    /**
     * Load timers from JSON file
     */
    private void loadTimers() {
        lock.writeLock().lock();
        try {
            timers.clear();
            File file = new File(dataFile);
            if (file.exists()) {
                StringBuilder contentBuilder = new StringBuilder();
                try (Stream<String> stream = Files.lines(Paths.get(dataFile), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> contentBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    System.err.println("Failed to load resource timers: " + e.getMessage());
                    return;
                }
                
                if (!contentBuilder.toString().trim().isEmpty()) {
                    try {
                        JSONObject main = new JSONObject(contentBuilder.toString());
                        JSONArray array = main.getJSONArray("timers");
                        for (int i = 0; i < array.length(); i++) {
                            ResourceTimer timer = new ResourceTimer(array.getJSONObject(i));
                            // Only load non-expired timers
                            if (!timer.isExpired()) {
                                timers.put(timer.getResourceId(), timer);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse resource timers JSON: " + e.getMessage());
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Save timers to JSON file
     */
    private void saveTimers() {
        // Called within write lock - don't lock again
        try {
            JSONObject main = new JSONObject();
            JSONArray jTimers = new JSONArray();
            for (ResourceTimer timer : timers.values()) {
                // Only save non-expired timers
                if (!timer.isExpired()) {
                    jTimers.put(timer.toJson());
                }
            }
            main.put("timers", jTimers);
            main.put("version", 1);
            main.put("lastSaved", java.time.Instant.now().toString());
            
            try (FileWriter writer = new FileWriter(dataFile, StandardCharsets.UTF_8)) {
                writer.write(main.toString(2)); // Pretty print with indent
            }
        } catch (IOException e) {
            System.err.println("Failed to save resource timers: " + e.getMessage());
        }
    }
    
    /**
     * Dispose the manager
     */
    public void dispose() {
        // Save any remaining timers
        lock.writeLock().lock();
        try {
            saveTimers();
        } finally {
            lock.writeLock().unlock();
        }
    }
}