package nurgling.widgets;

import haven.*;
import nurgling.NAlarmManager;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.overlays.StarvationVignetteOverlay;

/**
 * Widget that monitors player energy levels and triggers starvation alerts.
 * Provides progressive warnings through popups, screen vignette, and sound alerts.
 */
public class StarvationAlertWidget extends Widget {

    // State tracking for threshold crossings (to avoid popup spam)
    private int lastEnergy = 10000;  // Start high to avoid immediate triggers
    private boolean popup1Shown = false;
    private boolean popup2Shown = false;

    // Sound interval tracking
    private long lastSoundTime = 0;

    // Vignette overlay reference
    private StarvationVignetteOverlay vignetteOverlay = null;

    // Current popup reference (to avoid duplicates)
    private StarvationAlertPopup currentPopup = null;

    public StarvationAlertWidget() {
        super(Coord.z);  // Zero-size widget, just for tick()
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);

        // Check if game UI is available
        if (NUtils.getGameUI() == null) {
            return;
        }

        // Check if system is enabled
        Boolean enabled = (Boolean) NConfig.get(NConfig.Key.starvationAlertEnabled);
        if (enabled == null || !enabled) {
            // Clean up vignette if disabled
            if (vignetteOverlay != null) {
                vignetteOverlay.setActive(false);
            }
            return;
        }

        // Get current energy (0.0 - 1.0 scale)
        double energyRaw = NUtils.getEnergy();
        if (energyRaw < 0) {
            return;  // Energy not available yet
        }

        // Convert to game units (0-10000)
        int currentEnergy = (int) (energyRaw * 10000);

        // Get thresholds from config
        int popup1Threshold = getConfigInt(NConfig.Key.starvationPopup1Threshold, 2700);
        int popup2Threshold = getConfigInt(NConfig.Key.starvationPopup2Threshold, 2500);
        int vignetteStartThreshold = getConfigInt(NConfig.Key.starvationVignetteStartThreshold, 2300);
        int vignetteCriticalThreshold = getConfigInt(NConfig.Key.starvationVignetteCriticalThreshold, 2000);
        int soundThreshold = getConfigInt(NConfig.Key.starvationSoundThreshold, 2000);
        int soundInterval = getConfigInt(NConfig.Key.starvationSoundInterval, 10000);

        // Check popup 1 threshold crossing (enabled if threshold > 0)
        if (popup1Threshold > 0) {
            if (currentEnergy <= popup1Threshold && lastEnergy > popup1Threshold && !popup1Shown) {
                showPopup1(currentEnergy);
                popup1Shown = true;
            }
            // Reset when energy rises above threshold
            if (currentEnergy > popup1Threshold) {
                popup1Shown = false;
            }
        }

        // Check popup 2 threshold crossing (enabled if threshold > 0)
        if (popup2Threshold > 0) {
            if (currentEnergy <= popup2Threshold && lastEnergy > popup2Threshold && !popup2Shown) {
                showPopup2(currentEnergy);
                popup2Shown = true;
            }
            // Reset when energy rises above threshold
            if (currentEnergy > popup2Threshold) {
                popup2Shown = false;
            }
        }

        // Handle vignette overlay (enabled if either threshold > 0)
        if (vignetteStartThreshold > 0 || vignetteCriticalThreshold > 0) {
            updateVignette(currentEnergy, vignetteStartThreshold, vignetteCriticalThreshold);
        } else if (vignetteOverlay != null) {
            vignetteOverlay.setActive(false);
        }

        // Handle sound alerts (enabled if threshold > 0)
        if (soundThreshold > 0) {
            if (currentEnergy <= soundThreshold) {
                long now = System.currentTimeMillis();
                if (now - lastSoundTime >= soundInterval) {
                    NAlarmManager.play("alarm/alarm");
                    lastSoundTime = now;
                }
            }
        }

        lastEnergy = currentEnergy;
    }

    private void showPopup1(int energy) {
        if (ui == null || ui.root == null) return;

        // Close existing popup if any
        if (currentPopup != null) {
            currentPopup.close();
        }

        currentPopup = new StarvationAlertPopup(
            "Energy Warning",
            String.format("Your energy level is at %d.\nPlease eat soon!", energy),
            false  // Not critical
        );

        // Center on screen
        Coord screenSz = ui.root.sz;
        Coord popupSz = currentPopup.sz;
        Coord pos = screenSz.sub(popupSz).div(2);
        ui.root.add(currentPopup, pos);
    }

    private void showPopup2(int energy) {
        if (ui == null || ui.root == null) return;

        // Close existing popup if any
        if (currentPopup != null) {
            currentPopup.close();
        }

        currentPopup = new StarvationAlertPopup(
            "LOW ENERGY WARNING",
            String.format("Your energy is critically low at %d!\nEat immediately to avoid starvation damage!", energy),
            true  // Critical
        );

        // Center on screen
        Coord screenSz = ui.root.sz;
        Coord popupSz = currentPopup.sz;
        Coord pos = screenSz.sub(popupSz).div(2);
        ui.root.add(currentPopup, pos);
    }

    private void updateVignette(int energy, int startThreshold, int criticalThreshold) {
        // Create vignette overlay if needed
        if (vignetteOverlay == null) {
            vignetteOverlay = new StarvationVignetteOverlay(ui);
        }

        if (energy <= criticalThreshold) {
            // Critical level - intense vignette
            vignetteOverlay.setActive(true);
            vignetteOverlay.setIntensity(StarvationVignetteOverlay.Intensity.CRITICAL);
            vignetteOverlay.registerForNextFrame();
        } else if (energy <= startThreshold) {
            // Start level - subtle vignette
            vignetteOverlay.setActive(true);
            vignetteOverlay.setIntensity(StarvationVignetteOverlay.Intensity.WARNING);
            vignetteOverlay.registerForNextFrame();
        } else {
            // Above threshold - no vignette
            vignetteOverlay.setActive(false);
        }
    }

    private int getConfigInt(NConfig.Key key, int defaultValue) {
        Object val = NConfig.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    /**
     * Called when the widget is being removed/destroyed
     */
    @Override
    public void destroy() {
        if (vignetteOverlay != null) {
            vignetteOverlay.setActive(false);
            vignetteOverlay = null;
        }
        if (currentPopup != null) {
            currentPopup.close();
            currentPopup = null;
        }
        super.destroy();
    }

    /**
     * Reset alert states - useful for testing or when eating
     */
    public void resetAlerts() {
        popup1Shown = false;
        popup2Shown = false;
        lastEnergy = 10000;
    }
}
