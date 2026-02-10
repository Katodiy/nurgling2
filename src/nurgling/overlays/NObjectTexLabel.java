package nurgling.overlays;

import haven.*;
import haven.render.Camera;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NObjectTexLabel extends Sprite implements RenderTree.Node, PView.Render2D{
    protected Coord3f pos;
    public TexI label = null;
    protected TexI img = null;
    boolean forced = false;
    
    // Cached config values for performance
    private static boolean disableZoomHide = false;
    private static long lastConfigCheck = 0;
    private static final long CONFIG_CHECK_INTERVAL = 1000; // Check every 1 second
    
    public NObjectTexLabel(Owner owner) {
        super(owner, null);
        pos = new Coord3f(0,0,5);
    }
    
    private static void updateConfigCache() {
        long now = System.currentTimeMillis();
        if (now - lastConfigCheck > CONFIG_CHECK_INTERVAL) {
            Object val = NConfig.get(NConfig.Key.treeScaleDisableZoomHide);
            disableZoomHide = val instanceof Boolean && (Boolean) val;
            lastConfigCheck = now;
        }
    }

    @Override
    public void draw(GOut g, Pipe state) {
        if(NUtils.getGameUI()!=null) {
            updateConfigCache();
            MapView.Camera cam = NUtils.getGameUI().map.camera;
            
            // If zoom hide is disabled, always show full label
            boolean showFullLabel = forced || disableZoomHide;
            
            if (NUtils.getGameUI().map.camera instanceof MapView.FreeCam) {
                HomoCoord4f sc3 = Homo3D.obj2clip(pos, state);
                Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
                if (sc3.w > 1000 && !showFullLabel) {
                    if (img != null)
                        g.aimage(img, sc, 0.5, 0.5);
                } else {
                    if (label != null)
                        g.aimage(label, sc, 0.5, 0.5);
                }
            } else if (NUtils.getGameUI().map.camera instanceof MapView.OrthoCam) {
                Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
                if (((MapView.OrthoCam) cam).field > 400 && !showFullLabel) {
                    if (img != null)
                        g.aimage(img, sc, 0.5, 0.5);
                } else {
                    if (label != null)
                        g.aimage(label, sc, 0.5, 0.5);
                }
            } else if (NUtils.getGameUI().map.camera instanceof MapView.SimpleCam) {
                Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
                if (((MapView.SimpleCam) cam).dist > 600 && !showFullLabel) {
                    if (img != null)
                        g.aimage(img, sc, 0.5, 0.5);
                } else {
                    if (label != null)
                        g.aimage(label, sc, 0.5, 0.5);
                }
            } else {
                Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
                if (label != null)
                    g.aimage(label, sc, 0.5, 0.5);
            }
        }
    }
}
