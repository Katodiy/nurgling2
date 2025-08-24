package nurgling.widgets;

import haven.*;
import nurgling.ResourceTimer;
import nurgling.ResourceTimerManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Window for managing resource timers
 */
public class ResourceTimerWindow extends haven.Window {
    private static final Coord WINDOW_SIZE = new Coord(400, 300);
    private static final int ROW_HEIGHT = 20;
    private static final Text.Foundry headerFont = new Text.Foundry(Text.dfont, 12).aa(true);
    private static final Text.Foundry contentFont = new Text.Foundry(Text.dfont, 10).aa(true);
    
    private final ResourceTimerManager manager;
    private List<ResourceTimer> displayedTimers = new ArrayList<>();
    private int scrollOffset = 0;
    private final haven.Scrollbar scrollbar;
    
    public ResourceTimerWindow() {
        super(WINDOW_SIZE, "Resource Timers");
        this.manager = ResourceTimerManager.getInstance();
        this.scrollbar = adda(new haven.Scrollbar(WINDOW_SIZE.y - 40, 0, 0), 
                             WINDOW_SIZE.x - 20, 20);
        refreshTimers();
    }
    
    @Override
    public void tick(double dt) {
        super.tick(dt);
    }
    
    private void refreshTimers() {
        displayedTimers = new ArrayList<>(manager.getAllTimers());
        // Sort by remaining time (expired first, then by time remaining)
        Collections.sort(displayedTimers, new Comparator<ResourceTimer>() {
            @Override
            public int compare(ResourceTimer a, ResourceTimer b) {
                boolean aExpired = a.isExpired();
                boolean bExpired = b.isExpired();
                
                if(aExpired != bExpired) {
                    return aExpired ? -1 : 1; // Expired timers first
                }
                
                // Both expired or both active - sort by remaining time
                return Long.compare(a.getRemainingTime(), b.getRemainingTime());
            }
        });
        
        updateScrollbar();
    }
    
    private void updateScrollbar() {
        int maxVisible = (WINDOW_SIZE.y - 60) / ROW_HEIGHT;
        int maxScroll = Math.max(0, displayedTimers.size() - maxVisible);
        scrollbar.max = maxScroll;
        scrollbar.val = Math.min(scrollOffset, maxScroll);
    }
    
    @Override
    public void draw(GOut g) {
        super.draw(g);
        drawTimerList(g);
    }
    
    private void drawTimerList(GOut g) {
        // Draw header
        g.chcolor(java.awt.Color.WHITE);
        g.atext("Resource Timers", new Coord(WINDOW_SIZE.x / 2, 15), 0.5, 0.5);
        
        // Draw column headers
        int headerY = 35;
        g.chcolor(java.awt.Color.LIGHT_GRAY);
        g.image(headerFont.render("Resource", java.awt.Color.LIGHT_GRAY).tex(), new Coord(10, headerY));
        g.image(headerFont.render("Time Left", java.awt.Color.LIGHT_GRAY).tex(), new Coord(200, headerY));
        g.image(headerFont.render("Action", java.awt.Color.LIGHT_GRAY).tex(), new Coord(320, headerY));
        
        // Draw separator line
        g.chcolor(java.awt.Color.GRAY);
        g.line(new Coord(5, headerY + 15), new Coord(WINDOW_SIZE.x - 25, headerY + 15), 1);
        
        // Draw timer entries
        int startY = headerY + 25;
        int maxVisible = (WINDOW_SIZE.y - startY - 10) / ROW_HEIGHT;
        scrollOffset = scrollbar.val;
        
        for(int i = 0; i < Math.min(maxVisible, displayedTimers.size() - scrollOffset); i++) {
            int timerIndex = i + scrollOffset;
            if(timerIndex >= displayedTimers.size()) break;
            
            ResourceTimer timer = displayedTimers.get(timerIndex);
            int rowY = startY + (i * ROW_HEIGHT);
            
            drawTimerRow(g, timer, rowY, timerIndex);
        }
        
        g.chcolor();
    }
    
    private void drawTimerRow(GOut g, ResourceTimer timer, int y, int index) {
        // Highlight expired timers
        if(timer.isExpired()) {
            g.chcolor(0, 128, 0, 50); // Light green background for ready resources
            g.frect(new Coord(5, y - 2), new Coord(WINDOW_SIZE.x - 30, ROW_HEIGHT));
        }
        
        // Resource name
        java.awt.Color textColor = timer.isExpired() ? java.awt.Color.GREEN : java.awt.Color.WHITE;
        String displayName = timer.getDescription();
        if(displayName.length() > 25) {
            displayName = displayName.substring(0, 22) + "...";
        }
        g.image(contentFont.render(displayName, textColor).tex(), new Coord(10, y));
        
        // Time remaining
        String timeText = timer.getFormattedRemainingTime();
        g.image(contentFont.render(timeText, textColor).tex(), new Coord(200, y));
        
        // Remove button (small X)
        g.image(contentFont.render("[X]", java.awt.Color.RED).tex(), new Coord(320, y));
        
        g.chcolor();
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
        Coord c = ev.c;
        int button = ev.b;
        // Check if clicking on remove buttons
        if(button == 1) {
            int headerY = 35;
            int startY = headerY + 25;
            int maxVisible = (WINDOW_SIZE.y - startY - 10) / ROW_HEIGHT;
            
            for(int i = 0; i < Math.min(maxVisible, displayedTimers.size() - scrollOffset); i++) {
                int timerIndex = i + scrollOffset;
                if(timerIndex >= displayedTimers.size()) break;
                
                int rowY = startY + (i * ROW_HEIGHT);
                
                // Check if clicking on remove button
                if(c.x >= 320 && c.x <= 340 && 
                   c.y >= rowY - 2 && c.y <= rowY + ROW_HEIGHT - 2) {
                    
                    ResourceTimer timer = displayedTimers.get(timerIndex);
                    manager.removeTimer(timer.getResourceId());
                    refreshTimers();
                    return true;
                }
                
                // Check if clicking on timer row for details
                if(c.x >= 5 && c.x <= 315 && 
                   c.y >= rowY - 2 && c.y <= rowY + ROW_HEIGHT - 2) {
                    
                    ResourceTimer timer = displayedTimers.get(timerIndex);
                    showTimerDetails(timer);
                    return true;
                }
            }
        }
        
        return super.mousedown(ev);
    }
    
    private void showTimerDetails(ResourceTimer timer) {
        // Show detailed information about the timer
        String details = String.format(
            "Resource: %s\n" +
            "Location: Segment %d, (%d, %d)\n" +
            "Started: %s\n" +
            "Duration: %s\n" +
            "Remaining: %s\n" +
            "Status: %s",
            timer.getDescription(),
            timer.getSegmentId(),
            timer.getTileCoords().x, timer.getTileCoords().y,
            formatTimestamp(timer.getStartTime()),
            formatDuration(timer.getDuration()),
            timer.getFormattedRemainingTime(),
            timer.isExpired() ? "Ready for collection" : "Cooling down"
        );
        
        Object[] options = {"Navigate to Resource", "Remove Timer", "Close"};
        int choice = javax.swing.JOptionPane.showOptionDialog(
            null, details, "Timer Details: " + timer.getDescription(),
            javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
            javax.swing.JOptionPane.INFORMATION_MESSAGE,
            null, options, options[2]);
            
        switch(choice) {
            case 0: // Navigate to resource
                navigateToResource(timer);
                break;
            case 1: // Remove timer
                manager.removeTimer(timer.getResourceId());
                refreshTimers();
                break;
            // case 2 or default: Close (do nothing)
        }
    }
    
    private void navigateToResource(ResourceTimer timer) {
        // Try to center the minimap on the resource location
        try {
            if(this.ui != null && this.ui instanceof nurgling.NUI) {
                nurgling.NUI nui = (nurgling.NUI)this.ui;
                if(nui.gui != null && nui.gui.mmap != null) {
                    // Find the segment and create a location
                    MapFile.Segment segment = nui.gui.mmap.file.segments.get(timer.getSegmentId());
                    if(segment != null) {
                        MiniMap.Location targetLoc = new MiniMap.Location(segment, timer.getTileCoords());
                        nui.gui.mmap.center(targetLoc);
                        
                        // Show message
                        nui.gui.msg("Navigating to " + timer.getDescription(), java.awt.Color.YELLOW);
                    } else {
                        nui.gui.msg("Cannot navigate to " + timer.getDescription() + " - segment not loaded", java.awt.Color.RED);
                    }
                }
            }
        } catch(Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, 
                "Error navigating to resource: " + e.getMessage(), 
                "Navigation Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String formatTimestamp(long timestamp) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
    }
    
    private String formatDuration(long durationMs) {
        long hours = durationMs / (1000 * 60 * 60);
        long minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60);
        
        if(hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender == scrollbar && msg.equals("changed")) {
            scrollOffset = scrollbar.val;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
        // Handle keyboard shortcuts
        if(ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            hide();
            return true;
        } else if(ev.code == java.awt.event.KeyEvent.VK_F5) {
            refreshTimers();
            return true;
        }
        return super.keydown(ev);
    }
}