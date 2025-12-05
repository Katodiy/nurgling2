package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;

public class Navigation extends Panel {
    // Temporary settings structure
    private static class NavigationSettings {
        // Safety settings
        boolean autoHearthOnUnknown;
        boolean autoLogoutOnUnknown;
        
        // Navigation settings
        boolean useGlobalPf;
        boolean waypointRetryOnStuck;
        boolean showFullPathLines;
        boolean showPathLine;
        boolean showSpeedometer;
    }

    private final NavigationSettings tempSettings = new NavigationSettings();
    
    // Safety checkboxes    
    private CheckBox autoHearthOnUnknown;
    private CheckBox autoLogoutOnUnknown;
    private TextEntry alarmDelayFramesEntry;
    
    // Navigation checkboxes
    private CheckBox useGlobalPf;
    private CheckBox waypointRetryOnStuck;
    private CheckBox showFullPathLines;
    private CheckBox showPathLine;
    private CheckBox showSpeedometer;
    
    private Scrollport scrollport;
    private Widget content;

    public Navigation() {
        super("");
        int margin = UI.scale(10);

        // Create scrollport to contain all settings
        int scrollWidth = UI.scale(560);
        int scrollHeight = UI.scale(550);
        scrollport = add(new Scrollport(new Coord(scrollWidth, scrollHeight)), new Coord(margin, margin));

        // Create main content container
        content = new Widget(new Coord(scrollWidth - UI.scale(20), UI.scale(50))) {
            @Override
            public void pack() {
                resize(contentsz());
            }
        };
        scrollport.cont.add(content, Coord.z);

        int contentMargin = UI.scale(5);
        
        // Safety section
        Widget prev = content.add(new Label("● Safety"), new Coord(contentMargin, contentMargin));
        prev = content.add(new Label("Auto-actions on unknown/red players (useful for beginners)"), prev.pos("bl").adds(0, 3));
        
        prev = autoHearthOnUnknown = content.add(new CheckBox("Auto hearth on unknown/red players") {
            public void set(boolean val) {
                tempSettings.autoHearthOnUnknown = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 10));
        
        prev = autoLogoutOnUnknown = content.add(new CheckBox("Auto logout on unknown/red players") {
            public void set(boolean val) {
                tempSettings.autoLogoutOnUnknown = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        Widget alarmDelayLabel = content.add(new Label("Alarm delay frames (before unknown player triggers alarm):"), prev.pos("bl").adds(0, 10));
        alarmDelayFramesEntry = content.add(new TextEntry(UI.scale(60), ""), alarmDelayLabel.pos("ur").adds(5, 0));

        // Pathfinding section
        prev = content.add(new Label("● Pathfinding & Navigation"), alarmDelayLabel.pos("bl").adds(0, 15));
        
        prev = useGlobalPf = content.add(new CheckBox("Use global pathfinding") {
            public void set(boolean val) {
                tempSettings.useGlobalPf = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        prev = waypointRetryOnStuck = content.add(new CheckBox("Retry waypoint movement when stuck") {
            public void set(boolean val) {
                tempSettings.waypointRetryOnStuck = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        prev = showFullPathLines = content.add(new CheckBox("Show full path lines to destinations") {
            public void set(boolean val) {
                tempSettings.showFullPathLines = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        // Visual indicators section
        prev = content.add(new Label("● Visual indicators"), prev.pos("bl").adds(0, 15));
        
        prev = showPathLine = content.add(new CheckBox("Show path line to destination") {
            public void set(boolean val) {
                tempSettings.showPathLine = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        prev = showSpeedometer = content.add(new CheckBox("Show speedometer") {
            public void set(boolean val) {
                tempSettings.showSpeedometer = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        // Pack content and update scrollbar
        content.pack();
        scrollport.cont.update();
        
        pack();
    }

    @Override
    public void load() {
        // Load safety settings
        tempSettings.autoHearthOnUnknown = (Boolean) NConfig.get(NConfig.Key.autoHearthOnUnknown);
        tempSettings.autoLogoutOnUnknown = (Boolean) NConfig.get(NConfig.Key.autoLogoutOnUnknown);
        
        // Load navigation settings
        tempSettings.useGlobalPf = (Boolean) NConfig.get(NConfig.Key.useGlobalPf);
        tempSettings.waypointRetryOnStuck = (Boolean) NConfig.get(NConfig.Key.waypointRetryOnStuck);
        tempSettings.showFullPathLines = (Boolean) NConfig.get(NConfig.Key.showFullPathLines);
        tempSettings.showPathLine = (Boolean) NConfig.get(NConfig.Key.showPathLine);
        tempSettings.showSpeedometer = (Boolean) NConfig.get(NConfig.Key.showSpeedometer);

        // Update UI components
        autoHearthOnUnknown.a = tempSettings.autoHearthOnUnknown;
        autoLogoutOnUnknown.a = tempSettings.autoLogoutOnUnknown;
        alarmDelayFramesEntry.settext(String.valueOf(((Number) NConfig.get(NConfig.Key.alarmDelayFrames)).intValue()));
        useGlobalPf.a = tempSettings.useGlobalPf;
        waypointRetryOnStuck.a = tempSettings.waypointRetryOnStuck;
        showFullPathLines.a = tempSettings.showFullPathLines;
        showPathLine.a = tempSettings.showPathLine;
        showSpeedometer.a = tempSettings.showSpeedometer;
    }

    @Override
    public void save() {
        // Save safety settings
        NConfig.set(NConfig.Key.autoHearthOnUnknown, tempSettings.autoHearthOnUnknown);
        NConfig.set(NConfig.Key.autoLogoutOnUnknown, tempSettings.autoLogoutOnUnknown);
        try {
            int val = Integer.parseInt(alarmDelayFramesEntry.text());
            if (val >= 0 && val <= 1000) {
                NConfig.set(NConfig.Key.alarmDelayFrames, val);
            }
        } catch (NumberFormatException ignored) {}
        
        // Save navigation settings
        NConfig.set(NConfig.Key.useGlobalPf, tempSettings.useGlobalPf);
        NConfig.set(NConfig.Key.waypointRetryOnStuck, tempSettings.waypointRetryOnStuck);
        NConfig.set(NConfig.Key.showFullPathLines, tempSettings.showFullPathLines);
        NConfig.set(NConfig.Key.showPathLine, tempSettings.showPathLine);
        NConfig.set(NConfig.Key.showSpeedometer, tempSettings.showSpeedometer);
    }
}
