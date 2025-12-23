package nurgling.overlays;

import haven.*;
import haven.render.*;
import haven.res.gfx.fx.eq.Equed;
import nurgling.*;

/**
 * NGardenPotMarker displays growth stage indicators on garden pots.
 * - Yellow half circle: Plant is growing (1 Equed overlay)
 * - Green full circle: Ready to harvest (2+ Equed overlays)
 * - No indicator: Empty pot (0 Equed overlays)
 *
 * Uses the same showCropStage setting as crop markers.
 */
public class NGardenPotMarker extends Sprite implements RenderTree.Node, PView.Render2D {
    /** The image displayed for the garden pot marker */
    private TexI img = null;
    /** Position of the marker */
    private static final Coord3f MARKER_POS = new Coord3f(0, 0, 0);
    /** Cached screen coordinate */
    private Coord cachedScreenCoord = null;
    /** Frame counter for screen coord cache invalidation */
    private double lastFrameUpdate = -1;

    /** Current state: 0=empty, 1=growing, 2+=ready */
    private int currentState = -1;
    /** Indicates whether the stage should be shown */
    private static boolean showCropStage = false;
    /** Timestamp of last config check */
    private static long lastConfigCheck = 0;
    /** Flag to track initialization */
    private static boolean initialized = false;

    /** Yellow half circle for growing plants */
    private static final TexI GROWING_IMG = NStyle.getCropTexI(2, 4); // yellow_2_4 = 50% filled
    /** Green full circle for ready to harvest */
    private static final TexI READY_IMG = NStyle.iCropMap.get(NStyle.CropMarkers.GREEN);

    /**
     * Creates a new NGardenPotMarker for a specific Gob.
     *
     * @param gob the Gob that represents the garden pot
     */
    public NGardenPotMarker(Gob gob) {
        super(gob, null);
    }

    @Override
    public void gtick(Render g) {
        super.gtick(g);
    }

    @Override
    public boolean tick(double dt) {
        // Initialize on first run
        if (!initialized) {
            showCropStage = (Boolean) NConfig.get(NConfig.Key.showCropStage);
            lastConfigCheck = System.currentTimeMillis();
            initialized = true;
        }

        // Check configuration globally every second
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConfigCheck > 1000) {
            showCropStage = (Boolean) NConfig.get(NConfig.Key.showCropStage);
            lastConfigCheck = currentTime;
        }

        // Early exit if stage display is disabled
        if (!showCropStage) {
            img = null;
            return super.tick(dt);
        }

        Gob gob = (Gob) owner;
        int equedCount = countEquedOverlays(gob);

        // Update image only when state changes
        if (currentState != equedCount) {
            updateImage(equedCount);
            currentState = equedCount;
        }

        return super.tick(dt);
    }

    /**
     * Counts the number of Equed overlays on the garden pot.
     * 0 = empty, 1 = growing, 2+ = ready to harvest
     */
    private int countEquedOverlays(Gob pot) {
        int count = 0;
        try {
            for (Gob.Overlay ol : pot.ols) {
                if (ol.spr instanceof Equed) {
                    count++;
                }
            }
        } catch (Exception e) {
            // Handle concurrent modification gracefully
        }
        return count;
    }

    /**
     * Updates the image based on the Equed overlay count.
     *
     * @param equedCount number of Equed overlays on the pot
     */
    private void updateImage(int equedCount) {
        if (equedCount >= 2) {
            // Ready to harvest - green full circle
            img = READY_IMG;
        } else if (equedCount == 1) {
            // Growing - yellow half circle
            img = GROWING_IMG;
        } else {
            // Empty pot - no indicator
            img = null;
        }
    }

    @Override
    public void dispose() {
        img = null;
        super.dispose();
    }

    @Override
    public void draw(GOut g, Pipe state) {
        // Early exit if disabled or no image
        if (!showCropStage || img == null) {
            return;
        }

        // Get current frame counter from Gob
        Gob gob = (Gob) owner;
        double currentFrame = gob.glob.globtime();

        // Cache screen coordinate for this frame
        if (cachedScreenCoord == null || lastFrameUpdate != currentFrame) {
            Coord3f screenPos = Homo3D.obj2view(MARKER_POS, state, Area.sized(g.sz()));
            if (screenPos == null) {
                return;
            }
            cachedScreenCoord = screenPos.round2();
            lastFrameUpdate = currentFrame;
        }

        if (cachedScreenCoord == null) {
            return;
        }

        // Draw the marker centered at the pot position
        g.aimage(img, cachedScreenCoord, 0.5, 0.5);
    }
}
