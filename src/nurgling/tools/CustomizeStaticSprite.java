package nurgling.tools;

import haven.*;
import haven.render.Location;
import haven.render.RenderTree;
import nurgling.NConfig;

/**
 * Utility class for customizing static sprites.
 * Used to position parchment decals on cupboard tops.
 */
public class CustomizeStaticSprite {
    
    /**
     * Called when a static sprite is added to a slot.
     * Applies offset to parchment decals on cupboards when decalsOnTop is enabled.
     * 
     * @param sprite the static sprite being added
     * @param slot the render slot
     */
    public static void added(StaticSprite sprite, RenderTree.Slot slot) {
        try {
            Boolean decalsOnTop = (Boolean) NConfig.get(NConfig.Key.decalsOnTop);
            if (decalsOnTop != null && decalsOnTop
                && sprite.res.name.equals(CustomizeResLayer.PARCHMENT_DECAL)) {
                // Try to get parent resource from owner context
                Resource ownerRes = sprite.owner.context(Resource.class);
                if (ownerRes != null && ownerRes.name.equals(CustomizeResLayer.CUPBOARD)) {
                    slot.cstate(Location.xlate(new Coord3f(-5, -5, 17.5f)));
                }
            }
        } catch (Exception ignored) {}
    }
}
