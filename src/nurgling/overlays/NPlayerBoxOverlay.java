package nurgling.overlays;

import haven.Coord2d;
import haven.MCache;

public class NPlayerBoxOverlay extends NBoxOverlay {
    public NPlayerBoxOverlay(Owner owner) {
        super(owner, new Coord2d(MCache.cmaps), new Coord2d(MCache.cmaps));
    }
}
