package nurgling.widgets;

import haven.*;
import nurgling.LocalizedResourceTimer;
import nurgling.LocalizedResourceTimerService;
import nurgling.i18n.L10n;

public class LocalizedResourceTimerDialog extends Window {
    private MapFile.SMarker currentMarker;
    private String currentResourceDisplayName;
    private LocalizedResourceTimer currentExistingTimer;
    private LocalizedResourceTimerService currentService;
    
    private TextEntry hoursEntry;
    private TextEntry minutesEntry;
    private Label titleLabel;
    private Label existingTimerLabel;
    private Button removeButton;
    
    public LocalizedResourceTimerDialog() {
        super(UI.scale(new Coord(235, 140)), L10n.get("timer.title"));
        initializeWidgets();
        hide(); // Start hidden
    }
    
    public void showForMarker(LocalizedResourceTimerService service, MapFile.SMarker marker, String resourceDisplayName) {
        this.currentService = service;
        this.currentMarker = marker;
        this.currentResourceDisplayName = resourceDisplayName;
        
        // Check if timer already exists
        this.currentExistingTimer = service.getExistingTimer(marker.seg, marker.tc, marker.res.name);
        
        updateWidgetContent();
        show();
        raise();
        setfocus(hoursEntry);
    }
    
    private void initializeWidgets() {
        int y = UI.scale(5);
        
        // Title label
        titleLabel = new Label(L10n.get("timer.set_for"));
        add(titleLabel, UI.scale(new Coord(10, y)));
        y += UI.scale(18);
        
        // Existing timer label (initially empty)
        existingTimerLabel = new Label("");
        add(existingTimerLabel, UI.scale(new Coord(10, y)));
        y += UI.scale(18);
        
        // Input line: [hours] hrs [minutes] mins
        int inputY = y;
        hoursEntry = new TextEntry(UI.scale(40), "") {
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
        add(hoursEntry, UI.scale(new Coord(10, inputY)));
        
        add(new Label(L10n.get("timer.hrs")), UI.scale(new Coord(55, inputY + 3)));
        
        minutesEntry = new TextEntry(UI.scale(40), "") {
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
        add(minutesEntry, UI.scale(new Coord(85, inputY)));
        
        add(new Label(L10n.get("timer.mins")), UI.scale(new Coord(130, inputY + 3)));
        
        y += UI.scale(30);
        
        // Buttons with consistent spacing - always reserve space for Remove button
        Button saveButton = new Button(UI.scale(65), L10n.get("common.save")) {
            @Override
            public void click() {
                saveTimer();
            }
        };
        add(saveButton, UI.scale(new Coord(10, y)));
        
        Button cancelButton = new Button(UI.scale(65), L10n.get("common.cancel")) {
            @Override
            public void click() {
                close();
            }
        };
        add(cancelButton, UI.scale(new Coord(85, y)));
        
        // Remove button (initially hidden) - always positioned consistently
        removeButton = new Button(UI.scale(65), L10n.get("common.remove")) {
            @Override
            public void click() {
                removeTimer();
            }
        };
        add(removeButton, UI.scale(new Coord(160, y)));
        removeButton.hide();
    }
    
    private void updateWidgetContent() {
        // Update title
        titleLabel.settext(L10n.get("timer.set_for") + " " + currentResourceDisplayName);
        
        // Update existing timer info
        if(currentExistingTimer != null) {
            existingTimerLabel.settext(L10n.get("timer.existing") + ": " + currentExistingTimer.getFormattedRemainingTime());
            existingTimerLabel.show();
            removeButton.show();
        } else {
            existingTimerLabel.settext("");
            existingTimerLabel.hide();
            removeButton.hide();
        }
        
        // Clear input fields
        hoursEntry.settext("");
        minutesEntry.settext("");
    }
    
    private void saveTimer() {
        try {
            int hours = parseTimeInput(hoursEntry.text().trim());
            int minutes = parseTimeInput(minutesEntry.text().trim());
            
            if(!validateTimeInput(hours, minutes)) {
                return;
            }
            
            long duration = (hours * 60L + minutes) * 60L * 1000L;
            
            if(currentService != null) {
                if(currentExistingTimer != null) {
                    currentService.removeTimer(currentExistingTimer.getResourceId());
                }
                
                currentService.createTimer(currentMarker.seg, currentMarker.tc, currentMarker.nm, currentMarker.res.name, 
                                         duration, currentResourceDisplayName);
            }
            
            close();
            
        } catch(NumberFormatException e) {
            showError("Please enter valid numbers.");
        } catch(Exception e) {
            showError("Error creating timer: " + e.getMessage());
        }
    }
    
    private void removeTimer() {
        if(currentExistingTimer != null && currentService != null) {
            currentService.removeTimer(currentExistingTimer.getResourceId());
            close();
        }
    }
    
    private int parseTimeInput(String input) {
        return input.isEmpty() ? 0 : Integer.parseInt(input);
    }
    
    private boolean validateTimeInput(int hours, int minutes) {
        if(hours < 0 || minutes < 0 || minutes >= 60) {
            showError("Invalid time values. Minutes must be 0-59.");
            return false;
        }
        
        if(hours == 0 && minutes == 0) {
            showError("Please enter a valid time.");
            return false;
        }
        
        return true;
    }
    
    private void showError(String message) {
        System.err.println("Resource Timer Error: " + message);
    }
    
    private void close() {
        hide();
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
        if(ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            close();
            return true;
        }
        return super.keydown(ev);
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(msg, args);
        }
    }
}