package nurgling.widgets;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.tools.DirectionalVector;

/**
 * Small window that appears when tracking/dowsing vectors are added.
 * Provides a button to clear all vectors.
 */
public class TrackingVectorWindow extends Window {
    private static TrackingVectorWindow instance = null;

    public TrackingVectorWindow() {
        super(UI.scale(180, 50), L10n.get("tracking.title"));

        Button clearBtn = add(new Button(UI.scale(100), L10n.get("tracking.clear_vectors"), false), UI.scale(10, 10));
        clearBtn.action(this::onClearClicked);

        pack();
    }

    /**
     * Shows the tracking vector window, creating it if needed.
     * Only one instance exists at a time.
     */
    public static void showWindow() {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui == null) return;

            // Create instance if needed
            if (instance == null || instance.parent == null) {
                instance = new TrackingVectorWindow();
                gui.add(instance, UI.scale(300, 100));
            }

            // Show if hidden
            if (!instance.visible) {
                instance.show();
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Clears all directional vectors from the map
     */
    public static void clearVectors() {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map instanceof NMapView) {
                ((NMapView) gui.map).directionalVectors.clear();
                DirectionalVector.resetColorCycle();
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private void onClearClicked() {
        clearVectors();
        hide();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            // Just hide, don't destroy when closing with X
            clearVectors();
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
