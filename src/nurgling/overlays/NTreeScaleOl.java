package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.res.lib.tree.TreeScale;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NTreeScaleOl extends NObjectTexLabel {
    public static Text.Furnace fnd = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 16).aa(true), UI.scale(1), UI.scale(1), Color.BLACK);
    private static TexI qIcon = new TexI(Resource.loadsimg("nurgling/hud/growth"));
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
        this.img = qIcon;
        BufferedImage retlabel =fnd.render(String.format("%d%%",scale)).img;
        BufferedImage ret = TexI.mkbuf(new Coord(UI.scale(1)+img.sz().x+retlabel.getWidth(), Math.max(img.sz().y,retlabel.getHeight())));
        Graphics g = ret.getGraphics();
        g.drawImage(img.back, 0, ret.getHeight()/2-img.sz().y/2, null);
        g.drawImage(retlabel,UI.scale(1)+img.sz().x,ret.getHeight()/2-retlabel.getHeight()/2,null);
        g.dispose();
        this.label = new TexI(ret);

    }


    Gob gob;

    @Override
    public boolean tick(double dt) {
        return gob.getattr(TreeScale.class) == null;
    }
}
