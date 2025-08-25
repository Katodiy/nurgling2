package nurgling;

import haven.*;
import nurgling.widgets.LocalizedResourceTimerDialog;
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
 * Centralized service for all resource timer operations
 * Handles persistence, UI coordination, and map navigation
 */
public class LocalizedResourceTimerService {
    private final Map<String, LocalizedResourceTimer> timers = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String dataFile;
    private final NGameUI gui;
    
    public LocalizedResourceTimerService(NGameUI gui) {
        this.gui = gui;
        this.dataFile = ((haven.HashDirCache) haven.ResCache.global).base + "\\..\\" + "resource_timers.nurgling.json";
        loadTimers();
    }
    
    /**
     * Handle resource marker click for timer functionality
     */
    public boolean handleResourceClick(MapFile.SMarker marker) {
        if (!isTimerSupportedResource(marker.res.name)) {
            return false;
        }
        
        String displayName = marker.nm != null ? marker.nm : marker.res.name;
        showResourceTimerDialog(marker, displayName);
        return true;
    }
    
    /**
     * Show the resource timer dialog
     */
    public void showResourceTimerDialog(MapFile.SMarker marker, String displayName) {
        LocalizedResourceTimerDialog widget = gui.getAddResourceTimerWidget();
        if (widget != null) {
            widget.showForMarker(this, marker, displayName);
        }
    }
    
    /**
     * Create a timer for a resource
     */
    public void createTimer(long segmentId, haven.Coord tileCoords, String resourceName, 
                           String resourceType, long duration, String description) {
        lock.writeLock().lock();
        try {
            LocalizedResourceTimer timer = new LocalizedResourceTimer(segmentId, tileCoords, resourceName,
                                                   resourceType, duration, description);
            timers.put(timer.getResourceId(), timer);
            saveTimers();
            refreshTimerWindow();
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
                refreshTimerWindow();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get existing timer for a resource location
     */
    public LocalizedResourceTimer getExistingTimer(long segmentId, haven.Coord tileCoords, String resourceType) {
        String resourceId = generateResourceId(segmentId, tileCoords, resourceType);
        return getTimer(resourceId);
    }
    
    /**
     * Get timer by resource ID
     */
    public LocalizedResourceTimer getTimer(String resourceId) {
        lock.readLock().lock();
        try {
            return timers.get(resourceId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all timers for display
     */
    public java.util.Collection<LocalizedResourceTimer> getAllTimers() {
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
    public java.util.List<LocalizedResourceTimer> getTimersForSegment(long segmentId) {
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
     * Navigate to a resource timer location
     */
    public void openMapAtLocalizedResourceLocation(LocalizedResourceTimer timer) {
        try {
            openMapWindowIfNeeded();
            
            if (gui.mmap != null) {
                MapFile.Segment segment = gui.mmap.file.segments.get(timer.getSegmentId());
                if (segment != null) {
                    MiniMap.Location targetLoc = new MiniMap.Location(segment, timer.getTileCoords());
                    centerBigMapOnly(targetLoc);
                }
            }
        } catch (Exception e) {
            showMessage("Navigation error: " + e.getMessage());
        }
    }
    
    /**
     * Show the timer window
     */
    public void showTimerWindow() {
        if (gui.localizedResourceTimersWindow != null) {
            if (gui.localizedResourceTimersWindow.visible()) {
                gui.localizedResourceTimersWindow.hide();
            } else {
                gui.localizedResourceTimersWindow.show();
            }
        }
    }
    
    /**
     * Check if a resource type supports timers
     */
    public boolean isTimerSupportedResource(String resourceType) {
        return resourceType != null && resourceType.startsWith("gfx/terobjs/mm");
    }
    
    /**
     * Refresh the timer window display
     */
    private void refreshTimerWindow() {
        if (gui.localizedResourceTimersWindow != null) {
            gui.localizedResourceTimersWindow.refreshTimers();
        }
    }
    
    /**
     * Open map window if needed
     */
    private void openMapWindowIfNeeded() {
        if (gui.mapfile == null || !gui.mapfile.visible()) {
            gui.togglewnd(gui.mapfile);
        }
    }
    
    /**
     * Center only the big map window, not the minimap
     */
    private void centerBigMapOnly(MiniMap.Location targetLoc) {
        if (gui.mapfile != null) {
            nurgling.widgets.NMapWnd mapWnd = gui.mapfile;
            mapWnd.view.center(targetLoc);
            mapWnd.view.follow(null);
        }
    }
    
    /**
     * Show message to user
     */
    private void showMessage(String message) {
        gui.msg(message);
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
                            LocalizedResourceTimer timer = new LocalizedResourceTimer(array.getJSONObject(i));
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
            for (LocalizedResourceTimer timer : timers.values()) {
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
     * Dispose the service and cleanup resources
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