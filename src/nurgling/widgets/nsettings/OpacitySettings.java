package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.NUI;
import nurgling.widgets.NColorWidget;

public class OpacitySettings extends Panel {
    private HSlider opacitySlider;
    private Label opacityLabel;
    private CheckBox useSolidBackgroundBox;
    private NColorWidget backgroundColorWidget;

    public OpacitySettings() {
        super("UI Opacity");

        // UI Opacity setting
        add(new Label("UI Opacity:"), UI.scale(10, 50));
        opacityLabel = new Label("100%");
        addhlp(UI.scale(100, 52), UI.scale(5),
                opacitySlider = new HSlider(UI.scale(200), 0, 100, 100) {
                    protected void added() {
                        updateOpacityLabel();
                    }

                    public void changed() {
                        updateOpacityLabel();
                        applyOpacity();
                    }

                    private void updateOpacityLabel() {
                        opacityLabel.settext(val + "%");
                    }

                    private void applyOpacity() {
                        // Get the NUI instance and set opacity
                        NUI ui = (NUI) UI.getInstance();
                        if (ui != null) {
                            float opacity = val / 100.0f;
                            ui.setUIOpacity(opacity);
                        }
                    }
                }, opacityLabel);

        // Background mode setting
        add(new Label("Background Mode:"), UI.scale(10, 90));
        useSolidBackgroundBox = add(new CheckBox("Use solid color background") {
            @Override
            public void set(boolean val) {
                super.set(val);
                updateBackgroundMode();
            }
        }, UI.scale(10, 110));

        // Background color picker
        backgroundColorWidget = add(new NColorWidget("Background Color"), UI.scale(10, 140));

        // Load current settings
        load();
    }

    @Override
    public void load() {
        // Load settings from NConfig
        Object configOpacityObj = NConfig.get(NConfig.Key.uiOpacity);
        Boolean configUseSolid = (Boolean) NConfig.get(NConfig.Key.useSolidBackground);
        java.awt.Color configColor = NConfig.getColor(NConfig.Key.windowBackgroundColor, new java.awt.Color(32, 32, 32));

        // Handle opacity with proper type conversion (JSON may return BigDecimal)
        float opacity = 1.0f; // default
        if (configOpacityObj instanceof Number) {
            opacity = ((Number) configOpacityObj).floatValue();
        }
        boolean useSolid = configUseSolid != null ? configUseSolid : false;

        // Update UI controls
        opacitySlider.val = (int)(opacity * 100);
        opacitySlider.changed(); // Update label
        useSolidBackgroundBox.a = useSolid;
        backgroundColorWidget.color = configColor;

        // Apply settings to NUI for immediate effect
        NUI ui = (NUI) UI.getInstance();
        if (ui != null) {
            ui.setUIOpacity(opacity);
            ui.setUseSolidBackground(useSolid);
            ui.setWindowBackgroundColor(configColor);
        }

        updateBackgroundMode();
    }

    @Override
    public void save() {
        NUI ui = (NUI) UI.getInstance();
        if (ui != null) {
            // Apply settings to NUI (for immediate effect)
            float opacity = opacitySlider.val / 100.0f;
            ui.setUIOpacity(opacity);
            ui.setUseSolidBackground(useSolidBackgroundBox.a);
            ui.setWindowBackgroundColor(backgroundColorWidget.color);

            // Persist settings to NConfig
            NConfig.set(NConfig.Key.uiOpacity, opacity);
            NConfig.set(NConfig.Key.useSolidBackground, useSolidBackgroundBox.a);
            NConfig.setColor(NConfig.Key.windowBackgroundColor, backgroundColorWidget.color);
            NConfig.needUpdate();
        }
    }

    private void updateBackgroundMode() {
        NUI ui = (NUI) UI.getInstance();
        if (ui != null) {
            // Apply background mode settings immediately
            ui.setUseSolidBackground(useSolidBackgroundBox.a);
            ui.setWindowBackgroundColor(backgroundColorWidget.color);
        }

        // Show/hide color picker based on mode
        if (useSolidBackgroundBox.a) {
            backgroundColorWidget.show();
        } else {
            backgroundColorWidget.hide();
        }
    }
}