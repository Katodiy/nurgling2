package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class NCustomResult extends NObjectTexLabel {

    public static final Font bsans = new Font("Sans", Font.BOLD, 14);
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(bsans, 15, Color.WHITE).aa(true), 2, 1, new Color(36, 25, 25));

    static HashMap<String, BufferedImage> baubles = new HashMap<>();

    static {
        baubles.put("success", Resource.loadsimg("nurgling/hud/emoji/success"));
        baubles.put("fail", Resource.loadsimg("nurgling/hud/emoji/fail"));
    }

    public NCustomResult(Gob player, String name) {
        super(player);
        gob = (Gob) owner;
        pos = new Coord3f(0,0, 15);
        startTime = System.currentTimeMillis();
        img = new TexI(baubles.get(name));
    }


    Gob gob;
    TexI img;
    final long startTime;

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
