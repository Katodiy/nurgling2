package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.widgets.ChunkNavVisualizerWindow;
import nurgling.widgets.NColorWidget;
import java.awt.Color;

public class Navigation extends Panel {
    // Temporary settings structure
    private static class NavigationSettings {
        // Safety settings
        boolean autoHearthOnUnknown;
        boolean autoLogoutOnUnknown;

        // Navigation settings
        boolean useGlobalPf;
        boolean waypointRetryOnStuck;
        boolean showPathLine;
        int pathLineWidth = 4;
        Color pathLineColor = new Color(255, 255, 0);
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
    private CheckBox showPathLine;
    private NColorWidget pathLineColorWidget;
    private HSlider pathLineWidthSlider;
    private Label pathLineWidthLabel;
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
        Widget prev = content.add(new Label("● " + L10n.get("nav.section.safety")), new Coord(contentMargin, contentMargin));
        prev = content.add(new Label(L10n.get("nav.safety_desc")), prev.pos("bl").adds(0, 3));
        
        prev = autoHearthOnUnknown = content.add(new CheckBox(L10n.get("nav.auto_hearth")) {
            public void set(boolean val) {
                tempSettings.autoHearthOnUnknown = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 10));
        
        prev = autoLogoutOnUnknown = content.add(new CheckBox(L10n.get("nav.auto_logout")) {
            public void set(boolean val) {
                tempSettings.autoLogoutOnUnknown = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        Widget alarmDelayLabel = content.add(new Label(L10n.get("nav.alarm_delay")), prev.pos("bl").adds(0, 10));
        alarmDelayFramesEntry = content.add(new TextEntry(UI.scale(60), ""), alarmDelayLabel.pos("ur").adds(5, 0));

        // Pathfinding section
        prev = content.add(new Label("● " + L10n.get("nav.section.pathfinding")), alarmDelayLabel.pos("bl").adds(0, 15));
        
        prev = useGlobalPf = content.add(new CheckBox(L10n.get("nav.use_global_pf")) {
            public void set(boolean val) {
                tempSettings.useGlobalPf = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        
        prev = waypointRetryOnStuck = content.add(new CheckBox(L10n.get("nav.retry_waypoint")) {
            public void set(boolean val) {
                tempSettings.waypointRetryOnStuck = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        // Visual indicators section
        prev = content.add(new Label("● " + L10n.get("nav.section.visual")), prev.pos("bl").adds(0, 15));
        
        prev = showPathLine = content.add(new CheckBox(L10n.get("nav.show_path_line")) {
            public void set(boolean val) {
                tempSettings.showPathLine = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        // Path line color
        prev = pathLineColorWidget = content.add(new NColorWidget(L10n.get("nav.path_line_color")), prev.pos("bl").adds(10, 5));
        pathLineColorWidget.color = tempSettings.pathLineColor;

        // Path line thickness
        prev = pathLineWidthLabel = content.add(new Label(L10n.get("nav.path_line_thickness") + " 4"), prev.pos("bl").adds(0, 5));
        prev = pathLineWidthSlider = content.add(new HSlider(UI.scale(100), 1, 10, tempSettings.pathLineWidth) {
            public void changed() {
                tempSettings.pathLineWidth = val;
                pathLineWidthLabel.settext(L10n.get("nav.path_line_thickness") + " " + val);
            }
        }, prev.pos("bl").adds(0, 5));

        prev = showSpeedometer = content.add(new CheckBox(L10n.get("nav.show_speedometer")) {
            public void set(boolean val) {
                tempSettings.showSpeedometer = val;
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));

        // Tools section
        prev = content.add(new Label("● Tools"), prev.pos("bl").adds(0, 15));

        prev = content.add(new Button(UI.scale(150), "ChunkNav Visualizer") {
            @Override
            public void click() {
                openChunkNavVisualizer();
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
        tempSettings.showPathLine = (Boolean) NConfig.get(NConfig.Key.showPathLine);
        tempSettings.showSpeedometer = (Boolean) NConfig.get(NConfig.Key.showSpeedometer);

        // Load path line settings
        Object pathLineWidthObj = NConfig.get(NConfig.Key.pathLineWidth);
        tempSettings.pathLineWidth = (pathLineWidthObj instanceof Number) ? ((Number) pathLineWidthObj).intValue() : 4;
        tempSettings.pathLineColor = NConfig.getColor(NConfig.Key.pathLineColor, new Color(255, 255, 0));

        // Update UI components
        autoHearthOnUnknown.a = tempSettings.autoHearthOnUnknown;
        autoLogoutOnUnknown.a = tempSettings.autoLogoutOnUnknown;
        alarmDelayFramesEntry.settext(String.valueOf(((Number) NConfig.get(NConfig.Key.alarmDelayFrames)).intValue()));
        useGlobalPf.a = tempSettings.useGlobalPf;
        waypointRetryOnStuck.a = tempSettings.waypointRetryOnStuck;
        showPathLine.a = tempSettings.showPathLine;
        pathLineColorWidget.color = tempSettings.pathLineColor;
        pathLineWidthSlider.val = tempSettings.pathLineWidth;
        pathLineWidthLabel.settext(L10n.get("nav.path_line_thickness") + " " + tempSettings.pathLineWidth);
        showSpeedometer.a = tempSettings.showSpeedometer;
    }

    private void openChunkNavVisualizer() {
        try {
            UI ui = NUtils.getUI();
            if (ui != null && ui.gui != null) {
                ChunkNavVisualizerWindow window = new ChunkNavVisualizerWindow();
                ui.gui.add(window, new Coord(ui.gui.sz.x / 2 - window.sz.x / 2, ui.gui.sz.y / 2 - window.sz.y / 2));
            }
        } catch (Exception e) {
            // Ignore errors
        }
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
        NConfig.set(NConfig.Key.showPathLine, tempSettings.showPathLine);
        NConfig.set(NConfig.Key.showSpeedometer, tempSettings.showSpeedometer);

        // Save path line settings
        tempSettings.pathLineColor = pathLineColorWidget.color;
        NConfig.set(NConfig.Key.pathLineWidth, tempSettings.pathLineWidth);
        NConfig.setColor(NConfig.Key.pathLineColor, tempSettings.pathLineColor);
    }
}
