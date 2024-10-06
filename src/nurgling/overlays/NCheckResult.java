package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class NCheckResult extends NObjectTexLabel {

    public static final Font bsans = new Font("Sans", Font.BOLD, 14);
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(bsans, 15, Color.WHITE).aa(true), 2, 1, new Color(36, 25, 25));

    static HashMap<String, BufferedImage> baubles = new HashMap<>();

    static {
        baubles.put("EMPTY", Resource.loadsimg("nurgling/hud/bubles/empty"));
        baubles.put("Water", Resource.loadsimg("nurgling/hud/bubles/water"));
        baubles.put("Saltwater", Resource.loadsimg("nurgling/hud/bubles/saltwater"));
    }

    public NCheckResult(Gob player, double quality, String name, BufferedImage сimg) {
        super(player);
        gob = (Gob) owner;
        pos = new Coord3f(0,0, 15);
        startTime = System.currentTimeMillis();
        customImg = сimg;
        img = init((float) quality, (customImg == null) ? new TexI(baubles.get(name)):new TexI(baubles.get("EMPTY")));
    }

    TexI init(float qual, TexI img) {
        String value = String.format("%.0f", qual);
        BufferedImage retlabel = active_title.render(value).img;
        BufferedImage ret = TexI.mkbuf(new Coord(UI.scale(1) + img.sz().x + retlabel.getWidth(), Math.max(img.sz().y, retlabel.getHeight())));
        Graphics g = ret.getGraphics();
        g.drawImage(img.back, 0, ret.getHeight() / 2 - img.sz().y / 2, null);
        if(customImg!=null) {
            g.drawImage(customImg, UI.scale(8), 2*ret.getHeight()/5-retlabel.getHeight()/2 +UI.scale(2), UI.scale(24), UI.scale(24),null);
        }
        g.drawImage(retlabel, UI.scale(2) + ret.getWidth()/2 - 3*retlabel.getWidth()/4, 2*ret.getHeight()/5-retlabel.getHeight()/2 +UI.scale(2), null);
        g.dispose();
        return new TexI(ret);
    }

    Gob gob;
    TexI img;
    final long startTime;
    BufferedImage customImg = null;
    public NCheckResult(Owner owner, double qual, String val) {
        super(owner);
        gob = (Gob) owner;
        pos = new Coord3f(0,0, 15);
        startTime = System.currentTimeMillis();
        img = init((float) qual,new TexI(baubles.get(val)));
    }

    @Override
    public boolean tick(double dt) {
        return (System.currentTimeMillis() - startTime)>2000;
    }


    @Override
    public void draw(GOut g, Pipe state)
    {
        Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
        g.aimage(img, sc, 0.5, 0.5);
    }
}
