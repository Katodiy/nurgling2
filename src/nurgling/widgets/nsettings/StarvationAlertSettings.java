package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NAlarmManager;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.overlays.StarvationVignetteOverlay;
import nurgling.widgets.StarvationAlertPopup;

/**
 * Settings panel for configuring starvation alerts.
 * Allows users to enable/disable and customize warning thresholds.
 */
public class StarvationAlertSettings extends Panel {

    private CheckBox masterEnable;

    // Popup settings
    private TextEntry popup1Threshold;
    private TextEntry popup2Threshold;

    // Vignette settings
    private TextEntry vignetteStartThreshold;
    private TextEntry vignetteCriticalThreshold;

    // Sound settings
    private TextEntry soundThreshold;
    private TextEntry soundInterval;

    // Test vignette overlay
    private StarvationVignetteOverlay testVignette = null;
    private boolean testVignetteActive = false;

    public StarvationAlertSettings() {
        super("Starvation Alert Settings");

        int margin = UI.scale(10);
        int labelWidth = UI.scale(180);
        int entryWidth = UI.scale(80);
        int y = UI.scale(40);
        int lineHeight = UI.scale(28);
        int sectionGap = UI.scale(15);

        // Master enable
        add(new Label("Configure alerts to warn you when energy gets low:"), new Coord(margin, y));
        y += lineHeight;

        masterEnable = add(new CheckBox("Enable starvation alerts") {
            public void set(boolean val) {
                a = val;
            }
        }, new Coord(margin, y));
        y += lineHeight + sectionGap;

        // === Popup Warnings Section ===
        add(new Label("● Popup Warnings (set to 0 to disable)"), new Coord(margin, y));
        y += UI.scale(22);

        add(new Label("First warning at:"), new Coord(margin, y));
        popup1Threshold = add(new TextEntry(entryWidth, ""), new Coord(margin + labelWidth, y));
        add(new Label("energy"), new Coord(margin + labelWidth + entryWidth + UI.scale(5), y));
        y += lineHeight;

        add(new Label("Critical warning at:"), new Coord(margin, y));
        popup2Threshold = add(new TextEntry(entryWidth, ""), new Coord(margin + labelWidth, y));
        add(new Label("energy"), new Coord(margin + labelWidth + entryWidth + UI.scale(5), y));
        y += lineHeight + sectionGap;

        // === Visual Effects Section ===
        add(new Label("● Screen Vignette (set to 0 to disable)"), new Coord(margin, y));
        y += UI.scale(22);

        add(new Label("Warning vignette at:"), new Coord(margin, y));
        vignetteStartThreshold = add(new TextEntry(entryWidth, ""), new Coord(margin + labelWidth, y));
        add(new Label("energy (subtle)"), new Coord(margin + labelWidth + entryWidth + UI.scale(5), y));
        y += lineHeight;

        add(new Label("Critical vignette at:"), new Coord(margin, y));
        vignetteCriticalThreshold = add(new TextEntry(entryWidth, ""), new Coord(margin + labelWidth, y));
        add(new Label("energy (intense)"), new Coord(margin + labelWidth + entryWidth + UI.scale(5), y));
        y += lineHeight + sectionGap;

        // === Audio Alerts Section ===
        add(new Label("● Audio Alerts (set to 0 to disable)"), new Coord(margin, y));
        y += UI.scale(22);

        add(new Label("Play sound below:"), new Coord(margin, y));
        soundThreshold = add(new TextEntry(entryWidth, ""), new Coord(margin + labelWidth, y));
        add(new Label("energy"), new Coord(margin + labelWidth + entryWidth + UI.scale(5), y));
        y += lineHeight;

        add(new Label("Repeat every:"), new Coord(margin, y));
        soundInterval = add(new TextEntry(entryWidth, ""), new Coord(margin + labelWidth, y));
        add(new Label("seconds"), new Coord(margin + labelWidth + entryWidth + UI.scale(5), y));
        y += lineHeight + sectionGap;

        // === Test Buttons ===
        add(new Label("● Test Alerts"), new Coord(margin, y));
        y += UI.scale(22);

        int btnWidth = UI.scale(130);
        int btnGap = UI.scale(10);

        // Row 1: Popup tests
        add(new Button(btnWidth, "Warning Popup") {
            @Override
            public void click() {
                testWarningPopup();
            }
        }, new Coord(margin, y));

        add(new Button(btnWidth, "Critical Popup") {
            @Override
            public void click() {
                testCriticalPopup();
            }
        }, new Coord(margin + btnWidth + btnGap, y));

        y += lineHeight + UI.scale(8);

        // Row 2: Vignette and sound tests
        add(new Button(btnWidth, "Warning Vignette") {
            @Override
            public void click() {
                testVignetteEffect(StarvationVignetteOverlay.Intensity.WARNING);
            }
        }, new Coord(margin, y));

        add(new Button(btnWidth, "Critical Vignette") {
            @Override
            public void click() {
                testVignetteEffect(StarvationVignetteOverlay.Intensity.CRITICAL);
            }
        }, new Coord(margin + btnWidth + btnGap, y));

        add(new Button(btnWidth, "Test Sound") {
            @Override
            public void click() {
                NAlarmManager.play("alarm/alarm");
            }
        }, new Coord(margin + (btnWidth + btnGap) * 2, y));

        // Info text
        y += lineHeight + sectionGap;
        add(new Label("Note: Energy max is 10000. Starvation damage starts at 2000."), new Coord(margin, y));
        y += UI.scale(18);
        add(new Label("Click vignette buttons again to turn off."), new Coord(margin, y));
    }

    private void testWarningPopup() {
        if (ui == null || ui.root == null) return;

        StarvationAlertPopup popup = new StarvationAlertPopup(
            "Energy Warning",
            "Your energy level is at 2700.\nPlease eat soon!",
            false
        );

        Coord screenSz = ui.root.sz;
        Coord popupSz = popup.sz;
        Coord pos = screenSz.sub(popupSz).div(2);
        ui.root.add(popup, pos);
    }

    private void testCriticalPopup() {
        if (ui == null || ui.root == null) return;

        StarvationAlertPopup popup = new StarvationAlertPopup(
            "LOW ENERGY WARNING",
            "Your energy is critically low at 2500!\nEat immediately to avoid starvation damage!",
            true
        );

        Coord screenSz = ui.root.sz;
        Coord popupSz = popup.sz;
        Coord pos = screenSz.sub(popupSz).div(2);
        ui.root.add(popup, pos);
    }

    private void testVignetteEffect(StarvationVignetteOverlay.Intensity intensity) {
        if (ui == null) return;

        if (testVignette == null) {
            testVignette = new StarvationVignetteOverlay(ui);
        }

        // If already active with same intensity, toggle off
        // If different intensity or not active, switch to new intensity
        if (testVignetteActive && testVignette.getIntensity() == intensity) {
            testVignetteActive = false;
            testVignette.setActive(false);
        } else {
            testVignetteActive = true;
            testVignette.setIntensity(intensity);
            testVignette.setActive(true);
        }
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        // Register vignette for drawing each frame when test is active
        if (testVignetteActive && testVignette != null) {
            testVignette.registerForNextFrame();
        }
    }

    @Override
    public void load() {
        // Load master enable
        Boolean enabled = (Boolean) NConfig.get(NConfig.Key.starvationAlertEnabled);
        masterEnable.a = enabled != null && enabled;

        // Load popup settings
        popup1Threshold.settext(String.valueOf(getConfigInt(NConfig.Key.starvationPopup1Threshold, 2700)));
        popup2Threshold.settext(String.valueOf(getConfigInt(NConfig.Key.starvationPopup2Threshold, 2500)));

        // Load vignette settings
        vignetteStartThreshold.settext(String.valueOf(getConfigInt(NConfig.Key.starvationVignetteStartThreshold, 2300)));
        vignetteCriticalThreshold.settext(String.valueOf(getConfigInt(NConfig.Key.starvationVignetteCriticalThreshold, 2000)));

        // Load sound settings
        soundThreshold.settext(String.valueOf(getConfigInt(NConfig.Key.starvationSoundThreshold, 2000)));
        int intervalMs = getConfigInt(NConfig.Key.starvationSoundInterval, 10000);
        soundInterval.settext(String.valueOf(intervalMs / 1000));  // Convert to seconds for display

        // Stop test vignette when loading
        if (testVignette != null) {
            testVignette.setActive(false);
            testVignetteActive = false;
        }
    }

    @Override
    public void save() {
        // Save master enable
        NConfig.set(NConfig.Key.starvationAlertEnabled, masterEnable.a);

        // Save popup settings
        NConfig.set(NConfig.Key.starvationPopup1Threshold, parseIntSafe(popup1Threshold.text(), 2700));
        NConfig.set(NConfig.Key.starvationPopup2Threshold, parseIntSafe(popup2Threshold.text(), 2500));

        // Save vignette settings
        NConfig.set(NConfig.Key.starvationVignetteStartThreshold, parseIntSafe(vignetteStartThreshold.text(), 2300));
        NConfig.set(NConfig.Key.starvationVignetteCriticalThreshold, parseIntSafe(vignetteCriticalThreshold.text(), 2000));

        // Save sound settings
        NConfig.set(NConfig.Key.starvationSoundThreshold, parseIntSafe(soundThreshold.text(), 2000));
        int intervalSec = parseIntSafe(soundInterval.text(), 10);
        NConfig.set(NConfig.Key.starvationSoundInterval, intervalSec * 1000);  // Convert to milliseconds

        NConfig.needUpdate();

        // Stop test vignette when saving
        if (testVignette != null) {
            testVignette.setActive(false);
            testVignetteActive = false;
        }
    }

    @Override
    public void hide() {
        super.hide();
        // Stop test vignette when hiding panel
        if (testVignette != null) {
            testVignette.setActive(false);
            testVignetteActive = false;
        }
    }

    private int getConfigInt(NConfig.Key key, int defaultValue) {
        Object val = NConfig.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private int parseIntSafe(String text, int defaultValue) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
