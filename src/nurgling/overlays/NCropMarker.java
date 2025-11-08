package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.*;
/**
 * NCropMarker is responsible for rendering crop markers on the map.
 * It listens for changes in the crop's growth stage and updates the visual representation accordingly.
 */
public class NCropMarker extends Sprite implements RenderTree.Node, PView.Render2D {
    /** The image displayed for the crop marker */
    private TexI img = null;
    /** Position of the crop marker */
    private Coord3f pos = new Coord3f(0, 0, 0);

    /** Current growth stage of the crop */
    private long currentStage = -2;
    /** Crop properties such as specific stage and max stage */
    private NProperties.Crop crop;
    /** Indicates whether the crop stage should be shown */
    private static boolean showCropStage = false;
    /** Timestamp of last config check */
    private static long lastConfigCheck = 0;
    /** Flag to track initialization */
    private static boolean initialized = false;

    /**
     * Creates a new NCropMarker for a specific Gob.
     *
     * @param gob the Gob that represents the crop
     */
    public NCropMarker(Gob gob) {
        super(gob, null);
        this.crop = NProperties.Crop.getCrop((Gob) owner);
        // Config is now static and checked globally
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
        
        // Check configuration globally every second across all instances
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConfigCheck > 1000) { // Check every second max
            showCropStage = (Boolean) NConfig.get(NConfig.Key.showCropStage);
            lastConfigCheck = currentTime;
        }

        // Early exit if stage display is disabled
        if (!showCropStage) {
            return super.tick(dt);
        }

        Gob gob = (Gob) owner;
        long modelAttr = gob.ngob.getModelAttribute();

        // Update image only when stage changes
        if (currentStage != modelAttr) {
            updateImage(modelAttr);
            currentStage = modelAttr;
        }

        return super.tick(dt);
    }
	
	/**
     * Updates the image for the crop marker based on the model attribute.
     *
     * @param modelAttr the current model attribute
     */
    private void updateImage(long modelAttr) {
        if (modelAttr == crop.maxstage) {
            img = (crop.maxstage == 0) ? NStyle.iCropMap.get(NStyle.CropMarkers.GRAY) : NStyle.iCropMap.get(NStyle.CropMarkers.GREEN);
        } else if (modelAttr == 0) {
            img = NStyle.iCropMap.get(NStyle.CropMarkers.RED);
        } else if (crop.maxstage > 1 && crop.maxstage < 7) {
            img = (modelAttr == crop.specstage) ? NStyle.iCropMap.get(NStyle.CropMarkers.BLUE) : NStyle.getCropTexI(modelAttr, crop.maxstage);
        }
    }
	
	@Override
    public void dispose() {
        img = null;
        crop = null;
        super.dispose();
    }

	@Override
    public void draw(GOut g, Pipe state) {
        if (showCropStage && img != null) {
            Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
            g.aimage(img, sc, 0.5, 0.5);
        }
    }
}