package nurgling;

import haven.*;
import nurgling.widgets.AddResourceTimerWidget;

/**
 * Centralized service for all resource timer operations
 * Coordinates between ResourceTimerManager and UI components
 */
public class ResourceTimerService {
    private final ResourceTimerManager manager;
    private final NGameUI gui;
    
    public ResourceTimerService(NGameUI gui) {
        this.gui = gui;
        this.manager = new ResourceTimerManager();
    }
    
    /**
     * Handle resource marker click for timer functionality
     */
    public boolean handleResourceClick(MapFile.SMarker marker, MiniMap.Location location) {
        if (!isTimerSupportedResource(marker.res.name)) {
            return false;
        }
        
        String displayName = marker.nm != null ? marker.nm : marker.res.name;
        showResourceTimerDialog(marker, location, displayName);
        return true;
    }
    
    /**
     * Show the resource timer dialog
     */
    public void showResourceTimerDialog(MapFile.SMarker marker, MiniMap.Location location, String displayName) {
        AddResourceTimerWidget widget = gui.getAddResourceTimerWidget();
        if (widget != null) {
            widget.showForMarker(this, marker, location, displayName);
        }
    }
    
    /**
     * Create a timer for a resource
     */
    public void createTimer(long segmentId, haven.Coord tileCoords, String resourceName, 
                           String resourceType, long duration, String description) {
        manager.addTimer(segmentId, tileCoords, resourceName, resourceType, duration, description);
        refreshTimerWindow();
    }
    
    /**
     * Remove a timer
     */
    public boolean removeTimer(String resourceId) {
        boolean removed = manager.removeTimer(resourceId);
        if (removed) {
            refreshTimerWindow();
        }
        return removed;
    }
    
    /**
     * Get existing timer for a resource location
     */
    public ResourceTimer getExistingTimer(long segmentId, haven.Coord tileCoords, String resourceType) {
        return manager.getTimer(segmentId, tileCoords, resourceType);
    }
    
    /**
     * Get all timers for display
     */
    public java.util.Collection<ResourceTimer> getAllTimers() {
        return manager.getAllTimers();
    }
    
    /**
     * Get timers for a specific segment (for map display)
     */
    public java.util.List<ResourceTimer> getTimersForSegment(long segmentId) {
        return manager.getTimersForSegment(segmentId);
    }
    
    /**
     * Navigate to a resource timer location
     */
    public void navigateToResourceTimer(ResourceTimer timer) {
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
            showMessage("Navigation error: " + e.getMessage(), java.awt.Color.RED);
        }
    }
    
    /**
     * Show the timer window
     */
    public void showTimerWindow() {
        if (gui.resourceTimersWindow != null) {
            if (gui.resourceTimersWindow.visible()) {
                gui.resourceTimersWindow.hide();
            } else {
                gui.resourceTimersWindow.show();
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
        if (gui.resourceTimersWindow != null) {
            gui.resourceTimersWindow.refreshTimers();
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
        if (gui.mapfile != null && gui.mapfile instanceof nurgling.widgets.NMapWnd) {
            nurgling.widgets.NMapWnd mapWnd = (nurgling.widgets.NMapWnd) gui.mapfile;
            mapWnd.view.center(targetLoc);
            
            if (mapWnd.view instanceof MiniMap) {
                ((MiniMap) mapWnd.view).follow(null);
            }
        }
    }
    
    /**
     * Show message to user
     */
    private void showMessage(String message, java.awt.Color color) {
        gui.msg(message, color);
    }
    
    /**
     * Dispose the service and cleanup resources
     */
    public void dispose() {
        if (manager != null) {
            manager.dispose();
        }
    }
}