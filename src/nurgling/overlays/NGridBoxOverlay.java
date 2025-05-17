package nurgling.overlays;

import haven.Coord2d;
import haven.MCache;

public class NGridBoxOverlay extends NBoxOverlay {
    public NGridBoxOverlay(Owner owner) {
        super(owner, new Coord2d(MCache.cmaps.mul(11)), new Coord2d(MCache.cmaps.mul(11)));
    }
}
