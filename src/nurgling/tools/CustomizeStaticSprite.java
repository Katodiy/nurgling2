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
                // Get the parent Gob from the overlay owner
                Gob gob = sprite.owner.context(Gob.class);
                if (gob != null) {
                    // Get the Gob's resource through its Drawable attribute
                    Drawable drawable = gob.getattr(Drawable.class);
                    if (drawable instanceof ResDrawable) {
                        Resource gobRes = ((ResDrawable) drawable).rres;
                        if (gobRes != null && gobRes.name.equals(CustomizeResLayer.CUPBOARD)) {
                            slot.cstate(Location.xlate(new Coord3f(-5, -5, 17.5f)));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
