package nurgling.widgets.nsettings;

import haven.*;
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
        NUI ui = (NUI) UI.getInstance();
        if (ui != null) {
            float currentOpacity = ui.getUIOpacity();
            opacitySlider.val = (int)(currentOpacity * 100);
            opacitySlider.changed(); // Update label

            // Load background mode settings
            useSolidBackgroundBox.a = ui.getUseSolidBackground();
            backgroundColorWidget.color = ui.getWindowBackgroundColor();
            updateBackgroundMode();
        }
    }

    @Override
    public void save() {
        NUI ui = (NUI) UI.getInstance();
        if (ui != null) {
            // Apply background mode settings
            ui.setUseSolidBackground(useSolidBackgroundBox.a);
            ui.setWindowBackgroundColor(backgroundColorWidget.color);
        }
        // Opacity is applied immediately by the slider
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