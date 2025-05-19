package nurgling.overlays;

import haven.Coord2d;
import haven.MCache;

public class NRenderBoxOverlay extends NBoxOverlay {
    public NRenderBoxOverlay(Owner owner) {
        super(owner, new Coord2d(MCache.cmaps).mul(9), new Coord2d(MCache.cmaps));
    }
}
