package nurgling.overlays;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.util.*;

public class BlueprintOverlay {
    // Blue semi-transparent overlay with grid
    public static final MCache.OverlayInfo blueprintol = new MCache.OverlayInfo() {
        final Material mat = new Material(new BaseColor(64, 128, 255, 48), States.maskdepth);
        
        public Collection<String> tags() {
            return Arrays.asList("show");
        }
        
        public Material mat() {
            return mat;
        }
    };
    
    // Grid lines material (white lines like regular grid)
    public static final Material gridmat = new Material(
        new BaseColor(255, 255, 255, 96), 
        States.maskdepth,
        new MapMesh.OLOrder(null),
        Location.xlate(new Coord3f(0, 0, 0.5f))
    );
}
