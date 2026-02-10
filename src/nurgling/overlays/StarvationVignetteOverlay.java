package nurgling.overlays;

import haven.*;

/**
 * Full-screen vignette overlay that creates a pulsing red edge effect
 * to warn players about low energy levels.
 */
public class StarvationVignetteOverlay implements UI.AfterDraw {

    public enum Intensity {
        WARNING,   // Subtle pulsing at edges
        CRITICAL   // Strong pulsing, more coverage
    }

    private final UI ui;
    private boolean active = false;
    private Intensity intensity = Intensity.WARNING;

    // Warning level settings
    private static final float WARNING_BASE_ALPHA = 0.08f;
    private static final float WARNING_MAX_ALPHA = 0.20f;
    private static final double WARNING_PULSE_SPEED = 1.5;  // Slower pulse
    private static final float WARNING_EDGE_PERCENT = 0.20f;  // 20% of screen from each edge

    // Critical level settings
    private static final float CRITICAL_BASE_ALPHA = 0.15f;
    private static final float CRITICAL_MAX_ALPHA = 0.40f;
    private static final double CRITICAL_PULSE_SPEED = 4.0;  // Faster pulse
    private static final float CRITICAL_EDGE_PERCENT = 0.30f;  // 30% of screen from each edge

    public StarvationVignetteOverlay(UI ui) {
        this.ui = ui;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Call this every tick to keep the overlay registered for drawing.
     * Must be called from outside the draw cycle (e.g., from a widget's tick() method).
     */
    public void registerForNextFrame() {
        if (active && ui != null) {
            ui.drawafter(this);
        }
    }

    public void setIntensity(Intensity intensity) {
        this.intensity = intensity;
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void draw(GOut g) {
        if (!active || ui == null || ui.root == null) {
            return;
        }

        // NOTE: Do NOT call ui.drawafter() here - it causes ConcurrentModificationException
        // Instead, registerForNextFrame() should be called from tick()

        int screenW = ui.root.sz.x;
        int screenH = ui.root.sz.y;

        // Calculate pulse
        double time = System.currentTimeMillis() / 1000.0;
        float baseAlpha, maxAlpha, edgePercent;
        double pulseSpeed;

        if (intensity == Intensity.CRITICAL) {
            baseAlpha = CRITICAL_BASE_ALPHA;
            maxAlpha = CRITICAL_MAX_ALPHA;
            pulseSpeed = CRITICAL_PULSE_SPEED;
            edgePercent = CRITICAL_EDGE_PERCENT;
        } else {
            baseAlpha = WARNING_BASE_ALPHA;
            maxAlpha = WARNING_MAX_ALPHA;
            pulseSpeed = WARNING_PULSE_SPEED;
            edgePercent = WARNING_EDGE_PERCENT;
        }

        float pulse = (float) Math.abs(Math.sin(time * pulseSpeed));
        float currentAlpha = baseAlpha + pulse * (maxAlpha - baseAlpha);

        int edgeW = (int) (screenW * edgePercent);
        int edgeH = (int) (screenH * edgePercent);

        // Number of gradient bands for smooth falloff
        int bands = 20;

        // Draw full vignette effect (all four edges + corners)
        drawVignette(g, screenW, screenH, edgeW, edgeH, currentAlpha, bands);
    }

    private void drawVignette(GOut g, int screenW, int screenH, int edgeW, int edgeH, float maxAlpha, int bands) {
        // Draw gradient bands from edges toward center
        // The alpha decreases as we move toward center

        // Top edge
        for (int i = 0; i < bands; i++) {
            float progress = (float) i / bands;
            float alpha = maxAlpha * (1.0f - progress * progress);  // Quadratic falloff
            int y = (int) (edgeH * progress);
            int h = edgeH / bands + 1;

            g.chcolor(255, 0, 0, (int) (alpha * 255));
            g.frect(new Coord(0, y), new Coord(screenW, h));
        }

        // Bottom edge
        for (int i = 0; i < bands; i++) {
            float progress = (float) i / bands;
            float alpha = maxAlpha * (1.0f - progress * progress);
            int y = screenH - (int) (edgeH * progress) - edgeH / bands;
            int h = edgeH / bands + 1;

            g.chcolor(255, 0, 0, (int) (alpha * 255));
            g.frect(new Coord(0, y), new Coord(screenW, h));
        }

        // Left edge
        for (int i = 0; i < bands; i++) {
            float progress = (float) i / bands;
            float alpha = maxAlpha * (1.0f - progress * progress);
            int x = (int) (edgeW * progress);
            int w = edgeW / bands + 1;

            g.chcolor(255, 0, 0, (int) (alpha * 255));
            g.frect(new Coord(x, 0), new Coord(w, screenH));
        }

        // Right edge
        for (int i = 0; i < bands; i++) {
            float progress = (float) i / bands;
            float alpha = maxAlpha * (1.0f - progress * progress);
            int x = screenW - (int) (edgeW * progress) - edgeW / bands;
            int w = edgeW / bands + 1;

            g.chcolor(255, 0, 0, (int) (alpha * 255));
            g.frect(new Coord(x, 0), new Coord(w, screenH));
        }

        // Reset color
        g.chcolor();
    }
}
