package nurgling.tools;

import haven.Resource;
import haven.Skeleton;
import nurgling.NConfig;

import java.util.Objects;

/**
 * Utility class for customizing resource layers.
 * Used to skip certain bone offsets for cupboard decals when displaying them on top.
 */
public class CustomizeResLayer {
    public static final String CUPBOARD = "gfx/terobjs/cupboard";
    public static final String PARCHMENT_DECAL = "gfx/terobjs/items/parchment-decal";
    
    /**
     * Checks if the layer should return null (be skipped).
     * Used to skip 'decal' bone offset for cupboards so decals would be positioned 
     * statically at (0,0,0) and not moving on the door.
     * 
     * @param res the resource being queried
     * @param cl the layer class
     * @param id the layer id
     * @return true if the layer should be skipped (return null)
     */
    public static <I, L extends Resource.IDLayer<I>> boolean needReturnNull(Resource res, Class<L> cl, I id) {
        try {
            Boolean decalsOnTop = (Boolean) NConfig.get(NConfig.Key.decalsOnTop);
            if (decalsOnTop != null && decalsOnTop
                && cl == Skeleton.BoneOffset.class
                && res.name.equals(CUPBOARD)
                && Objects.equals(id, "decal")) {
                return true;
            }
        } catch (Exception ignored) {}
        
        return false;
    }
}
