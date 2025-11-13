package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NUI;

public class OpacitySettings extends Panel {
    private HSlider opacitySlider;
    private Label opacityLabel;

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

        // Load current opacity setting
        load();
    }

    @Override
    public void load() {
        NUI ui = (NUI) UI.getInstance();
        if (ui != null) {
            float currentOpacity = ui.getUIOpacity();
            opacitySlider.val = (int)(currentOpacity * 100);
            opacitySlider.changed(); // Update label
        }
    }

    @Override
    public void save() {
        // Opacity is applied immediately, no need to save to config
        // The slider automatically applies changes as it moves
    }
}