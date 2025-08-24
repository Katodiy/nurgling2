package nurgling.widgets;

import haven.*;
import nurgling.ResourceTimer;
import nurgling.ResourceTimerManager;

public class ResourceTimerWidget extends Window {
    private final MapFile.SMarker marker;
    private final MiniMap.Location location;
    private final String resourceDisplayName;
    private final ResourceTimer existingTimer;
    
    private TextEntry hoursEntry;
    private TextEntry minutesEntry;
    
    public ResourceTimerWidget(MapFile.SMarker marker, MiniMap.Location location, String resourceDisplayName) {
        super(new Coord(300, 150), "Resource Timer");
        this.marker = marker;
        this.location = location;
        this.resourceDisplayName = resourceDisplayName;
        
        // Check if timer already exists
        ResourceTimerManager manager = ResourceTimerManager.getInstance();
        this.existingTimer = manager.getTimer(marker.seg, marker.tc, marker.res.name);
        
        initializeWidgets();
    }
    
    private void initializeWidgets() {
        int y = 10;
        
        // Title label
        add(new Label("Set timer for: " + resourceDisplayName), new Coord(10, y));
        y += 25;
        
        if(existingTimer != null) {
            add(new Label("Existing timer: " + existingTimer.getFormattedRemainingTime()), new Coord(10, y));
            y += 25;
        }
        
        // Hours input
        add(new Label("Hours:"), new Coord(10, y));
        hoursEntry = new TextEntry(50, "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                // Only allow digits
                if(ev.c >= '0' && ev.c <= '9') {
                    return super.keydown(ev);
                }
                if(ev.c == 8 || ev.c == 127) { // backspace or delete
                    return super.keydown(ev);
                }
                return true; // consume other keys
            }
        };
        add(hoursEntry, new Coord(60, y));
        y += 30;
        
        // Minutes input  
        add(new Label("Minutes:"), new Coord(10, y));
        minutesEntry = new TextEntry(50, "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                // Only allow digits
                if(ev.c >= '0' && ev.c <= '9') {
                    return super.keydown(ev);
                }
                if(ev.c == 8 || ev.c == 127) { // backspace or delete
                    return super.keydown(ev);
                }
                return true; // consume other keys
            }
        };
        add(minutesEntry, new Coord(60, y));
        y += 40;
        
        // Buttons
        Button saveButton = new Button(60, "Save") {
            @Override
            public void click() {
                saveTimer();
            }
        };
        add(saveButton, new Coord(50, y));
        
        Button cancelButton = new Button(60, "Cancel") {
            @Override
            public void click() {
                close();
            }
        };
        add(cancelButton, new Coord(120, y));
        
        if(existingTimer != null) {
            Button removeButton = new Button(60, "Remove") {
                @Override
                public void click() {
                    removeTimer();
                }
            };
            add(removeButton, new Coord(190, y));
        }
        
        pack();
    }
    
    private void saveTimer() {
        try {
            String hoursText = hoursEntry.text().trim();
            String minutesText = minutesEntry.text().trim();
            
            int hours = hoursText.isEmpty() ? 0 : Integer.parseInt(hoursText);
            int minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);
            
            if(hours < 0 || minutes < 0 || minutes >= 60) {
                showError("Invalid time values. Minutes must be 0-59.");
                return;
            }
            
            if(hours == 0 && minutes == 0) {
                showError("Please enter a valid time.");
                return;
            }
            
            long duration = (hours * 60L + minutes) * 60L * 1000L; // Convert to milliseconds
            
            ResourceTimerManager manager = ResourceTimerManager.getInstance();
            
            // Remove existing timer if present
            if(existingTimer != null) {
                manager.removeTimer(existingTimer.getResourceId());
            }
            
            // Add new timer
            manager.addTimer(marker.seg, marker.tc, marker.nm, marker.res.name, 
                           duration, resourceDisplayName);
            
            close();
            
        } catch(NumberFormatException e) {
            showError("Please enter valid numbers.");
        } catch(Exception e) {
            showError("Error creating timer: " + e.getMessage());
        }
    }
    
    private void removeTimer() {
        if(existingTimer != null) {
            ResourceTimerManager.getInstance().removeTimer(existingTimer.getResourceId());
            close();
        }
    }
    
    private void showError(String message) {
        // For now, just print to console. Could be enhanced with in-game error display
        System.err.println("Resource Timer Error: " + message);
    }
    
    private void close() {
        ui.destroy(this);
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
        if(ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            close();
            return true;
        }
        return super.keydown(ev);
    }
}