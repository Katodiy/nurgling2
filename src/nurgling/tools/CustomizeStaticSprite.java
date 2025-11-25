package nurgling.tools;

import haven.Coord3f;
import haven.StaticSprite;
import haven.render.Location;
import haven.render.RenderTree;
import nurgling.NConfig;

/**
 * Utility class for customizing static sprites.
 * Used to reposition decals on cupboard tops when the option is enabled.
 */
public class CustomizeStaticSprite {
    
    /**
     * Called when a StaticSprite is added to apply custom positioning.
     * Moves parchment decals to the top of cupboards when decalsOnTop is enabled.
     * 
     * @param sprite the static sprite being added
     * @param slot the render tree slot
     */
    public static void added(StaticSprite sprite, RenderTree.Slot slot) {
        try {
            Boolean decalsOnTop = (Boolean) NConfig.get(NConfig.Key.decalsOnTop);
            if (decalsOnTop != null && decalsOnTop
                && sprite.res.name.equals(CustomizeResLayer.PARCHMENT_DECAL)
                && sprite.owner.getres().name.equals(CustomizeResLayer.CUPBOARD)) {
                // Move decal to cupboard top: offset (-5, -5, 17.5)
                slot.cstate(Location.xlate(new Coord3f(-5, -5, 17.5f)));
            }
        } catch (Exception ignored) {}
    }
}
