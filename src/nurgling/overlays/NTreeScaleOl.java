package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.res.lib.tree.TreeScale;
import nurgling.NConfig;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NTreeScaleOl extends NObjectTexLabel {
    public static Text.Furnace fnd = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 16).aa(true), UI.scale(1), UI.scale(1), Color.BLACK);
    private static TexI qIcon = new TexI(Resource.loadsimg("nurgling/hud/growth"));
    
    // Cached config value for minimum threshold
    private static int minThreshold = 0;
    private static long lastConfigCheck = 0;
    private static final long CONFIG_CHECK_INTERVAL = 1000;
    
    // Store the calculated scale for threshold checking
    private long calculatedScale = 0;
    
    public NTreeScaleOl(Gob target) {
        super(target);
        gob = (Gob) target;
        pos = new Coord3f(0, 0, 3);
        TreeScale ts = gob.getattr(TreeScale.class);
        long scale = 0;
        if (NParser.checkName(gob.ngob.name, new NAlias("bushes"))) {
            scale = Math.round(100 * (ts.scale - 0.3) / 0.7);

        } else {
            scale = Math.round(100 * (ts.scale - 0.1) / 0.9);
        }
        this.calculatedScale = scale;
        this.img = qIcon;
        BufferedImage retlabel =fnd.render(String.format("%d%%",scale)).img;
        BufferedImage ret = TexI.mkbuf(new Coord(UI.scale(1)+img.sz().x+retlabel.getWidth(), Math.max(img.sz().y,retlabel.getHeight())));
        Graphics g = ret.getGraphics();
        g.drawImage(img.back, 0, ret.getHeight()/2-img.sz().y/2, null);
        g.drawImage(retlabel,UI.scale(1)+img.sz().x,ret.getHeight()/2-retlabel.getHeight()/2,null);
        g.dispose();
        this.label = new TexI(ret);

    }
    
    private static void updateConfigCache() {
        long now = System.currentTimeMillis();
        if (now - lastConfigCheck > CONFIG_CHECK_INTERVAL) {
            Object val = NConfig.get(NConfig.Key.treeScaleMinThreshold);
            if (val instanceof Number) {
                minThreshold = ((Number) val).intValue();
            } else {
                minThreshold = 0;
            }
            lastConfigCheck = now;
        }
    }


    Gob gob;

    @Override
    public boolean tick(double dt) {
        return gob.getattr(TreeScale.class) == null;
    }
    
    @Override
    public void draw(GOut g, Pipe state) {
        updateConfigCache();
        // Don't draw if scale is below minimum threshold
        if (calculatedScale < minThreshold && calculatedScale>=100) {
            return;
        }
        super.draw(g, state);
    }
}
